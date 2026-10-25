package com.rinconsur.probadores.nativo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rinconsur.probadores.R

// Recurso nativo 1: notificaciones del sistema (con vibracion)
// Se usan para avisar al personal (Aclaracion 8 y C-09):
// - uso prolongado (RN-08)
// - probador libre con gente esperando por mas de 1 minuto
// - turno llamado que no se presento
object Notificaciones {

    const val CANAL_ID = "avisos_probadores"

    // Android 8+ necesita un canal para mostrar notificaciones
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID,
            "Avisos de probadores",
            NotificationManager.IMPORTANCE_HIGH
        )
        canal.description = "Usos prolongados, probadores libres y turnos que no se presentan"
        canal.enableVibration(true)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
    }

    // En Android 13+ hay que pedir el permiso POST_NOTIFICATIONS
    fun tienePermiso(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun mostrar(context: Context, id: Int, titulo: String, texto: String) {
        // si el usuario no dio permiso, no se muestra (la app igual lo marca en pantalla)
        if (!tienePermiso(context)) return

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (e: SecurityException) {
            // el permiso se quito mientras la app estaba abierta
        }
    }
}
