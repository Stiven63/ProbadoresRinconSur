package com.rinconsur.probadores.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta propia (Aclaracion 9: sin logo ni colores corporativos)
val AzulPetroleo = Color(0xFF1B4F72)
val AzulClaro = Color(0xFFD6EAF8)
val Ambar = Color(0xFFF5B041)
val FondoClaro = Color(0xFFF7F9FB)

// Colores de estado del probador. Siempre van junto a un icono y un texto
// para no depender solo del color (RNF-07)
val ColorLibre = Color(0xFF1E8449)
val ColorOcupado = Color(0xFFC0392B)
val ColorOrdenamiento = Color(0xFFB9770E)
val ColorFueraServicio = Color(0xFF5D6D7E)

private val Colores = lightColorScheme(
    primary = AzulPetroleo,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulPetroleo,
    secondary = Ambar,
    onSecondary = Color.Black,
    background = FondoClaro,
    surface = Color.White,
    error = Color(0xFFB00020)
)

private val Tipografia = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp)
)

@Composable
fun ProbadoresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colores,
        typography = Tipografia,
        content = content
    )
}
