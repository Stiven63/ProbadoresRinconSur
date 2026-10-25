package com.rinconsur.probadores.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.probadores.data.local.TurnoEntity
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.MotivoAbandono
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.nativo.Notificaciones
import com.rinconsur.probadores.ui.components.TarjetaProbador
import com.rinconsur.probadores.ui.theme.ColorLibre
import com.rinconsur.probadores.ui.theme.ColorOcupado
import com.rinconsur.probadores.viewmodel.PanelViewModel
import com.rinconsur.probadores.viewmodel.ProbadorUi
import com.rinconsur.probadores.viewmodel.SeccionUiState
import com.rinconsur.probadores.viewmodel.TurnoUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelScreen(
    alNuevoTurno: () -> Unit,
    alTerminarUso: (String) -> Unit,
    alIngresoDirecto: (String) -> Unit,
    alFueraServicio: (String) -> Unit,
    alPantallaCliente: () -> Unit,
    alResumen: () -> Unit,
    alParametros: () -> Unit,
    alColaboradores: () -> Unit,
    alCerrarSesion: () -> Unit,
    viewModel: PanelViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var pestania by remember { mutableIntStateOf(0) }
    var menuAbierto by remember { mutableStateOf(false) }
    var probadorElegido by remember { mutableStateOf<ProbadorUi?>(null) }
    var turnoAbandono by remember { mutableStateOf<TurnoEntity?>(null) }

    // Permiso de notificaciones (Android 13 o superior)
    // si no lo da, la app sigue funcionando y los avisos se ven igual en las tarjetas
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificaciones.tienePermiso(context)) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje!!)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Probadores", fontWeight = FontWeight.Bold)
                        Text(
                            "${Sesion.usuario?.nombre ?: ""} · ${Sesion.usuario?.perfil?.texto ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = alPantallaCliente) {
                        Icon(Icons.Filled.Tv, contentDescription = "Pantalla clientes")
                    }
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        // RN-11: opciones solo para la jefatura
                        if (Sesion.esJefatura()) {
                            DropdownMenuItem(
                                text = { Text("Resumen del día") },
                                leadingIcon = { Icon(Icons.Filled.BarChart, null) },
                                onClick = { menuAbierto = false; alResumen() }
                            )
                            DropdownMenuItem(
                                text = { Text("Parámetros") },
                                leadingIcon = { Icon(Icons.Filled.Settings, null) },
                                onClick = { menuAbierto = false; alParametros() }
                            )
                            DropdownMenuItem(
                                text = { Text("Colaboradores") },
                                leadingIcon = { Icon(Icons.Filled.People, null) },
                                onClick = { menuAbierto = false; alColaboradores() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                            onClick = { menuAbierto = false; alCerrarSesion() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alNuevoTurno,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo turno") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Resumen(estado)

            TabRow(selectedTabIndex = pestania) {
                Tab(selected = pestania == 0, onClick = { pestania = 0 }, text = { Text("Probadores") })
                Tab(selected = pestania == 1, onClick = { pestania = 1 }, text = { Text("Fila (${estado.fila.size})") })
            }

            // cambio de pestaña con animacion
            AnimatedContent(targetState = pestania, label = "pestanias") { p ->
                if (p == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(estado.probadores, key = { it.probador.codigo }) { p ->
                            TarjetaProbador(
                                codigo = p.probador.codigo,
                                estado = p.probador.estado,
                                minutos = p.minutos,
                                accesible = p.probador.accesible,
                                detalle = detalleProbador(p),
                                prolongado = p.prolongado,
                                ofrecerAyuda = p.ofrecerAyuda,
                                alTocar = { probadorElegido = p }
                            )
                        }
                    }
                } else {
                    ListaFila(
                        fila = estado.fila,
                        alPresentarse = { viewModel.sePresento(it) },
                        alNoPresentarse = { viewModel.noSePresento(it) },
                        alAbandonar = { turnoAbandono = it }
                    )
                }
            }
        }
    }

    // Dialogo con las acciones del probador tocado
    probadorElegido?.let { elegido ->
        // se busca la version actualizada por si cambio mientras estaba abierto
        val p = estado.probadores.find { it.probador.codigo == elegido.probador.codigo } ?: elegido
        AccionesProbadorDialog(
            p = p,
            hayFila = estado.enEspera > 0,
            alCerrar = { probadorElegido = null },
            alLlamar = { viewModel.llamarSiguiente(p.probador.codigo); probadorElegido = null },
            alPresentarse = { t -> viewModel.sePresento(t); probadorElegido = null },
            alNoPresentarse = { t -> viewModel.noSePresento(t); probadorElegido = null },
            alIngresoDirecto = { probadorElegido = null; alIngresoDirecto(p.probador.codigo) },
            alTerminarUso = { probadorElegido = null; alTerminarUso(p.probador.codigo) },
            alTerminarOrden = { viewModel.terminarOrdenamiento(p.probador.codigo); probadorElegido = null },
            alFueraServicio = { probadorElegido = null; alFueraServicio(p.probador.codigo) }
        )
    }

    turnoAbandono?.let { t ->
        AbandonoDialog(
            turno = t,
            alCerrar = { turnoAbandono = null },
            alConfirmar = { motivo -> viewModel.registrarAbandono(t, motivo); turnoAbandono = null }
        )
    }
}

private fun detalleProbador(p: ProbadorUi): String? {
    return when {
        p.turnoLlamado != null -> "Esperando a ${p.turnoLlamado.codigoTurno()}"
        p.uso != null -> "${p.uso.prendasIngresadas} prendas" + if (p.uso.conAtraso) " · con atraso" else ""
        p.probador.estado == EstadoProbador.FUERA_SERVICIO -> p.probador.motivoFueraServicio
        else -> p.probador.ubicacion
    }
}

