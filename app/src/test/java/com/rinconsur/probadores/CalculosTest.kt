package com.rinconsur.probadores

import com.rinconsur.probadores.data.local.ParametrosEntity
import com.rinconsur.probadores.data.local.ProbadorEntity
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.data.local.UsoEntity
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.MotivoAbandono
import com.rinconsur.probadores.utils.Calculos
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculosTest {

    private val parametros = ParametrosEntity()
    private val minuto = 60000L

    private fun turno(numero: Int, preferente: Boolean = false, estado: EstadoTurno = EstadoTurno.EN_ESPERA,
                      motivo: MotivoAbandono? = null, espera: Int = 5): TurnoEntity {
        return TurnoEntity(
            id = numero.toLong(), fecha = "2026-09-26", numero = numero, horaIngreso = 0,
            preferente = preferente, prendas = 3, estado = estado,
            horaCierre = if (estado == EstadoTurno.EN_ESPERA) null else espera * minuto,
            motivoAbandono = motivo, rutRegistro = "1-9"
        )
    }

    private fun uso(minutos: Int, ingresadas: Int, devueltas: Int): UsoEntity {
        return UsoEntity(
            codigoProbador = "P-01", fecha = "2026-09-26", horaInicio = 0,
            horaTermino = minutos * minuto, prendasIngresadas = ingresadas,
            prendasDevueltas = devueltas, rutInicio = "1-9"
        )
    }

    @Test
    fun hayProbadorLibreNoSeEspera() {
        // HU-01 CA-2
        assertEquals(0, Calculos.tiempoEstimado(0, 7, 2, emptyList(), parametros))
    }

    @Test
    fun sinDatosUsaTiempoDeReferencia() {
        // HU-01 CA-1 y CA-3: 4 personas antes, 7 habilitados -> (4+1)/7*(8+1) = 6.43 -> 7
        assertEquals(7, Calculos.tiempoEstimado(4, 7, 0, emptyList(), parametros))
    }

    @Test
    fun conDiezUsosUsaElPromedioDelDia() {
        val usos = List(10) { uso(12, 3, 1) }
        // (0+1)/7*(12+1) = 1.86 -> 2
        assertEquals(2, Calculos.tiempoEstimado(0, 7, 0, usos, parametros))
    }

    @Test
    fun sinProbadoresHabilitados() {
        assertEquals(-1, Calculos.tiempoEstimado(3, 0, 0, emptyList(), parametros))
    }

    @Test
    fun usoProlongado() {
        // HU-07
        assertEquals(true, Calculos.esUsoProlongado(16, parametros))
        assertEquals(false, Calculos.esUsoProlongado(15, parametros))
        assertEquals(true, Calculos.necesitaOfrecerAyuda(26, 3, parametros))
        assertEquals(false, Calculos.necesitaOfrecerAyuda(26, 0, parametros))
    }

    @Test
    fun preferenteVaPrimeroAP08() {
        val fila = listOf(turno(1), turno(2, preferente = true))
        val p08 = ProbadorEntity("P-08", true, "Fondo", EstadoProbador.LIBRE, 0)
        val p01 = ProbadorEntity("P-01", false, "Mesón", EstadoProbador.LIBRE, 0)
        assertEquals(2, Calculos.siguienteTurno(fila, p08, true)?.numero)
        // si P-08 esta libre el preferente espera P-08, el estandar va al normal
        assertEquals(1, Calculos.siguienteTurno(fila, p01, true)?.numero)
        // si P-08 esta ocupado, el preferente pasa primero
        assertEquals(2, Calculos.siguienteTurno(fila, p01, false)?.numero)
    }

    @Test
    fun resumenDelAnexoA() {
        // 12 llegaron, 3 se fueron, 6 de 9 compraron
        val turnos = mutableListOf<TurnoEntity>()
        for (i in 1..9) turnos.add(turno(i, estado = EstadoTurno.ATENDIDO))
        for (i in 10..12) turnos.add(turno(i, estado = EstadoTurno.ABANDONO, motivo = MotivoAbandono.ESPERA_LARGA))
        val usos = listOf(
            uso(9, 3, 1), uso(13, 5, 5), uso(18, 6, 4), uso(4, 1, 0), uso(8, 3, 3),
            uso(5, 2, 0), uso(12, 4, 2), uso(6, 3, 3), uso(14, 5, 1)
        )
        val r = Calculos.resumen(turnos, usos, parametros)
        assertEquals(12, r.ingresos)
        assertEquals(3, r.abandonos)
        assertEquals(25.0, r.porcentajeAbandono, 0.01)
        assertEquals(66.67, r.conversion, 0.01)
        // 3 * 0.6667 * 27990 = 55980
        assertEquals(55980L, r.ventaPerdida)
    }

    @Test
    fun comproSinProbarseNoEsVentaPerdida() {
        val turnos = listOf(
            turno(1, estado = EstadoTurno.ATENDIDO),
            turno(2, estado = EstadoTurno.ABANDONO, motivo = MotivoAbandono.COMPRO_SIN_PROBARSE)
        )
        val r = Calculos.resumen(turnos, listOf(uso(5, 2, 0)), parametros)
        assertEquals(1, r.abandonos)
        assertEquals(0, r.abandonosVentaPerdida)
        assertEquals(0L, r.ventaPerdida)
    }

    @Test
    fun formatoPesos() {
        assertEquals("$27.990", Calculos.formatoPesos(27990))
        assertEquals("$1.055.980", Calculos.formatoPesos(1055980))
        assertEquals("$0", Calculos.formatoPesos(0))
    }
}
