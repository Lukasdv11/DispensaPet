package cl.inacap.dispensapet.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ResultadoAcceso
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.seguridad.ControlInactividad
import cl.inacap.dispensapet.seguridad.ControlIntentos
import cl.inacap.dispensapet.utilidades.esCorreoValido
import cl.inacap.dispensapet.utilidades.normalizarCorreo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaAcceso(alIngresar: (Boolean) -> Unit, alIrARegistro: () -> Unit, alIrARecuperar: () -> Unit) {
    val contexto = LocalContext.current
    val control = remember { ControlIntentos(contexto) }

    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }
    var segundosBloqueo by remember { mutableStateOf(control.segundosRestantes()) }

    LaunchedEffect(segundosBloqueo > 0) {
        while (segundosBloqueo > 0) {
            delay(1000)
            segundosBloqueo = control.segundosRestantes()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("DispensaPet") }) }) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text("Inicia sesión", style = MaterialTheme.typography.headlineMedium)
            Text("Controla el dispensador de tu mascota de forma segura", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(20.dp))

            if (ControlInactividad.motivoCierre.isNotBlank()) {
                Text(ControlInactividad.motivoCierre, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextField(
                value = correo,
                onValueChange = {
                    correo = it.trim()
                    estado = EstadoOperacion.Inactivo
                },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            CampoContrasena(
                valor = contrasena,
                alCambiar = {
                    contrasena = it
                    estado = EstadoOperacion.Inactivo
                },
                etiqueta = "Contraseña"
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (segundosBloqueo > 0) {
                Text(
                    "Demasiados intentos fallidos. Podrás intentar de nuevo en $segundosBloqueo s.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    when {
                        correo.isBlank() || contrasena.isEmpty() ->
                            estado = EstadoOperacion.Error("Ingresa tu correo y tu contraseña.")
                        !correo.esCorreoValido() ->
                            estado = EstadoOperacion.Error("El formato del correo no es válido.")
                        else -> {
                            estado = EstadoOperacion.Cargando
                            ControlInactividad.motivoCierre = ""
                            ServicioAuth.iniciarSesion(correo.normalizarCorreo(), contrasena) { resultado ->
                                when (resultado) {
                                    is ResultadoAcceso.Correcto -> {
                                        control.registrarExito()
                                        contrasena = ""
                                        estado = EstadoOperacion.Inactivo
                                        ControlInactividad.registrarActividad()
                                        alIngresar(resultado.verificado)
                                    }
                                    ResultadoAcceso.SinConexion ->
                                        estado = EstadoOperacion.Error("Sin conexión a Internet. Este intento no se contó.")
                                    ResultadoAcceso.BloqueoServidor ->
                                        estado = EstadoOperacion.Error("El servidor bloqueó temporalmente los intentos. Espera unos minutos.")
                                    ResultadoAcceso.Incorrecto -> {
                                        val segundos = control.registrarFallo()
                                        segundosBloqueo = segundos
                                        estado = if (segundos > 0) {
                                            EstadoOperacion.Error("Correo o contraseña incorrectos.")
                                        } else {
                                            EstadoOperacion.Error("Correo o contraseña incorrectos. Intentos antes del bloqueo: ${control.intentosRestantes}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = estado != EstadoOperacion.Cargando && segundosBloqueo == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
            MensajeEstado(estado)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = alIrARegistro, modifier = Modifier.fillMaxWidth()) {
                Text("Crear una cuenta")
            }
            Text(
                "Olvidé mi contraseña",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { alIrARecuperar() }
                    .padding(12.dp)
            )
        }
    }
}
