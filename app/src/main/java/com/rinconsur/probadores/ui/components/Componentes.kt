package com.rinconsur.probadores.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.ui.theme.ColorFueraServicio
import com.rinconsur.probadores.ui.theme.ColorLibre
import com.rinconsur.probadores.ui.theme.ColorOcupado
import com.rinconsur.probadores.ui.theme.ColorOrdenamiento

// Campo de texto con validacion visual: icono de error y mensaje debajo (IE 2.1.2)
@Composable
fun CampoFormulario(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    esClave: Boolean = false,
    ayuda: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            label = { Text(etiqueta) },
            leadingIcon = { Icon(icono, contentDescription = null) },
            trailingIcon = {
                if (error != null) {
                    Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                } else if (valor.isNotEmpty()) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Correcto", tint = ColorLibre)
                }
            },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
            visualTransformation = if (esClave) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )
        // el mensaje de error aparece y desaparece con animacion
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (error == null && ayuda != null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

// Boton grande que se achica un poco al presionarlo y muestra un loader cuando esta cargando
@Composable
fun BotonPrincipal(
    texto: String,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    habilitado: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.95f else 1f,
        animationSpec = tween(120),
        label = "escalaBoton"
    )
    Button(
        onClick = alPresionar,
        enabled = habilitado && !cargando,
        interactionSource = interaccion,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(escala)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Color, icono y texto de cada estado (no depende solo del color)
fun colorEstado(estado: EstadoProbador): Color {
    return when (estado) {
        EstadoProbador.LIBRE -> ColorLibre
        EstadoProbador.OCUPADO -> ColorOcupado
        EstadoProbador.ORDENAMIENTO -> ColorOrdenamiento
        EstadoProbador.FUERA_SERVICIO -> ColorFueraServicio
    }
}

fun iconoEstado(estado: EstadoProbador): ImageVector {
    return when (estado) {
        EstadoProbador.LIBRE -> Icons.Filled.CheckCircle
        EstadoProbador.OCUPADO -> Icons.Filled.Person
        EstadoProbador.ORDENAMIENTO -> Icons.Filled.CleaningServices
        EstadoProbador.FUERA_SERVICIO -> Icons.Filled.Block
    }
}

// Tarjeta de un probador en el panel (HU-06)
@Composable
fun TarjetaProbador(
    codigo: String,
    estado: EstadoProbador,
    minutos: Long,
    accesible: Boolean,
    detalle: String?,
    prolongado: Boolean,
    ofrecerAyuda: Boolean,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // el color cambia con animacion cuando cambia el estado
    val colorFondo by animateColorAsState(
        targetValue = colorEstado(estado).copy(alpha = 0.12f),
        animationSpec = tween(500),
        label = "colorTarjeta"
    )
    val colorBorde = colorEstado(estado)

    // si es uso prolongado, el borde "late" para llamar la atencion (HU-07)
    val transicion = rememberInfiniteTransition(label = "latido")
    val grosor by transicion.animateFloat(
        initialValue = 2f,
        targetValue = if (prolongado) 6f else 2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "grosorBorde"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { alTocar() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = BorderStroke(grosor.dp, if (prolongado) ColorOcupado else colorBorde)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(codigo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (accesible) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Accessible,
                        contentDescription = "Accesible",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconoEstado(estado), contentDescription = null, tint = colorBorde, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(estado.texto, color = colorBorde, fontWeight = FontWeight.SemiBold)
            }
            Text("Hace $minutos min", style = MaterialTheme.typography.bodyMedium)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
            if (ofrecerAyuda) {
                Text("⚠ Ofrecer ayuda", color = ColorOcupado, fontWeight = FontWeight.Bold)
            } else if (prolongado) {
                Text("⚠ Uso prolongado", color = ColorOcupado, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Tarjeta simple con un indicador (resumen y pantalla cliente)
@Composable
fun TarjetaIndicador(titulo: String, valor: String, icono: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.bodyLarge)
                Text(valor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
