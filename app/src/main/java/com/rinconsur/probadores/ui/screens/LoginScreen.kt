package com.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.probadores.ui.components.BotonPrincipal
import com.rinconsur.probadores.ui.components.CampoFormulario
import com.rinconsur.probadores.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    alIngresar: () -> Unit,
    alVerPantallaCliente: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // cuando el login sale bien se navega al panel
    LaunchedEffect(estado.ingresoOk) {
        if (estado.ingresoOk) {
            viewModel.ingresoProcesado()
            alIngresar()
        }
    }

    // animacion de entrada del formulario
    var mostrar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mostrar = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Filled.Checkroom,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Text("Rincón Sur", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Probadores · Sección Vestuario", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            visible = mostrar,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ingreso colaboradores", style = MaterialTheme.typography.titleLarge)

                    CampoFormulario(
                        valor = estado.rut,
                        alCambiar = viewModel::cambiarRut,
                        etiqueta = "RUT",
                        icono = Icons.Filled.Badge,
                        error = estado.errorRut,
                        ayuda = "Ej: 16.842.311-K"
                    )
                    CampoFormulario(
                        valor = estado.clave,
                        alCambiar = viewModel::cambiarClave,
                        etiqueta = "Clave",
                        icono = Icons.Filled.Lock,
                        error = estado.errorClave,
                        tipoTeclado = KeyboardType.Password,
                        esClave = true
                    )

                    AnimatedVisibility(visible = estado.errorGeneral != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text(estado.errorGeneral ?: "", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    BotonPrincipal(texto = "Ingresar", alPresionar = viewModel::ingresar, cargando = estado.cargando)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        // El cliente no inicia sesion (Aclaracion 6): solo ve la pantalla de espera
        OutlinedButton(onClick = alVerPantallaCliente, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Tv, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ver pantalla para clientes")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Datos de prueba: RUT 12.984.561-9 (jefatura) o 16.842.311-K (asistente), clave 1234",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
