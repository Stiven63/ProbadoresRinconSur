package com.rinconsur.probadores.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rinconsur.probadores.model.Sesion
import com.rinconsur.probadores.ui.screens.ClienteScreen
import com.rinconsur.probadores.ui.screens.ColaboradoresScreen
import com.rinconsur.probadores.ui.screens.FueraServicioScreen
import com.rinconsur.probadores.ui.screens.IngresoDirectoScreen
import com.rinconsur.probadores.ui.screens.LoginScreen
import com.rinconsur.probadores.ui.screens.NuevoTurnoScreen
import com.rinconsur.probadores.ui.screens.PanelScreen
import com.rinconsur.probadores.ui.screens.ParametrosScreen
import com.rinconsur.probadores.ui.screens.ResumenScreen
import com.rinconsur.probadores.ui.screens.TerminarUsoScreen

// Nombres de las rutas de navegacion
object Rutas {
    const val LOGIN = "login"
    const val CLIENTE = "cliente"
    const val PANEL = "panel"
    const val NUEVO_TURNO = "nuevo_turno"
    const val TERMINAR_USO = "terminar_uso/{codigo}"
    const val INGRESO_DIRECTO = "ingreso_directo/{codigo}"
    const val FUERA_SERVICIO = "fuera_servicio/{codigo}"
    const val RESUMEN = "resumen"
    const val PARAMETROS = "parametros"
    const val COLABORADORES = "colaboradores"

    fun terminarUso(codigo: String) = "terminar_uso/$codigo"
    fun ingresoDirecto(codigo: String) = "ingreso_directo/$codigo"
    fun fueraServicio(codigo: String) = "fuera_servicio/$codigo"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val argCodigo = listOf(navArgument("codigo") { type = NavType.StringType })

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN,
        // animacion al cambiar de pantalla: desliza hacia el lado
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(350)) + fadeIn(tween(350))
        },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(350)) + fadeOut(tween(350))
        }
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                alIngresar = {
                    navController.navigate(Rutas.PANEL) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                alVerPantallaCliente = { navController.navigate(Rutas.CLIENTE) }
            )
        }

        composable(Rutas.CLIENTE) {
            ClienteScreen(alVolver = { navController.popBackStack() })
        }

        composable(Rutas.PANEL) {
            PanelScreen(
                alNuevoTurno = { navController.navigate(Rutas.NUEVO_TURNO) },
                alTerminarUso = { navController.navigate(Rutas.terminarUso(it)) },
                alIngresoDirecto = { navController.navigate(Rutas.ingresoDirecto(it)) },
                alFueraServicio = { navController.navigate(Rutas.fueraServicio(it)) },
                alPantallaCliente = { navController.navigate(Rutas.CLIENTE) },
                alResumen = { navController.navigate(Rutas.RESUMEN) },
                alParametros = { navController.navigate(Rutas.PARAMETROS) },
                alColaboradores = { navController.navigate(Rutas.COLABORADORES) },
                alCerrarSesion = {
                    Sesion.cerrar()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.PANEL) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.NUEVO_TURNO) {
            NuevoTurnoScreen(alVolver = { navController.popBackStack() })
        }

        composable(Rutas.TERMINAR_USO, arguments = argCodigo) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            TerminarUsoScreen(codigo = codigo, alVolver = { navController.popBackStack() })
        }

        composable(Rutas.INGRESO_DIRECTO, arguments = argCodigo) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            IngresoDirectoScreen(codigo = codigo, alVolver = { navController.popBackStack() })
        }

        composable(Rutas.FUERA_SERVICIO, arguments = argCodigo) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            FueraServicioScreen(codigo = codigo, alVolver = { navController.popBackStack() })
        }

        composable(Rutas.RESUMEN) {
            ResumenScreen(alVolver = { navController.popBackStack() })
        }

        composable(Rutas.PARAMETROS) {
            ParametrosScreen(alVolver = { navController.popBackStack() })
        }

        composable(Rutas.COLABORADORES) {
            ColaboradoresScreen(alVolver = { navController.popBackStack() })
        }
    }
}
