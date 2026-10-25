package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.local.ProbadorEntity
import com.rinconsur.probadores.data.local.UsoEntity
import com.rinconsur.probadores.data.repository.SeccionRepository
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.utils.FechaUtil
import com.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel para los 3 formularios que actuan sobre un probador:
// terminar uso (HU-04), ingreso directo sin fila (Aclaracion 1 y 3) y fuera de servicio (HU-08)
data class ProbadorFormUiState(
    val probador: ProbadorEntity? = null,
    val usoAbierto: UsoEntity? = null,
    val maxPrendas: Int = 6,
    val hayFila: Boolean = false,

    // terminar uso
    val devueltas: String = "",
    val errorDevueltas: String? = null,

    // ingreso directo
    val prendas: String = "",
    val errorPrendas: String? = null,
    val minutosAtras: String = "0",
    val errorMinutos: String? = null,

    // fuera de servicio
    val motivo: String = "",
    val errorMotivo: String? = null,
    val fotoRuta: String? = null,

    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    val listo: Boolean = false
)

class ProbadorFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))

    private val _estado = MutableStateFlow(ProbadorFormUiState())
    val estado: StateFlow<ProbadorFormUiState> = _estado.asStateFlow()

    private var cargado = false

    fun cargar(codigo: String) {
        if (cargado) return
        cargado = true
        viewModelScope.launch {
            val parametros = repositorio.obtenerParametros()
            val turnos = repositorio.turnosActivos(FechaUtil.hoy()).first()
            _estado.update {
                it.copy(maxPrendas = parametros.maxPrendas, hayFila = turnos.any { t -> t.estado == EstadoTurno.EN_ESPERA })
            }
        }
        viewModelScope.launch {
            repositorio.probador(codigo).collect { p -> _estado.update { it.copy(probador = p) } }
        }
        viewModelScope.launch {
            repositorio.usoAbierto(codigo).collect { u -> _estado.update { it.copy(usoAbierto = u) } }
        }
    }

    // ---------- Terminar uso ----------

    fun cambiarDevueltas(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        val ingresadas = _estado.value.usoAbierto?.prendasIngresadas ?: 0
        _estado.update {
            it.copy(devueltas = limpio, errorDevueltas = if (limpio.isEmpty()) null else Validaciones.validarDevueltas(limpio, ingresadas))
        }
    }

    fun terminarUso() {
        val actual = _estado.value
        val uso = actual.usoAbierto ?: return
        val error = Validaciones.validarDevueltas(actual.devueltas, uso.prendasIngresadas)
        _estado.update { it.copy(errorDevueltas = error) }
        if (error != null) return
        ejecutar { repositorio.terminarUso(uso.codigoProbador, actual.devueltas.toInt(), Sesion.rut()) }
    }

    // ---------- Ingreso directo ----------

    fun cambiarPrendas(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        _estado.update {
            it.copy(prendas = limpio, errorPrendas = if (limpio.isEmpty()) null else Validaciones.validarPrendas(limpio, it.maxPrendas))
        }
    }

    fun cambiarMinutos(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        _estado.update { it.copy(minutosAtras = limpio, errorMinutos = Validaciones.validarMinutosAtras(limpio)) }
    }

    fun ingresoDirecto() {
        val actual = _estado.value
        val codigo = actual.probador?.codigo ?: return
        // RN-04: fila unica, si hay gente esperando no se puede entrar directo
        if (actual.hayFila) {
            _estado.update { it.copy(errorGeneral = "Hay personas en la fila. Llama al siguiente turno.") }
            return
        }
        val errorPrendas = Validaciones.validarPrendas(actual.prendas, actual.maxPrendas)
        val errorMinutos = Validaciones.validarMinutosAtras(actual.minutosAtras)
        _estado.update { it.copy(errorPrendas = errorPrendas, errorMinutos = errorMinutos) }
        if (errorPrendas != null || errorMinutos != null) return
        ejecutar {
            repositorio.ingresoDirecto(codigo, actual.prendas.toInt(), actual.minutosAtras.toInt(), Sesion.rut())
        }
    }

    // ---------- Fuera de servicio ----------

    fun cambiarMotivo(valor: String) {
        _estado.update { it.copy(motivo = valor, errorMotivo = if (valor.isEmpty()) null else Validaciones.validarMotivo(valor)) }
    }

    fun fotoTomada(ruta: String?) {
        _estado.update { it.copy(fotoRuta = ruta) }
    }

    fun marcarFueraDeServicio() {
        val actual = _estado.value
        val codigo = actual.probador?.codigo ?: return
        // RN-11 y HU-08 CA-2: solo la jefatura
        if (!Sesion.esJefatura()) {
            _estado.update { it.copy(errorGeneral = "Solo la jefatura puede dejar un probador fuera de servicio. Infórmale la falla.") }
            return
        }
        val error = Validaciones.validarMotivo(actual.motivo)
        _estado.update { it.copy(errorMotivo = error) }
        if (error != null) return
        ejecutar { repositorio.marcarFueraDeServicio(codigo, actual.motivo.trim(), actual.fotoRuta, Sesion.rut()) }
    }

    fun habilitar() {
        val codigo = _estado.value.probador?.codigo ?: return
        if (!Sesion.esJefatura()) {
            _estado.update { it.copy(errorGeneral = "Solo la jefatura puede habilitar un probador") }
            return
        }
        ejecutar { repositorio.habilitar(codigo, Sesion.rut()) }
    }

    // corre la accion del repositorio y muestra el error si hubo
    private fun ejecutar(accion: suspend () -> String?) {
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorGeneral = null) }
            val error = accion()
            _estado.update { it.copy(guardando = false, errorGeneral = error, listo = error == null) }
        }
    }
}
