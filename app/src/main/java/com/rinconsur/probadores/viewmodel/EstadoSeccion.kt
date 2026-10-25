package com.rinconsur.probadores.viewmodel

import com.rinconsur.probadores.data.local.ParametrosEntity
import com.rinconsur.probadores.data.local.ProbadorEntity
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.data.local.UsoEntity
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.utils.Calculos
import com.rinconsur.probadores.utils.FechaUtil

// Como se ve un probador en el panel
data class ProbadorUi(
    val probador: ProbadorEntity,
    val minutos: Long,
    val uso: UsoEntity?,
    val turnoLlamado: TurnoEntity?,
    val prolongado: Boolean,
    val ofrecerAyuda: Boolean
)

// Como se ve un turno en la fila
data class TurnoUi(
    val turno: TurnoEntity,
    val minutosEsperando: Long,
    val estimado: Int
)

// Estado completo de la seccion que observan el panel y la pantalla del cliente
data class SeccionUiState(
    val probadores: List<ProbadorUi> = emptyList(),
    val fila: List<TurnoUi> = emptyList(),
    val libres: Int = 0,
    val habilitados: Int = 0,
    val enEspera: Int = 0,
    val estimadoNuevo: Int = 0,
    val parametros: ParametrosEntity = ParametrosEntity(),
    val cargando: Boolean = true
)

// Arma el estado de la seccion a partir de los datos de Room y la hora actual.
// Se usa en PanelViewModel y en ClienteViewModel para que ambos vean lo mismo (HU-06 CA-2).
fun construirEstado(
    probadores: List<ProbadorEntity>,
    turnosActivos: List<TurnoEntity>,
    usosDelDia: List<UsoEntity>,
    parametros: ParametrosEntity,
    ahora: Long
): SeccionUiState {
    val usosAbiertos = usosDelDia.filter { it.horaTermino == null }
    val usosCerrados = usosDelDia.filter { it.cerrado() }
    val llamados = turnosActivos.filter { it.estado == EstadoTurno.LLAMADO }
    val enEspera = Calculos.ordenarFila(turnosActivos.filter { it.estado == EstadoTurno.EN_ESPERA })

    val listaProbadores = probadores.map { p ->
        val minutos = FechaUtil.minutosEntre(p.estadoDesde, ahora)
        val uso = usosAbiertos.find { it.codigoProbador == p.codigo }
        val prolongado = p.estado == EstadoProbador.OCUPADO && Calculos.esUsoProlongado(minutos, parametros)
        val ayuda = p.estado == EstadoProbador.OCUPADO &&
                Calculos.necesitaOfrecerAyuda(minutos, enEspera.size, parametros)
        ProbadorUi(
            probador = p,
            minutos = minutos,
            uso = uso,
            turnoLlamado = llamados.find { it.probadorLlamado == p.codigo },
            prolongado = prolongado,
            ofrecerAyuda = ayuda
        )
    }

    val habilitados = probadores.count { it.habilitado() }
    // libres que no tienen a nadie llamado esperando entrar
    val libres = listaProbadores.count { it.probador.estado == EstadoProbador.LIBRE && it.turnoLlamado == null }

    val fila = mutableListOf<TurnoUi>()
    // primero los llamados (ya van caminando al probador)
    for (t in llamados) {
        fila.add(TurnoUi(t, FechaUtil.minutosEntre(t.horaIngreso, ahora), 0))
    }
    for ((i, t) in enEspera.withIndex()) {
        val estimado = Calculos.tiempoEstimado(i, habilitados, libres, usosCerrados, parametros)
        fila.add(TurnoUi(t, FechaUtil.minutosEntre(t.horaIngreso, ahora), estimado))
    }

    return SeccionUiState(
        probadores = listaProbadores,
        fila = fila,
        libres = libres,
        habilitados = habilitados,
        enEspera = enEspera.size,
        estimadoNuevo = Calculos.tiempoEstimado(enEspera.size, habilitados, libres, usosCerrados, parametros),
        parametros = parametros,
        cargando = false
    )
}
