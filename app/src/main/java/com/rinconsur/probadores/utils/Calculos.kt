package com.rinconsur.probadores.utils

import com.rinconsur.probadores.data.local.ParametrosEntity
import com.rinconsur.probadores.data.local.ProbadorEntity
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.data.local.UsoEntity
import com.rinconsur.probadores.model.EstadoTurno
import kotlin.math.ceil

// Resultado del resumen para la jefatura (RF-09)
data class ResumenDia(
    val ingresos: Int,
    val atendidos: Int,
    val abandonos: Int,
    val porcentajeAbandono: Double,
    val esperaPromedio: Double,
    val usosCerrados: Int,
    val usoPromedio: Double,
    val conversion: Double,
    val abandonosVentaPerdida: Int,
    val ventaPerdida: Long
)

// Calculos de negocio. No dependen de Android, por eso se pueden probar con JUnit.
object Calculos {

    // Promedio de minutos que dura un uso cerrado
    fun promedioUso(usosCerrados: List<UsoEntity>): Double {
        if (usosCerrados.isEmpty()) return 0.0
        var total = 0.0
        for (u in usosCerrados) {
            total += (u.horaTermino!! - u.horaInicio) / 60000.0
        }
        return total / usosCerrados.size
    }

    // RN-06: tiempo estimado de espera en minutos.
    // (personas antes + 1) / probadores habilitados * (promedio de uso + ordenamiento)
    // Si hay menos de 10 usos cerrados en el dia se usa el tiempo de referencia (8 min).
    // Devuelve 0 si hay un probador libre para la persona, y -1 si no hay probadores habilitados.
    fun tiempoEstimado(
        personasAntes: Int,
        probadoresHabilitados: Int,
        probadoresLibres: Int,
        usosCerrados: List<UsoEntity>,
        parametros: ParametrosEntity
    ): Int {
        if (probadoresHabilitados <= 0) return -1
        if (personasAntes < probadoresLibres) return 0

        val promedio = if (usosCerrados.size < 10) {
            parametros.tiempoRefUso.toDouble()
        } else {
            // RNF-08: nunca menos que el de referencia, para ser prudentes
            maxOf(promedioUso(usosCerrados), parametros.tiempoRefUso.toDouble())
        }
        val minutos = (personasAntes + 1).toDouble() / probadoresHabilitados * (promedio + parametros.minOrdenamiento)
        return ceil(minutos).toInt()
    }

    // RN-08
    fun esUsoProlongado(minutosOcupado: Long, parametros: ParametrosEntity): Boolean {
        return minutosOcupado > parametros.umbralProlongado
    }

    fun necesitaOfrecerAyuda(minutosOcupado: Long, personasEnFila: Int, parametros: ParametrosEntity): Boolean {
        return minutosOcupado > parametros.umbralAyuda && personasEnFila > 0
    }

    // Orden de la fila: primero los preferentes (RN-05) y despues por orden de llegada (RN-04)
    fun ordenarFila(turnos: List<TurnoEntity>): List<TurnoEntity> {
        return turnos.sortedWith(compareBy({ !it.preferente }, { it.numero }))
    }

    // Elige a quien llamar cuando se libera un probador.
    // RN-05: los preferentes tienen prioridad para P-08 y, si P-08 esta ocupado,
    // pasan primeros al siguiente probador libre.
    fun siguienteTurno(enEspera: List<TurnoEntity>, probador: ProbadorEntity, p08Libre: Boolean): TurnoEntity? {
        val esperando = enEspera.filter { it.estado == EstadoTurno.EN_ESPERA }.sortedBy { it.numero }
        if (esperando.isEmpty()) return null
        val preferente = esperando.firstOrNull { it.preferente }
        val normal = esperando.firstOrNull { !it.preferente }

        if (preferente != null && (probador.accesible || !p08Libre)) {
            return preferente
        }
        // HU-10 CA-2: si no hay preferentes, P-08 se asigna al siguiente normal
        return normal ?: preferente
    }

    // RF-09 / RN-09: resumen del dia o de una franja horaria
    fun resumen(
        turnos: List<TurnoEntity>,
        usos: List<UsoEntity>,
        parametros: ParametrosEntity,
        horaDesde: Int? = null,
        horaHasta: Int? = null
    ): ResumenDia {
        var listaTurnos = turnos
        var listaUsos = usos.filter { it.cerrado() }
        if (horaDesde != null && horaHasta != null) {
            listaTurnos = listaTurnos.filter {
                val h = FechaUtil.horaDelDia(it.horaIngreso)
                h >= horaDesde && h < horaHasta
            }
            listaUsos = listaUsos.filter {
                val h = FechaUtil.horaDelDia(it.horaInicio)
                h >= horaDesde && h < horaHasta
            }
        }

        val ingresos = listaTurnos.size
        val atendidos = listaTurnos.filter { it.estado == EstadoTurno.ATENDIDO }
        // RN-07: el que no se presenta tambien cuenta como abandono
        val abandonos = listaTurnos.filter {
            it.estado == EstadoTurno.ABANDONO || it.estado == EstadoTurno.NO_PRESENTO
        }
        val abandonosVenta = abandonos.count { it.motivoAbandono?.cuentaVentaPerdida ?: true }

        val porcentaje = if (ingresos > 0) abandonos.size * 100.0 / ingresos else 0.0

        var esperaTotal = 0.0
        for (t in atendidos) {
            esperaTotal += (t.horaCierre!! - t.horaIngreso) / 60000.0
        }
        val esperaPromedio = if (atendidos.isNotEmpty()) esperaTotal / atendidos.size else 0.0

        // Supuesto S-07: si no devolvio todas las prendas, se asume que compro
        val conCompra = listaUsos.count { it.prendasDevueltas!! < it.prendasIngresadas }
        val conversion = if (listaUsos.isNotEmpty()) conCompra.toDouble() / listaUsos.size else 0.0

        val ventaPerdida = Math.round(abandonosVenta * conversion * parametros.ticketPromedio)

        return ResumenDia(
            ingresos = ingresos,
            atendidos = atendidos.size,
            abandonos = abandonos.size,
            porcentajeAbandono = porcentaje,
            esperaPromedio = esperaPromedio,
            usosCerrados = listaUsos.size,
            usoPromedio = promedioUso(listaUsos),
            conversion = conversion * 100,
            abandonosVentaPerdida = abandonosVenta,
            ventaPerdida = ventaPerdida
        )
    }

    // formato chileno de pesos: 55980 -> $55.980
    fun formatoPesos(monto: Long): String {
        val texto = monto.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$$texto"
    }
}
