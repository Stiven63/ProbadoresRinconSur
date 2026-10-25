package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.repository.SeccionRepository
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.utils.FechaUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Lo unico que ve el cliente: informacion agregada, sin datos de nadie (RN-11)
data class ClienteUiState(
    val libres: Int = 0,
    val enEspera: Int = 0,
    val estimado: Int = 0,
    val llamados: List<String> = emptyList(),   // ej: "T-024 → P-03"
    val cargando: Boolean = true
)

class ClienteViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))
    private val hoy = FechaUtil.hoy()
    private val ahora = MutableStateFlow(FechaUtil.ahora())

    val estado: StateFlow<ClienteUiState> = combine(
        repositorio.probadores(),
        repositorio.turnosActivos(hoy),
        repositorio.usosDelDia(hoy),
        repositorio.parametros(),
        ahora
    ) { probadores, turnos, usos, parametros, hora ->
        val seccion = construirEstado(probadores, turnos, usos, parametros, hora)
        val llamados = seccion.fila
            .filter { it.turno.estado == EstadoTurno.LLAMADO }
            .map { "${it.turno.codigoTurno()} → ${it.turno.probadorLlamado}" }
        ClienteUiState(
            libres = seccion.libres,
            enEspera = seccion.enEspera,
            estimado = seccion.estimadoNuevo,
            llamados = llamados,
            cargando = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClienteUiState())

    init {
        viewModelScope.launch {
            while (true) {
                delay(15000)
                ahora.value = FechaUtil.ahora()
            }
        }
    }
}
