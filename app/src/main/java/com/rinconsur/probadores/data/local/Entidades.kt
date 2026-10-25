package com.rinconsur.probadores.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.MotivoAbandono
import com.rinconsur.probadores.model.Perfil

// Tablas de la base de datos local (Room / SQLite).
// Las fechas se guardan como texto "yyyy-MM-dd" y las horas como milisegundos (Long).

@Entity(tableName = "colaborador")
data class ColaboradorEntity(
    @PrimaryKey val rut: String,          // ej: 16842311-K (sin puntos)
    val nombre: String,
    val perfil: Perfil,
    val turnoLaboral: String,             // Mañana, Tarde, Fin de semana
    val activo: Boolean = true,
    val claveHash: String                 // la clave se guarda con hash SHA-256, nunca en texto plano
)

@Entity(tableName = "probador")
data class ProbadorEntity(
    @PrimaryKey val codigo: String,       // P-01 ... P-08
    val accesible: Boolean,
    val ubicacion: String,
    val estado: EstadoProbador,
    val estadoDesde: Long,
    val motivoFueraServicio: String? = null,
    val fotoRuta: String? = null          // foto tomada con la camara cuando se marca fuera de servicio
) {
    fun habilitado(): Boolean = estado != EstadoProbador.FUERA_SERVICIO
}

@Entity(tableName = "turno")
data class TurnoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String,
    val numero: Int,
    val horaIngreso: Long,
    val preferente: Boolean,
    val prendas: Int,
    val estado: EstadoTurno,
    val horaLlamado: Long? = null,
    val probadorLlamado: String? = null,  // a que probador se llamo
    val horaCierre: Long? = null,
    val motivoAbandono: MotivoAbandono? = null,
    val rutRegistro: String
) {
    // T-001, T-002 ...
    fun codigoTurno(): String = "T-" + numero.toString().padStart(3, '0')
}

@Entity(tableName = "uso")
data class UsoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigoProbador: String,
    val idTurno: Long? = null,            // null si entro sin hacer fila
    val fecha: String,
    val horaInicio: Long,
    val horaTermino: Long? = null,
    val prendasIngresadas: Int,
    val prendasDevueltas: Int? = null,
    val conAtraso: Boolean = false,       // Aclaracion 3
    val rutInicio: String,
    val rutTermino: String? = null
) {
    fun cerrado(): Boolean = horaTermino != null && prendasDevueltas != null
}

@Entity(tableName = "cambio_estado")
data class CambioEstadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigoProbador: String,
    val estadoAnterior: EstadoProbador?,
    val estadoNuevo: EstadoProbador,
    val fechaHora: Long,
    val rut: String,
    val motivo: String? = null
)

// Una sola fila (id = 1) con los parametros de la seccion (Anexo 9.6)
@Entity(tableName = "parametros")
data class ParametrosEntity(
    @PrimaryKey val id: Int = 1,
    val maxPrendas: Int = 6,
    val tiempoRefUso: Int = 8,
    val minOrdenamiento: Int = 1,
    val umbralProlongado: Int = 15,
    val umbralAyuda: Int = 25,
    val minPresentarse: Int = 2,
    val ticketPromedio: Int = 27990,
    val horaUltimoIngreso: String = "20:45",
    val horaCierre: String = "21:00"
)
