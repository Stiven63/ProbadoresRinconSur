package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.data.repository.SeccionRepository
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.MotivoAbandono
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.nativo.Notificaciones
import com.rinconsur.probadores.utils.Calculos
import com.rinconsur.probadores.utils.FechaUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PanelViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))
    private val hoy = FechaUtil.hoy()

    // la hora actual se actualiza cada 15 segundos para que los minutos avancen solos
    private val ahora = MutableStateFlow(FechaUtil.ahora())

    // mensajes para mostrar en el Snackbar
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    // Cada vez que cambia algo en Room (o pasa el tiempo) se recalcula todo el estado
    val estado: StateFlow<SeccionUiState> = combine(
        repositorio.probadores(),
        repositorio.turnosActivos(hoy),
        repositorio.usosDelDia(hoy),
        repositorio.parametros(),
        ahora
    ) { probadores, turnos, usos, parametros, hora ->
        construirEstado(probadores, turnos, usos, parametros, hora)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeccionUiState())

    // para no repetir la misma notificacion
    private val avisados = mutableSetOf<String>()

    init {
        Notificaciones.crearCanal(application)
        viewModelScope.launch {
            while (true) {
                delay(15000)
                ahora.value = FechaUtil.ahora()
                revisarAvisos()
            }
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    // ---------- Acciones del personal ----------

    // Llama al siguiente turno de la fila para el probador libre (HU-03 / RN-05)
    fun llamarSiguiente(codigo: String) {
        val actual = estado.value
        val probador = actual.probadores.find { it.probador.codigo == codigo }?.probador ?: return
        val p08Libre = actual.probadores.any {
            it.probador.accesible && it.probador.estado == EstadoProbador.LIBRE && it.turnoLlamado == null
        }
        val turnos = actual.fila.map { it.turno }
        val siguiente = Calculos.siguienteTurno(turnos, probador, p08Libre)
        if (siguiente == null) {
            _mensaje.value = "No hay nadie esperando en la fila"
            return
        }
        viewModelScope.launch {
            val error = repositorio.llamarTurno(siguiente.id, codigo)
            _mensaje.value = error ?: "Llamando a ${siguiente.codigoTurno()} al probador $codigo"
        }
    }

    // El turno llamado llego al probador
    fun sePresento(turno: TurnoEntity) {
        val codigo = turno.probadorLlamado ?: return
        viewModelScope.launch {
            val error = repositorio.asignarProbador(turno.id, codigo, Sesion.rut())
            _mensaje.value = error ?: "${turno.codigoTurno()} entró a $codigo"
        }
    }

    fun noSePresento(turno: TurnoEntity) {
        viewModelScope.launch {
            val error = repositorio.cerrarTurnoSinAtencion(turno.id, EstadoTurno.NO_PRESENTO, MotivoAbandono.NO_INFORMADO)
            _mensaje.value = error ?: "${turno.codigoTurno()} no se presentó"
        }
    }

    // HU-05
    fun registrarAbandono(turno: TurnoEntity, motivo: MotivoAbandono) {
        viewModelScope.launch {
            val error = repositorio.cerrarTurnoSinAtencion(turno.id, EstadoTurno.ABANDONO, motivo)
            _mensaje.value = error ?: "Abandono de ${turno.codigoTurno()} registrado"
        }
    }

    fun terminarOrdenamiento(codigo: String) {
        viewModelScope.launch {
            val error = repositorio.terminarOrdenamiento(codigo, Sesion.rut())
            _mensaje.value = error ?: "$codigo quedó libre"
        }
    }

    // ---------- Avisos automaticos (notificaciones) ----------

    private suspend fun revisarAvisos() {
        val actual = estado.value
        if (actual.cargando) return
        val parametros = actual.parametros
        val app = getApplication<Application>()

        // RN-07: llamado que no se presenta en 2 minutos
        for (t in actual.fila) {
            val llamado = t.turno
            if (llamado.estado == EstadoTurno.LLAMADO && llamado.horaLlamado != null) {
                val minutos = FechaUtil.minutosEntre(llamado.horaLlamado, FechaUtil.ahora())
                if (minutos >= parametros.minPresentarse) {
                    repositorio.cerrarTurnoSinAtencion(llamado.id, EstadoTurno.NO_PRESENTO, MotivoAbandono.NO_INFORMADO)
                    Notificaciones.mostrar(
                        app, llamado.numero,
                        "${llamado.codigoTurno()} no se presentó",
                        "Se llamará al siguiente turno para ${llamado.probadorLlamado}"
                    )
                }
            }
        }

        for (p in actual.probadores) {
            val codigo = p.probador.codigo
            val idNotif = 1000 + codigo.takeLast(2).toInt()

            // RN-08 y HU-07: uso prolongado
            if (p.uso != null) {
                if (p.ofrecerAyuda && avisados.add("ayuda-${p.uso.id}")) {
                    Notificaciones.mostrar(app, idNotif, "$codigo lleva ${p.minutos} min", "Hay gente esperando. Puedes acercarte a ofrecer ayuda.")
                } else if (p.prolongado && avisados.add("prolongado-${p.uso.id}")) {
                    Notificaciones.mostrar(app, idNotif, "Uso prolongado en $codigo", "Lleva ${p.minutos} minutos ocupado")
                }
            }

            // Aclaracion 8: probador libre mas de 1 minuto con gente en la fila
            if (p.probador.estado == EstadoProbador.LIBRE && p.turnoLlamado == null &&
                actual.enEspera > 0 && p.minutos >= 1 &&
                avisados.add("libre-$codigo-${p.probador.estadoDesde}")
            ) {
                Notificaciones.mostrar(app, idNotif, "$codigo está libre", "Hay ${actual.enEspera} personas esperando. Llama al siguiente turno.")
            }
        }

        // RN-12: cierre de la fila y de la tienda
        if (FechaUtil.yaPasoLaHora(parametros.horaUltimoIngreso) && avisados.add("cierre-fila-$hoy")) {
            Notificaciones.mostrar(app, 2000, "Fila cerrada", "Desde las ${parametros.horaUltimoIngreso} no se aceptan nuevos ingresos")
        }
        if (FechaUtil.yaPasoLaHora(parametros.horaCierre) && actual.enEspera > 0) {
            repositorio.cerrarFilaPorCierre(hoy)
        }
    }
}
