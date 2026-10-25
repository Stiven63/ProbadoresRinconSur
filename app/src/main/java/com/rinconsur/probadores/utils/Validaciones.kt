package com.rinconsur.probadores.utils

// Todas las validaciones de los formularios estan aca, separadas de las pantallas.
// Cada funcion devuelve null si el dato esta bien, o el mensaje de error para mostrar.
object Validaciones {

    // Deja el rut como 12345678-K (sin puntos, con guion y K mayuscula)
    fun normalizarRut(rut: String): String {
        val limpio = rut.replace(".", "").replace("-", "").replace(" ", "").uppercase()
        if (limpio.length < 2) return limpio
        return limpio.substring(0, limpio.length - 1) + "-" + limpio.last()
    }

    // Calcula el digito verificador con el algoritmo modulo 11
    fun calcularDv(cuerpo: Int): Char {
        var suma = 0
        var multiplicador = 2
        var numero = cuerpo
        while (numero > 0) {
            suma += (numero % 10) * multiplicador
            numero /= 10
            multiplicador++
            if (multiplicador > 7) multiplicador = 2
        }
        val resto = 11 - (suma % 11)
        return when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> resto.toString()[0]
        }
    }

    // RN-13: RUT con digito verificador valido
    fun validarRut(rut: String): String? {
        if (rut.isBlank()) return "Ingresa tu RUT"
        val limpio = rut.replace(".", "").replace("-", "").replace(" ", "").uppercase()
        if (limpio.length < 8 || limpio.length > 9) return "El RUT debe tener entre 8 y 9 caracteres"
        val cuerpo = limpio.substring(0, limpio.length - 1)
        val dv = limpio.last()
        if (!cuerpo.all { it.isDigit() }) return "El RUT solo puede tener números antes del guion"
        if (!(dv.isDigit() || dv == 'K')) return "El dígito verificador debe ser un número o K"
        if (calcularDv(cuerpo.toInt()) != dv) return "El dígito verificador no es válido"
        return null
    }

    fun validarClave(clave: String): String? {
        if (clave.isBlank()) return "Ingresa tu clave"
        if (clave.length < 4) return "La clave debe tener al menos 4 caracteres"
        return null
    }

    fun validarNombre(nombre: String): String? {
        val n = nombre.trim()
        if (n.isEmpty()) return "Ingresa el nombre completo"
        if (n.length < 5) return "El nombre es muy corto"
        if (!n.all { it.isLetter() || it == ' ' }) return "El nombre solo puede tener letras"
        if (!n.contains(" ")) return "Ingresa nombre y apellido"
        return null
    }

    // RN-02: minimo 1 y maximo 6 prendas (el maximo viene de los parametros)
    fun validarPrendas(texto: String, maximo: Int): String? {
        if (texto.isBlank()) return "Ingresa la cantidad de prendas"
        val cantidad = texto.toIntOrNull() ?: return "Debe ser un número"
        if (cantidad < 1) return "Debe ingresar al menos 1 prenda"
        if (cantidad > maximo) return "El máximo es $maximo prendas por ingreso"
        return null
    }

    // RN-02: las devueltas nunca pueden ser mas que las ingresadas
    fun validarDevueltas(texto: String, ingresadas: Int): String? {
        if (texto.isBlank()) return "Ingresa cuántas prendas devolvió"
        val cantidad = texto.toIntOrNull() ?: return "Debe ser un número"
        if (cantidad < 0) return "No puede ser negativo"
        if (cantidad > ingresadas) return "Ingresó $ingresadas prendas, no puede devolver $cantidad"
        return null
    }

    // Aclaracion 3: maximo 15 minutos hacia atras
    fun validarMinutosAtras(texto: String): String? {
        if (texto.isBlank()) return "Ingresa los minutos (0 si entró recién)"
        val minutos = texto.toIntOrNull() ?: return "Debe ser un número"
        if (minutos < 0) return "No puede ser negativo"
        if (minutos > 15) return "Solo se puede registrar hasta 15 minutos hacia atrás"
        return null
    }

    fun validarMotivo(texto: String): String? {
        val t = texto.trim()
        if (t.isEmpty()) return "Debes indicar el motivo"
        if (t.length < 5) return "Describe el motivo con más detalle"
        if (t.length > 100) return "Máximo 100 caracteres"
        return null
    }

    // RN-12: no se aceptan ingresos desde 15 min antes del cierre
    fun validarHorarioFila(ahora: Long, horaUltimoIngreso: String): String? {
        if (FechaUtil.yaPasoLaHora(horaUltimoIngreso, ahora)) {
            return "La fila ya está cerrada (último ingreso $horaUltimoIngreso)"
        }
        return null
    }

    // Para los parametros de la seccion (RD-05)
    fun validarEnteroRango(texto: String, nombre: String, minimo: Int, maximo: Int): String? {
        if (texto.isBlank()) return "Ingresa $nombre"
        val valor = texto.toIntOrNull() ?: return "Debe ser un número entero"
        if (valor < minimo || valor > maximo) return "Debe estar entre $minimo y $maximo"
        return null
    }

    fun validarHora(texto: String): String? {
        val regex = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
        if (!regex.matches(texto)) return "Usa el formato HH:mm (ej: 20:45)"
        return null
    }

    fun validarFecha(texto: String): String? {
        val regex = Regex("^\\d{2}-\\d{2}-\\d{4}$")
        if (!regex.matches(texto)) return "Usa el formato dd-mm-aaaa"
        return try {
            FechaUtil.chileABase(texto)
            null
        } catch (e: Exception) {
            "La fecha no existe"
        }
    }

    // franja horaria opcional: si se llena una hora, se debe llenar la otra
    fun validarFranja(desde: String, hasta: String): String? {
        if (desde.isBlank() && hasta.isBlank()) return null
        val d = desde.toIntOrNull()
        val h = hasta.toIntOrNull()
        if (d == null || h == null) return "Indica ambas horas (ej: 16 y 18)"
        if (d < 10 || h > 21) return "La tienda abre de 10 a 21 hrs"
        if (d >= h) return "La hora de inicio debe ser menor que la de término"
        return null
    }
}
