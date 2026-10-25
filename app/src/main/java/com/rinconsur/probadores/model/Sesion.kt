package com.rinconsur.probadores.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rinconsur.probadores.data.local.ColaboradorEntity

// Guarda quien inicio sesion mientras la app esta abierta (RF-10)
object Sesion {
    var usuario by mutableStateOf<ColaboradorEntity?>(null)

    fun rut(): String = usuario?.rut ?: ""

    fun esJefatura(): Boolean = usuario?.perfil == Perfil.JEFATURA

    fun cerrar() {
        usuario = null
    }
}
