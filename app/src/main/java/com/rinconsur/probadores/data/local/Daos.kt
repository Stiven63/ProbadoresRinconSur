package com.rinconsur.probadores.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ColaboradorDao {
    @Query("SELECT * FROM colaborador ORDER BY nombre")
    fun obtenerTodos(): Flow<List<ColaboradorEntity>>

    @Query("SELECT * FROM colaborador WHERE rut = :rut LIMIT 1")
    suspend fun buscarPorRut(rut: String): ColaboradorEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(colaborador: ColaboradorEntity)

    @Update
    suspend fun actualizar(colaborador: ColaboradorEntity)
}

@Dao
interface ProbadorDao {
    @Query("SELECT * FROM probador ORDER BY codigo")
    fun obtenerTodos(): Flow<List<ProbadorEntity>>

    @Query("SELECT * FROM probador WHERE codigo = :codigo LIMIT 1")
    suspend fun buscar(codigo: String): ProbadorEntity?

    @Query("SELECT * FROM probador WHERE codigo = :codigo LIMIT 1")
    fun observar(codigo: String): Flow<ProbadorEntity?>

    @Update
    suspend fun actualizar(probador: ProbadorEntity)
}

@Dao
interface TurnoDao {
    // turnos que todavia estan en la fila (en espera o llamados)
    @Query("SELECT * FROM turno WHERE fecha = :fecha AND estado IN ('EN_ESPERA', 'LLAMADO') ORDER BY numero")
    fun obtenerActivos(fecha: String): Flow<List<TurnoEntity>>

    @Query("SELECT * FROM turno WHERE fecha = :fecha ORDER BY numero")
    suspend fun obtenerDelDia(fecha: String): List<TurnoEntity>

    @Query("SELECT MAX(numero) FROM turno WHERE fecha = :fecha")
    suspend fun ultimoNumero(fecha: String): Int?

    @Query("SELECT * FROM turno WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Long): TurnoEntity?

    @Insert
    suspend fun insertar(turno: TurnoEntity): Long

    @Update
    suspend fun actualizar(turno: TurnoEntity)
}

@Dao
interface UsoDao {
    @Query("SELECT * FROM uso WHERE fecha = :fecha ORDER BY horaInicio")
    fun observarDelDia(fecha: String): Flow<List<UsoEntity>>

    @Query("SELECT * FROM uso WHERE fecha = :fecha ORDER BY horaInicio")
    suspend fun obtenerDelDia(fecha: String): List<UsoEntity>

    // uso que sigue abierto en un probador (RN-01: solo uno a la vez)
    @Query("SELECT * FROM uso WHERE codigoProbador = :codigo AND horaTermino IS NULL LIMIT 1")
    suspend fun usoAbierto(codigo: String): UsoEntity?

    @Query("SELECT * FROM uso WHERE codigoProbador = :codigo AND horaTermino IS NULL LIMIT 1")
    fun observarUsoAbierto(codigo: String): Flow<UsoEntity?>

    @Insert
    suspend fun insertar(uso: UsoEntity): Long

    @Update
    suspend fun actualizar(uso: UsoEntity)
}

@Dao
interface CambioEstadoDao {
    @Insert
    suspend fun insertar(cambio: CambioEstadoEntity)

    @Query("SELECT * FROM cambio_estado WHERE codigoProbador = :codigo ORDER BY fechaHora DESC")
    fun historial(codigo: String): Flow<List<CambioEstadoEntity>>
}

@Dao
interface ParametrosDao {
    @Query("SELECT * FROM parametros WHERE id = 1")
    fun observar(): Flow<ParametrosEntity?>

    @Query("SELECT * FROM parametros WHERE id = 1")
    suspend fun obtener(): ParametrosEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(parametros: ParametrosEntity)
}
