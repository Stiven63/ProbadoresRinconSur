package com.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.probadores.model.Perfil
import com.rinconsur.probadores.ui.components.BotonPrincipal
import com.rinconsur.probadores.ui.components.CampoFormulario
import com.rinconsur.probadores.ui.components.MensajeError
import com.rinconsur.probadores.ui.components.PantallaFormulario
import com.rinconsur.probadores.ui.components.TarjetaIndicador
import com.rinconsur.probadores.ui.theme.ColorLibre
import com.rinconsur.probadores.utils.Calculos
import com.rinconsur.probadores.viewmodel.ColaboradoresViewModel
import com.rinconsur.probadores.viewmodel.ParametrosViewModel
import com.rinconsur.probadores.viewmodel.ResumenViewModel

// ===================== HU-09: resumen del dia y por franja =====================
@Composable
fun ResumenScreen(alVolver: () -> Unit, viewModel: ResumenViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaFormulario(titulo = "Resumen del día", alVolver = alVolver) {
        CampoFormulario(
            valor = estado.fecha,
            alCambiar = viewModel::cambiarFecha,
            etiqueta = "Fecha (dd-mm-aaaa)",
            icono = Icons.Filled.CalendarMonth,
            error = estado.errorFecha,
            ayuda = "Prueba con 26-09-2026 (registro manual del Anexo A)"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoFormulario(
                valor = estado.desde,
                alCambiar = viewModel::cambiarDesde,
                etiqueta = "Desde (hora)",
                icono = Icons.Filled.Schedule,
                error = null,
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CampoFormulario(
                valor = estado.hasta,
                alCambiar = viewModel::cambiarHasta,
                etiqueta = "Hasta (hora)",
                icono = Icons.Filled.Schedule,
                error = null,
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }
        // el error de la franja se muestra una sola vez para los dos campos
        MensajeError(estado.errorFranja)
        Text("Deja las horas vacías para ver el día completo", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        BotonPrincipal(texto = "Consultar", alPresionar = viewModel::consultar, cargando = estado.cargando)

        val r = estado.resumen
        AnimatedVisibility(visible = r != null, enter = fadeIn() + expandVertically()) {
            if (r != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (r.ingresos == 0) {
                        Text("No hay registros para esa fecha o franja.")
                    }
                    TarjetaIndicador("Ingresos a la fila", r.ingresos.toString(), Icons.Filled.Groups)
                    TarjetaIndicador(
                        "Abandonos",
                        "${r.abandonos} (${String.format("%.1f", r.porcentajeAbandono)}%)",
                        Icons.Filled.DirectionsWalk
                    )
                    TarjetaIndicador("Espera promedio", String.format("%.1f min", r.esperaPromedio), Icons.Filled.Timer)
                    TarjetaIndicador("Uso promedio del probador", String.format("%.1f min", r.usoPromedio), Icons.Filled.AccessTime)
                    TarjetaIndicador("Conversión", String.format("%.1f%%", r.conversion), Icons.Filled.ShoppingBag)
                    TarjetaIndicador("Venta potencial perdida", Calculos.formatoPesos(r.ventaPerdida), Icons.Filled.MoneyOff)
                    Text(
                        "Venta perdida = ${r.abandonosVentaPerdida} abandonos × ${String.format("%.1f", r.conversion)}% conversión × " +
                                "${Calculos.formatoPesos(estado.ticket.toLong())} ticket promedio (RN-09). " +
                                "\"Decidió comprar sin probarse\" y \"Cierre de tienda\" no cuentan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ===================== RD-05: parametros de la seccion =====================
@Composable
fun ParametrosScreen(alVolver: () -> Unit, viewModel: ParametrosViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaFormulario(titulo = "Parámetros", alVolver = alVolver) {
        CampoFormulario(estado.maxPrendas, { viewModel.cambiar("maxPrendas", it) }, "Máximo de prendas por ingreso",
            Icons.Filled.Checkroom, estado.errores["maxPrendas"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.tiempoRef, { viewModel.cambiar("tiempoRef", it) }, "Tiempo de referencia de uso (min)",
            Icons.Filled.Timer, estado.errores["tiempoRef"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.ordenamiento, { viewModel.cambiar("ordenamiento", it) }, "Minutos de ordenamiento",
            Icons.Filled.AccessTime, estado.errores["ordenamiento"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.umbralProlongado, { viewModel.cambiar("umbralProlongado", it) }, "Umbral uso prolongado (min)",
            Icons.Filled.Warning, estado.errores["umbralProlongado"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.umbralAyuda, { viewModel.cambiar("umbralAyuda", it) }, "Umbral para ofrecer ayuda (min)",
            Icons.Filled.Warning, estado.errores["umbralAyuda"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.ticket, { viewModel.cambiar("ticket", it) }, "Ticket promedio ($)",
            Icons.Filled.AttachMoney, estado.errores["ticket"], tipoTeclado = KeyboardType.Number)
        CampoFormulario(estado.ultimoIngreso, { viewModel.cambiar("ultimoIngreso", it) }, "Último ingreso a la fila (HH:mm)",
            Icons.Filled.Schedule, estado.errores["ultimoIngreso"])

        BotonPrincipal(texto = "Guardar parámetros", alPresionar = viewModel::guardar)
        AnimatedVisibility(visible = estado.guardado) {
            Text("✔ Parámetros guardados", color = ColorLibre, fontWeight = FontWeight.Bold)
        }
    }
}

// ===================== RF-10 / RN-13: colaboradores =====================
@Composable
fun ColaboradoresScreen(alVolver: () -> Unit, viewModel: ColaboradoresViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val lista by viewModel.lista.collectAsStateWithLifecycle()

    PantallaFormulario(titulo = "Colaboradores", alVolver = alVolver) {
        Text("Agregar colaborador", style = MaterialTheme.typography.titleLarge)
        CampoFormulario(estado.rut, viewModel::cambiarRut, "RUT", Icons.Filled.Badge, estado.errorRut, ayuda = "Con dígito verificador")
        CampoFormulario(estado.nombre, viewModel::cambiarNombre, "Nombre completo", Icons.Filled.Person, estado.errorNombre)
        CampoFormulario(
            estado.clave, viewModel::cambiarClave, "Clave inicial", Icons.Filled.Lock, estado.errorClave,
            tipoTeclado = KeyboardType.Password, esClave = true
        )

        Text("Perfil")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (p in Perfil.values()) {
                FilterChip(
                    selected = estado.perfil == p,
                    onClick = { viewModel.cambiarPerfil(p) },
                    label = { Text(if (p == Perfil.ASISTENTE) "Asistente" else p.texto) }
                )
            }
        }
        Text("Turno habitual")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (t in listOf("Mañana", "Tarde", "Fin de semana")) {
                FilterChip(selected = estado.turno == t, onClick = { viewModel.cambiarTurno(t) }, label = { Text(t) })
            }
        }

        if (estado.mensaje != null) {
            Text(estado.mensaje ?: "", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        BotonPrincipal(texto = "Agregar", alPresionar = viewModel::guardar)

        HorizontalDivider()
        Text("Equipo de la sección", style = MaterialTheme.typography.titleLarge)
        for (c in lista) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.nombre, fontWeight = FontWeight.SemiBold)
                        Text("${c.rut} · ${c.perfil.texto} · ${c.turnoLaboral}", style = MaterialTheme.typography.bodyMedium)
                        Text(if (c.activo) "Activo" else "Inactivo", color = if (c.activo) ColorLibre else Color.Gray)
                    }
                    // RN-13: un inactivo no puede ingresar ni registrar
                    Switch(checked = c.activo, onCheckedChange = { viewModel.cambiarActivo(c) })
                }
            }
        }
    }
}