// Franja de arriba con los numeros principales
@Composable
private fun Resumen(estado: SeccionUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatoChico("Libres", estado.libres.toString(), Modifier.weight(1f))
        DatoChico("En fila", estado.enEspera.toString(), Modifier.weight(1f))
        DatoChico(
            "Espera",
            if (estado.estimadoNuevo < 0) "—" else "${estado.estimadoNuevo} min",
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun DatoChico(titulo: String, valor: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
            AnimatedContent(targetState = valor, label = "datoChico") { v ->
                Text(v, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ListaFila(
    fila: List<TurnoUi>,
    alPresentarse: (TurnoEntity) -> Unit,
    alNoPresentarse: (TurnoEntity) -> Unit,
    alAbandonar: (TurnoEntity) -> Unit
) {
    if (fila.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("No hay nadie en la fila", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(fila, key = { it.turno.id }) { t ->
            // animateItem: los turnos se mueven con animacion cuando entran o salen de la fila
            Card(modifier = Modifier.fillMaxWidth().animateItem()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.turno.codigoTurno(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (t.turno.preferente) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Accessible, contentDescription = "Preferente", tint = MaterialTheme.colorScheme.primary)
                            Text(" Preferente", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.weight(1f))
                        val colorEstado = if (t.turno.estado == EstadoTurno.LLAMADO) ColorLibre else Color.DarkGray
                        Text(t.turno.estado.texto, color = colorEstado, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${t.turno.prendas} prendas · esperando ${t.minutosEsperando} min")
                    if (t.turno.estado == EstadoTurno.LLAMADO) {
                        Text("Llamado al ${t.turno.probadorLlamado}", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { alPresentarse(t.turno) }) { Text("Se presentó") }
                            OutlinedButton(onClick = { alNoPresentarse(t.turno) }) { Text("No se presentó") }
                        }
                    } else {
                        Text("Espera estimada: ${if (t.estimado < 0) "—" else "${t.estimado} min"}")
                        TextButton(onClick = { alAbandonar(t.turno) }) {
                            Text("Registrar abandono", color = ColorOcupado)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccionesProbadorDialog(
    p: ProbadorUi,
    hayFila: Boolean,
    alCerrar: () -> Unit,
    alLlamar: () -> Unit,
    alPresentarse: (TurnoEntity) -> Unit,
    alNoPresentarse: (TurnoEntity) -> Unit,
    alIngresoDirecto: () -> Unit,
    alTerminarUso: () -> Unit,
    alTerminarOrden: () -> Unit,
    alFueraServicio: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("${p.probador.codigo} · ${p.probador.estado.texto}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${p.probador.ubicacion} · hace ${p.minutos} min en este estado")
                when (p.probador.estado) {
                    EstadoProbador.LIBRE -> {
                        if (p.turnoLlamado != null) {
                            Text("Esperando a ${p.turnoLlamado.codigoTurno()}", fontWeight = FontWeight.Bold)
                            Button(onClick = { alPresentarse(p.turnoLlamado) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Se presentó: iniciar uso")
                            }
                            OutlinedButton(onClick = { alNoPresentarse(p.turnoLlamado) }, modifier = Modifier.fillMaxWidth()) {
                                Text("No se presentó")
                            }
                        } else {
                            Button(onClick = alLlamar, enabled = hayFila, modifier = Modifier.fillMaxWidth()) {
                                Text("Llamar al siguiente turno")
                            }
                            OutlinedButton(onClick = alIngresoDirecto, enabled = !hayFila, modifier = Modifier.fillMaxWidth()) {
                                Text("Ingreso directo (sin fila)")
                            }
                        }
                    }
                    EstadoProbador.OCUPADO -> {
                        if (p.uso != null) {
                            Text("Ingresó con ${p.uso.prendasIngresadas} prendas")
                        }
                        Button(onClick = alTerminarUso, modifier = Modifier.fillMaxWidth()) {
                            Text("Registrar salida del cliente")
                        }
                    }
                    EstadoProbador.ORDENAMIENTO -> {
                        Button(
                            onClick = alTerminarOrden,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorLibre),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Listo, dejar libre")
                        }
                    }
                    EstadoProbador.FUERA_SERVICIO -> {
                        Text("Motivo: ${p.probador.motivoFueraServicio ?: "—"}")
                    }
                }

                // RN-11: solo jefatura maneja fuera de servicio
                if (Sesion.esJefatura()) {
                    TextButton(onClick = alFueraServicio, modifier = Modifier.fillMaxWidth()) {
                        Text(if (p.probador.estado == EstadoProbador.FUERA_SERVICIO) "Ver detalle / habilitar" else "Marcar fuera de servicio")
                    }
                } else if (p.probador.estado != EstadoProbador.FUERA_SERVICIO) {
                    Text(
                        "¿Ves una falla? Avísale a la jefatura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun AbandonoDialog(
    turno: TurnoEntity,
    alCerrar: () -> Unit,
    alConfirmar: (MotivoAbandono) -> Unit
) {
    // HU-05 CA-2: por defecto "No informado"
    var motivo by remember { mutableStateOf(MotivoAbandono.NO_INFORMADO) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Abandono de ${turno.codigoTurno()}") },
        text = {
            Column {
                Text("¿Por qué se fue?")
                for (m in MotivoAbandono.elegibles()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = motivo == m, onClick = { motivo = m })
                    ) {
                        RadioButton(selected = motivo == m, onClick = { motivo = m })
                        Text(m.texto)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { alConfirmar(motivo) }) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text("Cancelar") }
        }
    )
}
