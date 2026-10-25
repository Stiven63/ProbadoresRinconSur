package com.rinconsur.probadores.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Funciones de ayuda para manejar fechas y horas
object FechaUtil {

    private val formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val formatoFechaChile = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

    fun ahora(): Long = System.currentTimeMillis()

    // fecha de hoy como se guarda en la base de datos
    fun hoy(): String = LocalDate.now().format(formatoFecha)

    fun hoyChile(): String = LocalDate.now().format(formatoFechaChile)

    // convierte "dd-MM-yyyy" (como lo escribe el usuario) a "yyyy-MM-dd"
    fun chileABase(fecha: String): String {
        val f = LocalDate.parse(fecha, formatoFechaChile)
        return f.format(formatoFecha)
    }

    // milisegundos de una fecha "yyyy-MM-dd" y hora "HH:mm"
    fun millis(fecha: String, hora: String): Long {
        val fechaHora = LocalDateTime.of(LocalDate.parse(fecha, formatoFecha), LocalTime.parse(hora, formatoHora))
        return fechaHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun horaTexto(millis: Long?): String {
        if (millis == null) return "—"
        val hora = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        return hora.format(formatoHora)
    }

    // hora del dia (0 a 23) de un momento
    fun horaDelDia(millis: Long): Int {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour
    }

    fun minutosEntre(desde: Long, hasta: Long): Long {
        return (hasta - desde) / 60000
    }

    // true si la hora actual ya paso la hora indicada ("20:45")
    fun yaPasoLaHora(hora: String, ahora: Long = ahora()): Boolean {
        val actual = Instant.ofEpochMilli(ahora).atZone(ZoneId.systemDefault()).toLocalTime()
        return !actual.isBefore(LocalTime.parse(hora, formatoHora))
    }
}
