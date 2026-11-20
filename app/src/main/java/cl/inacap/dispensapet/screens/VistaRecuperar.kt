package cl.inacap.dispensapet.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.utilidades.esCorreoValido
import cl.inacap.dispensapet.utilidades.normalizarCorreo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaRecuperar(alVolver: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña") },
                navigationIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.clickable { alVolver() }.padding(12.dp)
                    )
                }
            )
        }
    ) { relleno ->
        Column(modifier = Modifier.fillMaxSize().padding(relleno).padding(24.dp)) {
            Text("Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva.")
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = correo,
                onValueChange = { correo = it.trim() },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!correo.esCorreoValido()) {
                        estado = EstadoOperacion.Error("El formato del correo no es válido.")
                    } else {
                        estado = EstadoOperacion.Cargando
                        ServicioAuth.recuperarContrasena(correo.normalizarCorreo()) { resultado -> estado = resultado }
                    }
                },
                enabled = estado != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar enlace")
            }
            MensajeEstado(estado)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Por seguridad, el mensaje es el mismo exista o no la cuenta.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
