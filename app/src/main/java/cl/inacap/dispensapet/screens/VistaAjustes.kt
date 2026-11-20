package cl.inacap.dispensapet.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.utilidades.esNombreValido

@Composable
fun VistaAjustes(dispensador: Dispensador, alCerrarSesion: () -> Unit) {
    val correo = ServicioAuth.usuarioActual?.email ?: ""
    var nombre by rememberSaveable(dispensador.id) { mutableStateOf(dispensador.nombre) }
    var mascota by rememberSaveable(dispensador.id) { mutableStateOf(dispensador.mascota) }
    var estadoDatos by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }
    var estadoCuenta by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    val hayCambios = nombre.trim() != dispensador.nombre || mascota.trim() != dispensador.mascota

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Seccion("Datos del dispensador") {
            TextField(
                value = nombre,
                onValueChange = { nombre = it.take(40) },
                label = { Text("Nombre del dispensador") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = mascota,
                onValueChange = { mascota = it.take(40) },
                label = { Text("Nombre de la mascota (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!nombre.esNombreValido()) {
                        estadoDatos = EstadoOperacion.Error("El nombre debe tener entre 2 y 40 caracteres.")
                    } else {
                        estadoDatos = EstadoOperacion.Cargando
                        ServicioDatos.actualizarDatos(dispensador.id, nombre, mascota) { r -> estadoDatos = r }
                    }
                },
                enabled = hayCambios && estadoDatos != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar") }
            MensajeEstado(estadoDatos)
        }

        Seccion("Mi cuenta") {
            Text(ServicioAuth.nombreVisible(), style = MaterialTheme.typography.titleSmall)
            Text(correo, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    estadoCuenta = EstadoOperacion.Cargando
                    ServicioAuth.recuperarContrasena(correo) { r ->
                        estadoCuenta = if (r is EstadoOperacion.Exito) {
                            EstadoOperacion.Exito("Te enviamos un enlace a $correo para cambiar tu contraseña.")
                        } else {
                            r
                        }
                    }
                },
                enabled = correo.isNotEmpty() && estadoCuenta != EstadoOperacion.Cargando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cambiar contraseña") }
            MensajeEstado(estadoCuenta)
        }

        Seccion("Sesión") {
            Button(onClick = alCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        }
    }
}
