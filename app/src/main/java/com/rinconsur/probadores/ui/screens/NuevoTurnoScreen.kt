package com.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.probadores.ui.components.BotonPrincipal
import com.rinconsur.probadores.ui.components.CampoFormulario
import com.rinconsur.probadores.ui.components.MensajeError
import com.rinconsur.probadores.ui.components.PantallaFormulario
import com.rinconsur.probadores.viewmodel.NuevoTurnoViewModel

// Formulario HU-02: registrar el ingreso de un cliente a la fila
@Composable
fun NuevoTurnoScreen(alVolver: () -> Unit, viewModel: NuevoTurnoViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaFormulario(titulo = "Nuevo turno", alVolver = alVolver) {

        if (estado.turnoCreado == null) {
            Text("Cuenta las prendas del cliente y regístralo en la fila.", style = MaterialTheme.typography.bodyLarge)

            // botones grandes de - y + porque el asistente tiene las manos ocupadas (RNF-03)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { viewModel.sumarPrenda(-1) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Quitar prenda")
                }
                CampoFormulario(
                    valor = estado.prendas,
                    alCambiar = viewModel::cambiarPrendas,
                    etiqueta = "Prendas (1 a ${estado.maxPrendas})",
                    icono = Icons.Filled.Checkroom,
                    error = estado.errorPrendas,
                    tipoTeclado = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { viewModel.sumarPrenda(1) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar prenda")
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Accessible, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Atención preferente", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Movilidad reducida, embarazada, adulto mayor o con coche de bebé. Tiene prioridad para P-08.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = estado.preferente, onCheckedChange = viewModel::cambiarPreferente)
                }
            }

            MensajeError(estado.errorGeneral)

            BotonPrincipal(texto = "Registrar en la fila", alPresionar = viewModel::guardar, cargando = estado.guardando)
        }

        // Confirmacion animada con el numero de turno y la espera estimada
        AnimatedVisibility(
            visible = estado.turnoCreado != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Turno", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp)
                    Text(
                        estado.turnoCreado ?: "",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val textoEspera = when {
                        estado.estimado < 0 -> "Sin probadores disponibles por ahora"
                        estado.estimado == 0 -> "Hay un probador libre, pasa altiro"
                        else -> "Espera estimada: ${estado.estimado} min"
                    }
                    Text(textoEspera, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp)
                }
            }
        }

        if (estado.turnoCreado != null) {
            BotonPrincipal(texto = "Registrar otro cliente", alPresionar = viewModel::nuevoRegistro)
            OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al panel")
            }
        }
    }
}
