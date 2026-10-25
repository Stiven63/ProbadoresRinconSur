package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.repository.ColaboradorRepository
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val rut: String = "",
    val clave: String = "",
    val errorRut: String? = null,
    val errorClave: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val ingresoOk: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = ColaboradorRepository(AppDatabase.getDatabase(application).colaboradorDao())

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    // se valida mientras escribe (si el campo esta vacio no se marca error todavia)
    fun cambiarRut(valor: String) {
        _estado.update {
            it.copy(
                rut = valor,
                errorRut = if (valor.isEmpty()) null else Validaciones.validarRut(valor),
                errorGeneral = null
            )
        }
    }

    fun cambiarClave(valor: String) {
        _estado.update {
            it.copy(
                clave = valor,
                errorClave = if (valor.isEmpty()) null else Validaciones.validarClave(valor),
                errorGeneral = null
            )
        }
    }

    fun ingresar() {
        val actual = _estado.value
        val errorRut = Validaciones.validarRut(actual.rut)
        val errorClave = Validaciones.validarClave(actual.clave)
        _estado.update { it.copy(errorRut = errorRut, errorClave = errorClave) }
        if (errorRut != null || errorClave != null) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val (colaborador, error) = repositorio.login(actual.rut, actual.clave)
            if (colaborador != null) {
                Sesion.usuario = colaborador
                _estado.update { LoginUiState(ingresoOk = true) }
            } else {
                _estado.update { it.copy(cargando = false, errorGeneral = error) }
            }
        }
    }

    fun ingresoProcesado() {
        _estado.update { it.copy(ingresoOk = false) }
    }
}
