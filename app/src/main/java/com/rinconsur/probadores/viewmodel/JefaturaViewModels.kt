package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.local.ColaboradorEntity
import com.rinconsur.probadores.data.local.ParametrosEntity
import com.rinconsur.probadores.data.repository.ColaboradorRepository
import com.rinconsur.probadores.data.repository.SeccionRepository
import com.rinconsur.probadores.model.Perfil
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.utils.Calculos
import com.rinconsur.probadores.utils.FechaUtil
import com.rinconsur.probadores.utils.ResumenDia
import com.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ===================== Resumen del dia (RF-09 / HU-09) =====================

data class ResumenUiState(
    val fecha: String = FechaUtil.hoyChile(),
    val desde: String = "",
    val hasta: String = "",
    val errorFecha: String? = null,
    val errorFranja: String? = null,
    val resumen: ResumenDia? = null,
    val ticket: Int = 27990,
    val cargando: Boolean = false
)

class ResumenViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))

    private val _estado = MutableStateFlow(ResumenUiState())
    val estado: StateFlow<ResumenUiState> = _estado.asStateFlow()

    init {
        consultar()
    }

    fun cambiarFecha(valor: String) {
        _estado.update { it.copy(fecha = valor, errorFecha = Validaciones.validarFecha(valor)) }
    }

    fun cambiarDesde(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        _estado.update { it.copy(desde = limpio, errorFranja = Validaciones.validarFranja(limpio, it.hasta)) }
    }

    fun cambiarHasta(valor: String) {
        val limpio = valor.filter { it.isDigit() }.take(2)
        _estado.update { it.copy(hasta = limpio, errorFranja = Validaciones.validarFranja(it.desde, limpio)) }
    }

    fun consultar() {
        val actual = _estado.value
        val errorFecha = Validaciones.validarFecha(actual.fecha)
        val errorFranja = Validaciones.validarFranja(actual.desde, actual.hasta)
        _estado.update { it.copy(errorFecha = errorFecha, errorFranja = errorFranja) }
        if (errorFecha != null || errorFranja != null) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val fecha = FechaUtil.chileABase(actual.fecha)
            val parametros = repositorio.obtenerParametros()
            val resumen = Calculos.resumen(
                repositorio.turnosDelDia(fecha),
                repositorio.usosCerradosDelDia(fecha),
                parametros,
                actual.desde.toIntOrNull(),
                actual.hasta.toIntOrNull()
            )
            _estado.update { it.copy(resumen = resumen, ticket = parametros.ticketPromedio, cargando = false) }
        }
    }
}

// ===================== Parametros de la seccion (RD-05) =====================

data class ParametrosUiState(
    val maxPrendas: String = "",
    val tiempoRef: String = "",
    val ordenamiento: String = "",
    val umbralProlongado: String = "",
    val umbralAyuda: String = "",
    val ticket: String = "",
    val ultimoIngreso: String = "",
    val errores: Map<String, String?> = emptyMap(),
    val guardado: Boolean = false
)

class ParametrosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SeccionRepository(AppDatabase.getDatabase(application))
    private var original = ParametrosEntity()

    private val _estado = MutableStateFlow(ParametrosUiState())
    val estado: StateFlow<ParametrosUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val p = repositorio.obtenerParametros()
            original = p
            _estado.value = ParametrosUiState(
                maxPrendas = p.maxPrendas.toString(),
                tiempoRef = p.tiempoRefUso.toString(),
                ordenamiento = p.minOrdenamiento.toString(),
                umbralProlongado = p.umbralProlongado.toString(),
                umbralAyuda = p.umbralAyuda.toString(),
                ticket = p.ticketPromedio.toString(),
                ultimoIngreso = p.horaUltimoIngreso
            )
        }
    }

    // un solo metodo para todos los campos, segun el nombre del campo
    fun cambiar(campo: String, valor: String) {
        _estado.update {
            val nuevo = when (campo) {
                "maxPrendas" -> it.copy(maxPrendas = valor)
                "tiempoRef" -> it.copy(tiempoRef = valor)
                "ordenamiento" -> it.copy(ordenamiento = valor)
                "umbralProlongado" -> it.copy(umbralProlongado = valor)
                "umbralAyuda" -> it.copy(umbralAyuda = valor)
                "ticket" -> it.copy(ticket = valor)
                else -> it.copy(ultimoIngreso = valor)
            }
            nuevo.copy(errores = validar(nuevo), guardado = false)
        }
    }

    private fun validar(e: ParametrosUiState): Map<String, String?> {
        val errores = mutableMapOf<String, String?>()
        errores["maxPrendas"] = Validaciones.validarEnteroRango(e.maxPrendas, "el máximo de prendas", 1, 10)
        errores["tiempoRef"] = Validaciones.validarEnteroRango(e.tiempoRef, "el tiempo de referencia", 1, 60)
        errores["ordenamiento"] = Validaciones.validarEnteroRango(e.ordenamiento, "los minutos de ordenamiento", 0, 10)
        errores["umbralProlongado"] = Validaciones.validarEnteroRango(e.umbralProlongado, "el umbral", 5, 60)
        errores["umbralAyuda"] = Validaciones.validarEnteroRango(e.umbralAyuda, "el umbral de ayuda", 5, 90)
        errores["ticket"] = Validaciones.validarEnteroRango(e.ticket, "el ticket promedio", 1000, 1000000)
        errores["ultimoIngreso"] = Validaciones.validarHora(e.ultimoIngreso)
        // el umbral de ayuda tiene que ser mayor que el de uso prolongado
        val prolongado = e.umbralProlongado.toIntOrNull()
        val ayuda = e.umbralAyuda.toIntOrNull()
        if (errores["umbralAyuda"] == null && prolongado != null && ayuda != null && ayuda <= prolongado) {
            errores["umbralAyuda"] = "Debe ser mayor que el umbral de uso prolongado"
        }
        return errores
    }

    fun guardar() {
        val actual = _estado.value
        val errores = validar(actual)
        _estado.update { it.copy(errores = errores) }
        if (errores.values.any { it != null }) return

        viewModelScope.launch {
            repositorio.guardarParametros(
                original.copy(
                    maxPrendas = actual.maxPrendas.toInt(),
                    tiempoRefUso = actual.tiempoRef.toInt(),
                    minOrdenamiento = actual.ordenamiento.toInt(),
                    umbralProlongado = actual.umbralProlongado.toInt(),
                    umbralAyuda = actual.umbralAyuda.toInt(),
                    ticketPromedio = actual.ticket.toInt(),
                    horaUltimoIngreso = actual.ultimoIngreso
                )
            )
            _estado.update { it.copy(guardado = true) }
        }
    }
}

// ===================== Colaboradores (RF-10 / RN-13) =====================

data class ColaboradoresUiState(
    val rut: String = "",
    val nombre: String = "",
    val perfil: Perfil = Perfil.ASISTENTE,
    val turno: String = "Mañana",
    val clave: String = "",
    val errorRut: String? = null,
    val errorNombre: String? = null,
    val errorClave: String? = null,
    val mensaje: String? = null
)

class ColaboradoresViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = ColaboradorRepository(AppDatabase.getDatabase(application).colaboradorDao())

    val lista: StateFlow<List<ColaboradorEntity>> = repositorio.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _estado = MutableStateFlow(ColaboradoresUiState())
    val estado: StateFlow<ColaboradoresUiState> = _estado.asStateFlow()

    fun cambiarRut(v: String) = _estado.update { it.copy(rut = v, errorRut = if (v.isEmpty()) null else Validaciones.validarRut(v)) }
    fun cambiarNombre(v: String) = _estado.update { it.copy(nombre = v, errorNombre = if (v.isEmpty()) null else Validaciones.validarNombre(v)) }
    fun cambiarClave(v: String) = _estado.update { it.copy(clave = v, errorClave = if (v.isEmpty()) null else Validaciones.validarClave(v)) }
    fun cambiarPerfil(p: Perfil) = _estado.update { it.copy(perfil = p) }
    fun cambiarTurno(t: String) = _estado.update { it.copy(turno = t) }

    fun guardar() {
        val e = _estado.value
        val errorRut = Validaciones.validarRut(e.rut)
        val errorNombre = Validaciones.validarNombre(e.nombre)
        val errorClave = Validaciones.validarClave(e.clave)
        _estado.update { it.copy(errorRut = errorRut, errorNombre = errorNombre, errorClave = errorClave, mensaje = null) }
        if (errorRut != null || errorNombre != null || errorClave != null) return

        viewModelScope.launch {
            val error = repositorio.registrar(e.rut, e.nombre, e.perfil, e.turno, e.clave)
            if (error == null) {
                _estado.value = ColaboradoresUiState(mensaje = "Colaborador agregado")
            } else {
                _estado.update { it.copy(mensaje = error) }
            }
        }
    }

    fun cambiarActivo(colaborador: ColaboradorEntity) {
        if (colaborador.rut == Sesion.rut()) {
            _estado.update { it.copy(mensaje = "No puedes desactivar tu propio usuario") }
            return
        }
        viewModelScope.launch { repositorio.cambiarActivo(colaborador) }
    }
}
