package cl.inacap.dispensapet.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import cl.inacap.dispensapet.utilidades.esNombreValido
import cl.inacap.dispensapet.utilidades.nivelFortaleza
import cl.inacap.dispensapet.utilidades.normalizarCorreo
import cl.inacap.dispensapet.utilidades.problemasContrasena

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaRegistro(alRegistrado: () -> Unit, alVolver: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    val problemas = contrasena.problemasContrasena(correo, nombre)
    val nivel = contrasena.nivelFortaleza()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            TextField(
                value = nombre,
                onValueChange = { nombre = it.take(40) },
                label = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = correo,
                onValueChange = { correo = it.trim() },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            if (correo.isNotBlank() && !correo.esCorreoValido()) {
                Text("Formato de correo no válido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            CampoContrasena(valor = contrasena, alCambiar = { contrasena = it }, etiqueta = "Contraseña")

            if (contrasena.isNotEmpty()) {
                IndicadorFortaleza(nivel)
                for (problema in problemas) {
                    Text("• $problema", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    "Mínimo 10 caracteres, con mayúscula, minúscula, número y símbolo, sin espacios.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            CampoContrasena(valor = confirmacion, alCambiar = { confirmacion = it }, etiqueta = "Confirmar contraseña")
            if (confirmacion.isNotEmpty() && confirmacion != contrasena) {
                Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        !nombre.esNombreValido() -> estado = EstadoOperacion.Error("Ingresa tu nombre (2 a 40 caracteres).")
                        !correo.esCorreoValido() -> estado = EstadoOperacion.Error("El formato del correo no es válido.")
                        problemas.isNotEmpty() -> estado = EstadoOperacion.Error("La contraseña no cumple todos los requisitos.")
                        contrasena != confirmacion -> estado = EstadoOperacion.Error("Las contraseñas no coinciden.")
                        else -> {
                            estado = EstadoOperacion.Cargando
                            ServicioAuth.registrar(nombre.trim(), correo.normalizarCorreo(), contrasena) { resultado ->
                                estado = resultado
                                if (resultado is EstadoOperacion.Exito) {
                                    contrasena = ""
                                    confirmacion = ""
                                    alRegistrado()
                                }
                            }
                        }
                    }
                },
                enabled = estado != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear cuenta")
            }
            MensajeEstado(estado)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Te enviaremos un correo para verificar tu cuenta. Sin verificarlo no podrás usar el dispensador.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
