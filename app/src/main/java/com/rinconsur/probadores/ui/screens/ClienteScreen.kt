package com.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.probadores.ui.theme.Ambar
import com.rinconsur.probadores.viewmodel.ClienteViewModel

// Pantalla para el punto de espera (RF-06, HU-01).
// Letra grande y alto contraste para leerse a 2 metros (RNF-05 y RNF-07).
// Fondo oscuro con letras blancas y amarillas.
private val FondoCliente = Color(0xFF0B1F33)

@Composable
fun ClienteScreen(alVolver: () -> Unit, viewModel: ClienteViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCliente)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text("Probadores · Rincón Sur", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // mensaje principal segun la situacion (HU-01 CA-1 y CA-2)
        val mensaje = when {
            estado.estimado < 0 -> "Por ahora no hay probadores disponibles"
            estado.estimado == 0 -> "¡Hay probadores libres! Acércate al mesón"
            else -> "Espera estimada: unos ${estado.estimado} minutos"
        }
        AnimatedContent(
            targetState = mensaje,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "mensajeCliente"
        ) { texto ->
            Text(
                texto,
                color = Ambar,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        DatoGrande(Icons.Filled.DoorFront, "Probadores libres", estado.libres.toString())
        DatoGrande(Icons.Filled.Groups, "Personas esperando", estado.enEspera.toString())
        DatoGrande(
            Icons.Filled.Schedule,
            "Tiempo estimado",
            if (estado.estimado < 0) "—" else "${estado.estimado} min"
        )

        // RD-01: el cliente ve cuando lo llaman (solo el numero de turno, es anonimo)
        AnimatedVisibility(visible = estado.llamados.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ambar),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.Black, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Llamando", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    for (l in estado.llamados) {
                        Text(l, color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Máximo 6 prendas por ingreso. El personal del mesón te asignará un probador.",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DatoGrande(icono: ImageVector, titulo: String, valor: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16324F)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = Color.White, fontSize = 22.sp)
            }
            // el numero cambia con animacion hacia arriba / abajo
            AnimatedContent(
                targetState = valor,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "valorCliente"
            ) { v ->
                Text(v, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
