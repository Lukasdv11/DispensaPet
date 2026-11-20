package cl.inacap.dispensapet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.inacap.dispensapet.comunicacion.GestorPuente
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.screens.VistaAcceso
import cl.inacap.dispensapet.screens.VistaControl
import cl.inacap.dispensapet.screens.VistaInicio
import cl.inacap.dispensapet.screens.VistaPuente
import cl.inacap.dispensapet.screens.VistaRecuperar
import cl.inacap.dispensapet.screens.VistaRegistro
import cl.inacap.dispensapet.screens.VistaVerificarCorreo
import cl.inacap.dispensapet.seguridad.ControlInactividad
import kotlinx.coroutines.delay

object Rutas {
    const val ACCESO = "acceso"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val VERIFICAR = "verificar"
    const val INICIO = "inicio"
    const val CONTROL = "control/{dispensadorId}"
    const val PUENTE = "puente/{dispensadorId}"

    fun control(id: String) = "control/$id"
    fun puente(id: String) = "puente/$id"
}

@Composable
fun NavegacionApp() {
    val navController = rememberNavController()

    val destinoInicial = remember {
        val usuario = ServicioAuth.usuarioActual
        when {
            usuario == null -> Rutas.ACCESO
            !usuario.isEmailVerified -> Rutas.VERIFICAR
            else -> Rutas.INICIO
        }
    }

    fun irSinVolver(ruta: String) {
        navController.navigate(ruta) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun cerrarSesion(motivo: String) {
        GestorPuente.detener()
        ServicioAuth.cerrarSesion()
        ControlInactividad.motivoCierre = motivo
        irSinVolver(Rutas.ACCESO)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            if (ServicioAuth.usuarioActual != null && !GestorPuente.activo && ControlInactividad.superoLimite()) {
                cerrarSesion("Por seguridad, la sesión se cerró tras 5 minutos sin uso.")
            }
        }
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(Rutas.ACCESO) {
            VistaAcceso(
                alIngresar = { verificado -> irSinVolver(if (verificado) Rutas.INICIO else Rutas.VERIFICAR) },
                alIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                alIrARecuperar = { navController.navigate(Rutas.RECUPERAR) }
            )
        }
        composable(Rutas.REGISTRO) {
            VistaRegistro(
                alRegistrado = { irSinVolver(Rutas.VERIFICAR) },
                alVolver = { navController.popBackStack() }
            )
        }
        composable(Rutas.RECUPERAR) {
            VistaRecuperar(alVolver = { navController.popBackStack() })
        }
        composable(Rutas.VERIFICAR) {
            VistaVerificarCorreo(
                alVerificado = { irSinVolver(Rutas.INICIO) },
                alCerrarSesion = { cerrarSesion("") }
            )
        }
        composable(Rutas.INICIO) {
            VistaInicio(
                alAbrirControl = { id -> navController.navigate(Rutas.control(id)) },
                alAbrirPuente = { id -> navController.navigate(Rutas.puente(id)) },
                alCerrarSesion = { motivo -> cerrarSesion(motivo) }
            )
        }
        composable(Rutas.CONTROL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("dispensadorId") ?: ""
            VistaControl(
                dispensadorId = id,
                alVolver = { navController.popBackStack() },
                alCerrarSesion = { motivo -> cerrarSesion(motivo) }
            )
        }
        composable(Rutas.PUENTE) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("dispensadorId") ?: ""
            VistaPuente(
                dispensadorId = id,
                alVolver = { navController.popBackStack() }
            )
        }
    }
}
