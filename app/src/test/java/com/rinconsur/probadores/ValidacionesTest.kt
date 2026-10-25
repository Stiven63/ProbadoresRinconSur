package com.rinconsur.probadores

import com.rinconsur.probadores.utils.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesTest {

    @Test
    fun rutsDelAnexoSonValidos() {
        assertNull(Validaciones.validarRut("16.842.311-K"))
        assertNull(Validaciones.validarRut("19.234.587-1"))
        assertNull(Validaciones.validarRut("12984561-9"))
        assertNull(Validaciones.validarRut("20118436-3"))
    }

    @Test
    fun rutConDvMaloEsInvalido() {
        assertNotNull(Validaciones.validarRut("16.842.311-1"))
        assertNotNull(Validaciones.validarRut("abc"))
        assertNotNull(Validaciones.validarRut(""))
    }

    @Test
    fun normalizarRut() {
        assertEquals("16842311-K", Validaciones.normalizarRut("16.842.311-k"))
    }

    @Test
    fun prendasEntreUnoYSeis() {
        assertNull(Validaciones.validarPrendas("6", 6))
        assertNotNull(Validaciones.validarPrendas("8", 6))   // HU-02 CA-2
        assertNotNull(Validaciones.validarPrendas("0", 6))
        assertNotNull(Validaciones.validarPrendas("dos", 6))
    }

    @Test
    fun devueltasNoPuedenSerMasQueIngresadas() {
        assertNull(Validaciones.validarDevueltas("3", 5))
        assertNotNull(Validaciones.validarDevueltas("6", 5)) // HU-04 CA-3
    }

    @Test
    fun minutosAtrasMaximoQuince() {
        assertNull(Validaciones.validarMinutosAtras("15"))
        assertNotNull(Validaciones.validarMinutosAtras("16"))
    }

    @Test
    fun franjaHoraria() {
        assertNull(Validaciones.validarFranja("", ""))
        assertNull(Validaciones.validarFranja("16", "18"))
        assertNotNull(Validaciones.validarFranja("18", "16"))
        assertNotNull(Validaciones.validarFranja("16", ""))
    }
}
