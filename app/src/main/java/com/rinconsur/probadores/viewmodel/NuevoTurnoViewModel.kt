package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.repository.SeccionRepository
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.utils.FechaUtil
import com.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevoTurnoUiState(
    val prendas: String = "",
    val preferente: Boolean = false,
    val errorPrendas: String? = null,
    val errorGeneral: String? = null,
    val maxPrendas: Int = 6,
    val guardando: Boolean = false,
    val turnoCreado: String? = null,      // ej: T-025
    val estimado: Int = 0
)

class NuevoTurnoViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))

    private val _estado = MutableStateFlow(NuevoTurnoUiState())
    val estado: StateFlow<NuevoTurnoUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val parametros = repositorio.obtenerParametros()
            _estado.update { it.copy(maxPrendas = parametros.maxPrendas) }
        }
    }

    fun cambiarPrendas(valor: String) {
        // solo deja escribir numeros
        val limpio = valor.filter { it.isDigit() }.take(2)
        _estado.update {
            it.copy(
                prendas = limpio,
                errorPrendas = if (limpio.isEmpty()) null else Validaciones.validarPrendas(limpio, it.maxPrendas)
            )
        }
    }

    fun sumarPrenda(cantidad: Int) {
        val actual = _estado.value.prendas.toIntOrNull() ?: 0
        val nuevo = (actual + cantidad).coerceAtLeast(0)
        cambiarPrendas(nuevo.toString())
    }

    fun cambiarPreferente(valor: Boolean) {
        _estado.update { it.copy(preferente = valor) }
    }

    fun guardar() {
        val actual = _estado.value
        val errorPrendas = Validaciones.validarPrendas(actual.prendas, actual.maxPrendas)
        _estado.update { it.copy(errorPrendas = errorPrendas, errorGeneral = null) }
        if (errorPrendas != null) return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val parametros = repositorio.obtenerParametros()

            // RN-12: la fila se cierra 15 min antes del cierre
            val errorHorario = Validaciones.validarHorarioFila(FechaUtil.ahora(), parametros.horaUltimoIngreso)
            if (errorHorario != null) {
                _estado.update { it.copy(guardando = false, errorGeneral = errorHorario) }
                return@launch
            }

            val turno = repositorio.registrarTurno(actual.prendas.toInt(), actual.preferente, Sesion.rut())

            // calcula cuanto va a esperar este turno con la fila actual (RF-05)
            val hoy = FechaUtil.hoy()
            val seccion = construirEstado(
                repositorio.probadores().first(),
                repositorio.turnosActivos(hoy).first(),
                repositorio.usosDelDia(hoy).first(),
                parametros,
                FechaUtil.ahora()
            )
            val estimado = seccion.fila.find { it.turno.id == turno.id }?.estimado ?: 0

            _estado.update {
                it.copy(guardando = false, turnoCreado = turno.codigoTurno(), estimado = estimado)
            }
        }
    }

    // deja el formulario limpio para registrar otro cliente
    fun nuevoRegistro() {
        _estado.update { NuevoTurnoUiState(maxPrendas = it.maxPrendas) }
    }
}
