package com.rinconsur.probadores.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rinconsur.probadores.utils.FechaUtil
import com.rinconsur.probadores.utils.Seguridad

@Database(
    entities = [
        ColaboradorEntity::class,
        ProbadorEntity::class,
        TurnoEntity::class,
        UsoEntity::class,
        CambioEstadoEntity::class,
        ParametrosEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun colaboradorDao(): ColaboradorDao
    abstract fun probadorDao(): ProbadorDao
    abstract fun turnoDao(): TurnoDao
    abstract fun usoDao(): UsoDao
    abstract fun cambioEstadoDao(): CambioEstadoDao
    abstract fun parametrosDao(): ParametrosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton: una sola instancia de la base de datos en toda la app
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "probadores_rincon_sur.db"
                )
                    .addCallback(DatosIniciales())
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }

    // Se ejecuta solo la primera vez que se crea la base de datos.
    // Carga los datos ficticios del Anexo 9 y el registro manual del Anexo A.
    private class DatosIniciales : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val ahora = FechaUtil.ahora()
            val clave = Seguridad.hashClave("1234")

            // Colaboradores (Anexo 9.2) - clave de prueba para todos: 1234
            val colaboradores = listOf(
                arrayOf("16842311-K", "Camila Andrea Fuentes Rojas", "ASISTENTE", "Fin de semana", "1"),
                arrayOf("19234587-1", "Matías Ignacio Soto Pizarro", "ASISTENTE", "Tarde", "1"),
                arrayOf("17550982-8", "Javiera Paz Muñoz Contreras", "VENDEDOR", "Mañana", "1"),
                arrayOf("20118436-3", "Benjamín Alonso Díaz Vera", "ASISTENTE", "Fin de semana", "1"),
                arrayOf("15763204-3", "Daniela Francisca Reyes Araya", "VENDEDOR", "Tarde", "1"),
                arrayOf("12984561-9", "Patricia Elena Morales Castillo", "JEFATURA", "Mañana", "1"),
                arrayOf("18447093-4", "Felipe Andrés Torres Godoy", "ASISTENTE", "Tarde", "0")
            )
            for (c in colaboradores) {
                db.execSQL(
                    "INSERT INTO colaborador (rut, nombre, perfil, turnoLaboral, activo, claveHash) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(c[0], c[1], c[2], c[3], c[4].toInt(), clave)
                )
            }

            // Probadores P-01 a P-08 (Aclaracion 2)
            for (i in 1..8) {
                val codigo = "P-0$i"
                val ubicacion = when {
                    i <= 4 -> "Lado mesón"
                    i <= 7 -> "Lado espejo"
                    else -> "Fondo (accesible)"
                }
                val accesible = if (i == 8) 1 else 0
                val estado = if (i == 6) "FUERA_SERVICIO" else "LIBRE"
                val motivo: String? = if (i == 6) "Cortina rota" else null
                db.execSQL(
                    "INSERT INTO probador (codigo, accesible, ubicacion, estado, estadoDesde, motivoFueraServicio, fotoRuta) VALUES (?, ?, ?, ?, ?, ?, NULL)",
                    arrayOf<Any?>(codigo, accesible, ubicacion, estado, ahora, motivo)
                )
            }
            db.execSQL(
                "INSERT INTO cambio_estado (codigoProbador, estadoAnterior, estadoNuevo, fechaHora, rut, motivo) VALUES ('P-06', 'LIBRE', 'FUERA_SERVICIO', ?, '12984561-9', 'Cortina rota')",
                arrayOf<Any?>(ahora)
            )

            // Parametros de la seccion (Anexo 9.6)
            db.execSQL(
                "INSERT INTO parametros (id, maxPrendas, tiempoRefUso, minOrdenamiento, umbralProlongado, umbralAyuda, minPresentarse, ticketPromedio, horaUltimoIngreso, horaCierre) " +
                        "VALUES (1, 6, 8, 1, 15, 25, 2, 27990, '20:45', '21:00')"
            )

            cargarRegistroManual(db)
        }

        // Anexo A: registro a mano del sabado 26-09-2026 entre 15:00 y 16:00.
        // Sirve para probar el resumen del dia (deberia dar 25% de abandono).
        private fun cargarRegistroManual(db: SupportSQLiteDatabase) {
            val fecha = "2026-09-26"
            val rut = "16842311-K"
            // numero, llego, prendas, preferente, probador, entro, salio, devolvio, horaSeFue
            val filas = listOf(
                arrayOf("1", "15:02", "3", "0", "P-02", "15:02", "15:11", "1", ""),
                arrayOf("2", "15:04", "5", "0", "P-05", "15:06", "15:19", "5", ""),
                arrayOf("3", "15:05", "2", "0", "", "", "", "", "15:06"),
                arrayOf("4", "15:07", "6", "0", "P-01", "15:12", "15:30", "4", ""),
                arrayOf("5", "15:09", "1", "0", "P-03", "15:13", "15:17", "0", ""),
                arrayOf("6", "15:10", "4", "0", "", "", "", "", "15:18"),
                arrayOf("7", "15:14", "3", "0", "P-07", "15:18", "15:26", "3", ""),
                arrayOf("8", "15:15", "2", "1", "P-08", "15:16", "15:21", "0", ""),
                arrayOf("9", "15:20", "6", "0", "", "", "", "", "15:21"),
                arrayOf("10", "15:22", "4", "0", "P-02", "15:23", "15:35", "2", ""),
                arrayOf("11", "15:25", "3", "0", "P-05", "15:27", "15:33", "3", ""),
                arrayOf("12", "15:31", "5", "0", "P-03", "15:33", "15:47", "1", "")
            )
            for (f in filas) {
                val numero = f[0].toLong()
                val ingreso = FechaUtil.millis(fecha, f[1])
                if (f[4].isEmpty()) {
                    // abandono
                    db.execSQL(
                        "INSERT INTO turno (id, fecha, numero, horaIngreso, preferente, prendas, estado, horaLlamado, probadorLlamado, horaCierre, motivoAbandono, rutRegistro) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 'ABANDONO', NULL, NULL, ?, 'ESPERA_LARGA', ?)",
                        arrayOf<Any?>(numero, fecha, numero, ingreso, f[3].toInt(), f[2].toInt(), FechaUtil.millis(fecha, f[8]), rut)
                    )
                } else {
                    val entro = FechaUtil.millis(fecha, f[5])
                    db.execSQL(
                        "INSERT INTO turno (id, fecha, numero, horaIngreso, preferente, prendas, estado, horaLlamado, probadorLlamado, horaCierre, motivoAbandono, rutRegistro) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 'ATENDIDO', ?, ?, ?, NULL, ?)",
                        arrayOf<Any?>(numero, fecha, numero, ingreso, f[3].toInt(), f[2].toInt(), entro, f[4], entro, rut)
                    )
                    db.execSQL(
                        "INSERT INTO uso (codigoProbador, idTurno, fecha, horaInicio, horaTermino, prendasIngresadas, prendasDevueltas, conAtraso, rutInicio, rutTermino) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
                        arrayOf<Any?>(f[4], numero, fecha, entro, FechaUtil.millis(fecha, f[6]), f[2].toInt(), f[7].toInt(), rut, rut)
                    )
                }
            }
        }
    }
}
