package com.rinconsur.probadores.data.repository

import androidx.room.withTransaction
import com.rinconsur.probadores.data.local.AppDatabase
import com.rinconsur.probadores.data.local.CambioEstadoEntity
import com.rinconsur.probadores.data.local.ParametrosEntity
import com.rinconsur.probadores.data.local.ProbadorEntity
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.data.local.UsoEntity
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.MotivoAbandono
import com.rinconsur.probadores.utils.FechaUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Repositorio principal: probadores, fila de espera, usos y parametros.
// Las operaciones que cambian varias tablas van dentro de una transaccion
// para que no quede nada a medias (por ejemplo un probador ocupado sin su uso).
class SeccionRepository(private val db: AppDatabase) {

    private val probadorDao = db.probadorDao()
    private val turnoDao = db.turnoDao()
    private val usoDao = db.usoDao()
    private val cambioDao = db.cambioEstadoDao()
    private val parametrosDao = db.parametrosDao()

    // ---------- Consultas ----------

    fun probadores(): Flow<List<ProbadorEntity>> = probadorDao.obtenerTodos()

    fun probador(codigo: String): Flow<ProbadorEntity?> = probadorDao.observar(codigo)

    fun turnosActivos(fecha: String): Flow<List<TurnoEntity>> = turnoDao.obtenerActivos(fecha)

    fun usosDelDia(fecha: String): Flow<List<UsoEntity>> = usoDao.observarDelDia(fecha)

    fun usoAbierto(codigo: String): Flow<UsoEntity?> = usoDao.observarUsoAbierto(codigo)

    fun parametros(): Flow<ParametrosEntity> = parametrosDao.observar().map { it ?: ParametrosEntity() }

    suspend fun obtenerParametros(): ParametrosEntity = parametrosDao.obtener() ?: ParametrosEntity()

    suspend fun turnosDelDia(fecha: String): List<TurnoEntity> = turnoDao.obtenerDelDia(fecha)

    suspend fun usosCerradosDelDia(fecha: String): List<UsoEntity> {
        return usoDao.obtenerDelDia(fecha).filter { it.cerrado() }
    }

    // ---------- Fila de espera ----------

    // HU-02: registra un nuevo turno con el siguiente numero del dia (RN-04)
    suspend fun registrarTurno(prendas: Int, preferente: Boolean, rut: String): TurnoEntity {
        return db.withTransaction {
            val hoy = FechaUtil.hoy()
            val numero = (turnoDao.ultimoNumero(hoy) ?: 0) + 1
            val turno = TurnoEntity(
                fecha = hoy,
                numero = numero,
                horaIngreso = FechaUtil.ahora(),
                preferente = preferente,
                prendas = prendas,
                estado = EstadoTurno.EN_ESPERA,
                rutRegistro = rut
            )
            val id = turnoDao.insertar(turno)
            turno.copy(id = id)
        }
    }

    // Llama al turno para que vaya a un probador libre
    suspend fun llamarTurno(idTurno: Long, codigoProbador: String): String? {
        return db.withTransaction {
            val turno = turnoDao.buscar(idTurno) ?: return@withTransaction "El turno no existe"
            val probador = probadorDao.buscar(codigoProbador) ?: return@withTransaction "El probador no existe"
            if (turno.estado != EstadoTurno.EN_ESPERA) return@withTransaction "El turno ya no está en espera"
            if (probador.estado != EstadoProbador.LIBRE) return@withTransaction "${probador.codigo} ya no está libre"
            turnoDao.actualizar(
                turno.copy(
                    estado = EstadoTurno.LLAMADO,
                    horaLlamado = FechaUtil.ahora(),
                    probadorLlamado = codigoProbador
                )
            )
            null
        }
    }

