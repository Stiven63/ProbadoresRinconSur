package com.rinconsur.probadores.nativo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Recurso nativo 2: camara.
// Se usa para sacar una foto de la falla cuando la jefatura marca un probador
// fuera de servicio (ej: cortina rota). Nunca se fotografia a clientes (RNF-06).
object Camara {

    // Crea un archivo vacio dentro de la carpeta privada de la app
    // y devuelve el Uri que la camara necesita para guardar la foto
    fun crearArchivoFoto(context: Context, codigoProbador: String): Pair<File, Uri> {
        val carpeta = File(context.filesDir, "fotos")
        if (!carpeta.exists()) carpeta.mkdirs()
        val archivo = File(carpeta, "probador_${codigoProbador}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            archivo
        )
        return Pair(archivo, uri)
    }
}
