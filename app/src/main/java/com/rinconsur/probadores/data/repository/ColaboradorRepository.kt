package com.rinconsur.probadores.data.repository

import com.rinconsur.probadores.data.local.ColaboradorDao
import com.rinconsur.probadores.data.local.ColaboradorEntity
import com.rinconsur.probadores.model.Perfil
import com.rinconsur.probadores.utils.Seguridad
import com.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.flow.Flow

class ColaboradorRepository(private val dao: ColaboradorDao) {

    fun obtenerTodos(): Flow<List<ColaboradorEntity>> = dao.obtenerTodos()

    // Devuelve el colaborador si el rut y la clave son correctos, o un mensaje de error
    suspend fun login(rut: String, clave: String): Pair<ColaboradorEntity?, String?> {
        val rutLimpio = Validaciones.normalizarRut(rut)
        val colaborador = dao.buscarPorRut(rutLimpio)
            ?: return Pair(null, "RUT o clave incorrectos")

        if (colaborador.claveHash != Seguridad.hashClave(clave)) {
            return Pair(null, "RUT o clave incorrectos")
        }
        // RN-13: un colaborador inactivo no puede registrar acciones
        if (!colaborador.activo) {
            return Pair(null, "Tu usuario está inactivo. Habla con la jefatura.")
        }
        return Pair(colaborador, null)
    }

    // Devuelve null si se guardo bien, o el mensaje de error
    suspend fun registrar(rut: String, nombre: String, perfil: Perfil, turno: String, clave: String): String? {
        val rutLimpio = Validaciones.normalizarRut(rut)
        if (dao.buscarPorRut(rutLimpio) != null) {
            return "Ya existe un colaborador con ese RUT"
        }
        dao.insertar(
            ColaboradorEntity(
                rut = rutLimpio,
                nombre = nombre.trim(),
                perfil = perfil,
                turnoLaboral = turno,
                activo = true,
                claveHash = Seguridad.hashClave(clave)
            )
        )
        return null
    }

    suspend fun cambiarActivo(colaborador: ColaboradorEntity) {
        dao.actualizar(colaborador.copy(activo = !colaborador.activo))
    }
}
