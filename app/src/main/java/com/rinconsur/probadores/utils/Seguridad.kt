package com.rinconsur.probadores.utils

import java.security.MessageDigest

object Seguridad {

    // Genera el hash SHA-256 de la clave para no guardarla en texto plano
    fun hashClave(clave: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(clave.toByteArray())
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