    // HU-03: el turno se presento y entra al probador.
    // Aclaracion 7: si dos personas asignan al mismo tiempo, vale la primera.
    suspend fun asignarProbador(idTurno: Long, codigoProbador: String, rut: String): String? {
        return db.withTransaction {
            val turno = turnoDao.buscar(idTurno) ?: return@withTransaction "El turno no existe"
            val probador = probadorDao.buscar(codigoProbador) ?: return@withTransaction "El probador no existe"

            if (probador.estado != EstadoProbador.LIBRE) {
                return@withTransaction "${probador.codigo} ya fue asignado por otro colaborador"
            }
            if (turno.estado != EstadoTurno.EN_ESPERA && turno.estado != EstadoTurno.LLAMADO) {
                return@withTransaction "El turno ${turno.codigoTurno()} ya fue cerrado"
            }
            val ahora = FechaUtil.ahora()
            turnoDao.actualizar(turno.copy(estado = EstadoTurno.ATENDIDO, horaCierre = ahora))
            usoDao.insertar(
                UsoEntity(
                    codigoProbador = codigoProbador,
                    idTurno = turno.id,
                    fecha = turno.fecha,
                    horaInicio = ahora,
                    prendasIngresadas = turno.prendas,
                    rutInicio = rut
                )
            )
            cambiarEstado(probador, EstadoProbador.OCUPADO, rut, null, ahora)
            null
        }
    }

    // HU-05 y RN-07: abandono o no se presento
    suspend fun cerrarTurnoSinAtencion(idTurno: Long, estadoFinal: EstadoTurno, motivo: MotivoAbandono): String? {
        return db.withTransaction {
            val turno = turnoDao.buscar(idTurno) ?: return@withTransaction "El turno no existe"
            // RN-03: un turno cerrado no se reabre
            if (turno.estado != EstadoTurno.EN_ESPERA && turno.estado != EstadoTurno.LLAMADO) {
                return@withTransaction "El turno ya estaba cerrado"
            }
            turnoDao.actualizar(
                turno.copy(estado = estadoFinal, horaCierre = FechaUtil.ahora(), motivoAbandono = motivo)
            )
            null
        }
    }

    // RN-12: al cierre los turnos en espera quedan como abandono por cierre de tienda
    suspend fun cerrarFilaPorCierre(fecha: String) {
        db.withTransaction {
            val turnos = turnoDao.obtenerDelDia(fecha)
            for (t in turnos) {
                if (t.estado == EstadoTurno.EN_ESPERA || t.estado == EstadoTurno.LLAMADO) {
                    turnoDao.actualizar(
                        t.copy(
                            estado = EstadoTurno.ABANDONO,
                            horaCierre = FechaUtil.ahora(),
                            motivoAbandono = MotivoAbandono.CIERRE_TIENDA
                        )
                    )
                }
            }
        }
    }

    // ---------- Probadores ----------

    // Aclaracion 1 y 3: cliente entra sin fila. Se puede registrar hasta 15 min hacia atras.
    suspend fun ingresoDirecto(codigo: String, prendas: Int, minutosAtras: Int, rut: String): String? {
        return db.withTransaction {
            val probador = probadorDao.buscar(codigo) ?: return@withTransaction "El probador no existe"
            if (probador.estado != EstadoProbador.LIBRE) {
                return@withTransaction "$codigo no está libre"
            }
            val inicio = FechaUtil.ahora() - minutosAtras * 60000L
            usoDao.insertar(
                UsoEntity(
                    codigoProbador = codigo,
                    idTurno = null,
                    fecha = FechaUtil.hoy(),
                    horaInicio = inicio,
                    prendasIngresadas = prendas,
                    conAtraso = minutosAtras > 0,
                    rutInicio = rut
                )
            )
            cambiarEstado(probador, EstadoProbador.OCUPADO, rut, null, inicio)
            null
        }
    }

    // HU-04: el cliente sale y devuelve prendas. El probador pasa a ordenamiento.
    suspend fun terminarUso(codigo: String, devueltas: Int, rut: String): String? {
        return db.withTransaction {
            val probador = probadorDao.buscar(codigo) ?: return@withTransaction "El probador no existe"
            val uso = usoDao.usoAbierto(codigo) ?: return@withTransaction "$codigo no tiene un uso abierto"
            if (devueltas > uso.prendasIngresadas) {
                return@withTransaction "No puede devolver más prendas de las que ingresó"
            }
            usoDao.actualizar(
                uso.copy(horaTermino = FechaUtil.ahora(), prendasDevueltas = devueltas, rutTermino = rut)
            )
            cambiarEstado(probador, EstadoProbador.ORDENAMIENTO, rut, null, FechaUtil.ahora())
            null
        }
    }

