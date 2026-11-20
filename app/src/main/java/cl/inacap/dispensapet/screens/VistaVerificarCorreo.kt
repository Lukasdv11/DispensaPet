package cl.inacap.dispensapet.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.modelos.EstadoOperacion
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaVerificarCorreo(alVerificado: () -> Unit, alCerrarSesion: () -> Unit) {
    val correo = ServicioAuth.usuarioActual?.email ?: ""
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }
    var espera by remember { mutableStateOf(0) }

    LaunchedEffect(espera > 0) {
        while (espera > 0) {
            delay(1000)
            espera = espera - 1
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Verifica tu correo") }) }) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Enviamos un enlace de verificación a:")
            Text(correo, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ábrelo y luego vuelve aquí. Por seguridad, no podrás usar el dispensador hasta verificar tu correo.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    estado = EstadoOperacion.Cargando
                    ServicioAuth.comprobarVerificacion { verificado, mensaje ->
                        if (verificado) {
                            estado = EstadoOperacion.Inactivo
                            alVerificado()
                        } else {
                            estado = EstadoOperacion.Error(mensaje)
                        }
                    }
                },
                enabled = estado != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ya verifiqué mi correo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    estado = EstadoOperacion.Cargando
                    ServicioAuth.reenviarVerificacion { resultado ->
                        estado = resultado
                        espera = 60
                    }
                },
                enabled = espera == 0 && estado != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (espera > 0) "Reenviar en $espera s" else "Reenviar correo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = alCerrarSesion, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
            MensajeEstado(estado)
        }
    }
}
