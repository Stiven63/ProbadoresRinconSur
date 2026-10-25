package com.rinconsur.probadores.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.nativo.Camara
import com.rinconsur.probadores.ui.components.BotonPrincipal
import com.rinconsur.probadores.ui.components.CampoFormulario
import com.rinconsur.probadores.ui.components.MensajeError
import com.rinconsur.probadores.ui.components.PantallaFormulario
import com.rinconsur.probadores.ui.theme.ColorLibre
import com.rinconsur.probadores.utils.FechaUtil
import com.rinconsur.probadores.viewmodel.ProbadorFormViewModel
import java.io.File

// ===================== HU-04: registrar salida del cliente =====================
@Composable
fun TerminarUsoScreen(codigo: String, alVolver: () -> Unit, viewModel: ProbadorFormViewModel = viewModel()) {
    LaunchedEffect(codigo) { viewModel.cargar(codigo) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.listo) { if (estado.listo) alVolver() }

    PantallaFormulario(titulo = "Salida de $codigo", alVolver = alVolver) {
        val uso = estado.usoAbierto
        if (uso == null) {
            Text("Este probador no tiene un uso abierto.")
            return@PantallaFormulario
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Entró a las ${FechaUtil.horaTexto(uso.horaInicio)}", fontWeight = FontWeight.SemiBold)
                Text("Lleva ${FechaUtil.minutosEntre(uso.horaInicio, FechaUtil.ahora())} minutos")
                Text("Prendas ingresadas: ${uso.prendasIngresadas}")
            }
        }
        CampoFormulario(
            valor = estado.devueltas,
            alCambiar = viewModel::cambiarDevueltas,
            etiqueta = "Prendas que devuelve en el mesón",
            icono = Icons.Filled.AssignmentReturn,
            error = estado.errorDevueltas,
            tipoTeclado = KeyboardType.Number,
            ayuda = "Entre 0 y ${uso.prendasIngresadas}. Si devuelve menos, se asume que compró."
        )
        MensajeError(estado.errorGeneral)
        BotonPrincipal(texto = "Registrar salida", alPresionar = viewModel::terminarUso, cargando = estado.guardando)
        Text("El probador quedará \"En ordenamiento\" hasta que lo dejes listo.", style = MaterialTheme.typography.bodyMedium)
    }
}

// ===================== Aclaracion 1 y 3: cliente entra sin fila =====================
@Composable
fun IngresoDirectoScreen(codigo: String, alVolver: () -> Unit, viewModel: ProbadorFormViewModel = viewModel()) {
    LaunchedEffect(codigo) { viewModel.cargar(codigo) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.listo) { if (estado.listo) alVolver() }

    PantallaFormulario(titulo = "Ingreso directo a $codigo", alVolver = alVolver) {
        Text("Usa esto cuando no hay fila y el cliente entra directo a un probador libre.")
        CampoFormulario(
            valor = estado.prendas,
            alCambiar = viewModel::cambiarPrendas,
            etiqueta = "Prendas (1 a ${estado.maxPrendas})",
            icono = Icons.Filled.Checkroom,
            error = estado.errorPrendas,
            tipoTeclado = KeyboardType.Number
        )
        CampoFormulario(
            valor = estado.minutosAtras,
            alCambiar = viewModel::cambiarMinutos,
            etiqueta = "¿Hace cuántos minutos entró?",
            icono = Icons.Filled.History,
            error = estado.errorMinutos,
            tipoTeclado = KeyboardType.Number,
            ayuda = "0 si entró recién. Máximo 15 (queda marcado \"con atraso\")."
        )
        MensajeError(estado.errorGeneral)
        BotonPrincipal(texto = "Registrar ingreso", alPresionar = viewModel::ingresoDirecto, cargando = estado.guardando)
    }
}

// ===================== HU-08: fuera de servicio con foto (camara) =====================
@Composable
fun FueraServicioScreen(codigo: String, alVolver: () -> Unit, viewModel: ProbadorFormViewModel = viewModel()) {
    LaunchedEffect(codigo) { viewModel.cargar(codigo) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(estado.listo) { if (estado.listo) alVolver() }

    // ruta del archivo donde la camara va a guardar la foto
    var rutaTemporal by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCamara by rememberSaveable { mutableStateOf<String?>(null) }

    // 1) abre la app de camara y guarda la foto en el archivo
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) {
            viewModel.fotoTomada(rutaTemporal)
        } else {
            rutaTemporal?.let { File(it).delete() }
        }
    }

    fun abrirCamara() {
        val (archivo, uri) = Camara.crearArchivoFoto(context, codigo)
        rutaTemporal = archivo.absolutePath
        tomarFoto.launch(uri)
    }

    // 2) pide el permiso de camara si no lo tiene
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            errorCamara = null
            abrirCamara()
        } else {
            errorCamara = "Sin permiso de cámara no se puede adjuntar la foto. Puedes guardar igual sin foto."
        }
    }

    val probador = estado.probador

    PantallaFormulario(titulo = "Fuera de servicio · $codigo", alVolver = alVolver) {
        if (probador == null) {
            Text("Cargando...")
            return@PantallaFormulario
        }

        if (probador.estado == EstadoProbador.FUERA_SERVICIO) {
            // ---- ya esta fuera de servicio: ver detalle y habilitar ----
            Text("Motivo: ${probador.motivoFueraServicio ?: "—"}", style = MaterialTheme.typography.titleMedium)
            Text("Desde las ${FechaUtil.horaTexto(probador.estadoDesde)}")
            if (probador.fotoRuta != null) {
                FotoProbador(probador.fotoRuta)
            } else {
                Text("Sin foto registrada", style = MaterialTheme.typography.bodyMedium)
            }
            MensajeError(estado.errorGeneral)
            BotonPrincipal(
                texto = "Habilitar probador",
                alPresionar = viewModel::habilitar,
                cargando = estado.guardando,
                color = ColorLibre,
                habilitado = Sesion.esJefatura()
            )
        } else {
            // ---- formulario para dejarlo fuera de servicio ----
            Text("El probador no se podrá asignar y el tiempo estimado se calculará sin él.")
            CampoFormulario(
                valor = estado.motivo,
                alCambiar = viewModel::cambiarMotivo,
                etiqueta = "Motivo (ej: cortina rota)",
                icono = Icons.Filled.Description,
                error = estado.errorMotivo
            )

            OutlinedButton(
                onClick = {
                    val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                    if (tienePermiso) abrirCamara() else pedirPermiso.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (estado.fotoRuta == null) "Tomar foto de la falla" else "Tomar otra foto")
            }
            MensajeError(errorCamara)

            // la foto aparece con animacion cuando se toma
            AnimatedVisibility(visible = estado.fotoRuta != null, enter = fadeIn() + scaleIn()) {
                estado.fotoRuta?.let { FotoProbador(it) }
            }

            MensajeError(estado.errorGeneral)
            BotonPrincipal(
                texto = "Marcar fuera de servicio",
                alPresionar = viewModel::marcarFueraDeServicio,
                cargando = estado.guardando,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Muestra la foto con bordes redondeados y tamaño fijo, recortada para que no se deforme
@Composable
private fun FotoProbador(ruta: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        AsyncImage(
            model = File(ruta),
            contentDescription = "Foto de la falla",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(16.dp))
        )
    }
}