    // HU-04 CA-2: termina de ordenar y queda libre
    suspend fun terminarOrdenamiento(codigo: String, rut: String): String? {
        return db.withTransaction {
            val probador = probadorDao.buscar(codigo) ?: return@withTransaction "El probador no existe"
            if (probador.estado != EstadoProbador.ORDENAMIENTO) {
                return@withTransaction "$codigo no está en ordenamiento"
            }
            cambiarEstado(probador, EstadoProbador.LIBRE, rut, null, FechaUtil.ahora())
            null
        }
    }

    // HU-08: solo jefatura (se valida en el ViewModel). Puede llevar foto de la falla.
    suspend fun marcarFueraDeServicio(codigo: String, motivo: String, fotoRuta: String?, rut: String): String? {
        return db.withTransaction {
            val probador = probadorDao.buscar(codigo) ?: return@withTransaction "El probador no existe"
            if (probador.estado == EstadoProbador.FUERA_SERVICIO) {
                return@withTransaction "$codigo ya está fuera de servicio"
            }
            if (probador.estado == EstadoProbador.OCUPADO) {
                return@withTransaction "Primero registra la salida del cliente de $codigo"
            }
            val ahora = FechaUtil.ahora()
            cambioDao.insertar(
                CambioEstadoEntity(
                    codigoProbador = codigo,
                    estadoAnterior = probador.estado,
                    estadoNuevo = EstadoProbador.FUERA_SERVICIO,
                    fechaHora = ahora,
                    rut = rut,
                    motivo = motivo
                )
            )
            probadorDao.actualizar(
                probador.copy(
                    estado = EstadoProbador.FUERA_SERVICIO,
                    estadoDesde = ahora,
                    motivoFueraServicio = motivo,
                    fotoRuta = fotoRuta
                )
            )
            null
        }
    }

    // RN-01: desde fuera de servicio solo vuelve a libre
    suspend fun habilitar(codigo: String, rut: String): String? {
        return db.withTransaction {
            val probador = probadorDao.buscar(codigo) ?: return@withTransaction "El probador no existe"
            if (probador.estado != EstadoProbador.FUERA_SERVICIO) {
                return@withTransaction "$codigo ya está habilitado"
            }
            val ahora = FechaUtil.ahora()
            cambioDao.insertar(
                CambioEstadoEntity(
                    codigoProbador = codigo,
                    estadoAnterior = probador.estado,
                    estadoNuevo = EstadoProbador.LIBRE,
                    fechaHora = ahora,
                    rut = rut,
                    motivo = "Habilitado nuevamente"
                )
            )
            probadorDao.actualizar(
                probador.copy(
                    estado = EstadoProbador.LIBRE,
                    estadoDesde = ahora,
                    motivoFueraServicio = null,
                    fotoRuta = null
                )
            )
            null
        }
    }

    // Cambia el estado del probador y deja el registro del cambio (Anexo 8)
    private suspend fun cambiarEstado(
        probador: ProbadorEntity,
        nuevo: EstadoProbador,
        rut: String,
        motivo: String?,
        cuando: Long
    ) {
        cambioDao.insertar(
            CambioEstadoEntity(
                codigoProbador = probador.codigo,
                estadoAnterior = probador.estado,
                estadoNuevo = nuevo,
                fechaHora = cuando,
                rut = rut,
                motivo = motivo
            )
        )
        probadorDao.actualizar(probador.copy(estado = nuevo, estadoDesde = cuando))
    }

    // ---------- Parametros (RD-05) ----------

    suspend fun guardarParametros(parametros: ParametrosEntity) {
        parametrosDao.guardar(parametros.copy(id = 1))
    }
}
