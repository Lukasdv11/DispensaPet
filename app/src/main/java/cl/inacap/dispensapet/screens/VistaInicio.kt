package cl.inacap.dispensapet.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.comunicacion.GestorPuente
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.utilidades.esNombreValido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaInicio(alAbrirControl: (String) -> Unit, alAbrirPuente: (String) -> Unit, alCerrarSesion: (String) -> Unit) {
    val usuario = ServicioAuth.usuarioActual
    val uid = usuario?.uid ?: ""
    val nombreUsuario = ServicioAuth.nombreVisible()

    var ids by remember { mutableStateOf(listOf<String>()) }
    var dispensadores by remember { mutableStateOf(listOf<Dispensador>()) }
    var seleccionId by rememberSaveable { mutableStateOf("") }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Cargando) }
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    var nombreNuevo by rememberSaveable { mutableStateOf("") }
    var mascotaNueva by rememberSaveable { mutableStateOf("") }
    var sesionLista by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        ServicioAuth.validarSesion { vigente ->
            if (vigente) sesionLista = true
            else alCerrarSesion("Tu sesión expiró. Vuelve a iniciar sesión.")
        }
    }

    DisposableEffect(uid, sesionLista) {
        if (uid.isEmpty() || !sesionLista) {
            onDispose {}
        } else {
            ServicioDatos.asegurarPerfil(uid, nombreUsuario) { resultado ->
                if (resultado is EstadoOperacion.Error) estado = resultado
            }
            val escucha = ServicioDatos.escucharPerfil(uid,
                alCambio = { lista -> ids = lista },
                alError = { mensaje -> estado = EstadoOperacion.Error(mensaje) }
            )
            onDispose { escucha.remove() }
        }
    }

    LaunchedEffect(ids) {
        ServicioDatos.cargarDispensadores(ids) { lista ->
            dispensadores = lista
            if (lista.none { it.id == seleccionId }) seleccionId = lista.firstOrNull()?.id ?: ""
            if (estado == EstadoOperacion.Cargando) estado = EstadoOperacion.Inactivo
        }
    }

    val seleccionado = dispensadores.firstOrNull { it.id == seleccionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, $nombreUsuario") },
                actions = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.clickable { alCerrarSesion("") }.padding(12.dp)
                    )
                }
            )
        }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (GestorPuente.activo) {
                Seccion("Este teléfono está funcionando como Puente") {
                    Button(onClick = { alAbrirPuente(GestorPuente.dispensadorActivo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver a la pantalla del Puente")
                    }
                }
            }

            Seccion("Tus dispensadores") {
                if (dispensadores.isEmpty()) {
                    Text("Aún no tienes dispensadores. Crea uno abajo.")
                } else {
                    SelectorDesplegable(
                        etiqueta = "Dispensador",
                        opciones = dispensadores.map { it.titulo },
                        seleccion = dispensadores.indexOfFirst { it.id == seleccionId },
                        alElegir = { indice -> seleccionId = dispensadores[indice].id }
                    )
                }
            }

            if (seleccionado != null) {
                Seccion("¿Cómo usarás este teléfono?") {
                    Button(onClick = { alAbrirControl(seleccionado.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo Control")
                    }
                    Text(
                        "Desde cualquier lugar con Internet: dispensar, horarios e historial.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { alAbrirPuente(seleccionado.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo Puente")
                    }
                    Text(
                        "Solo en el teléfono que queda en casa, junto al dispensador (Bluetooth + Internet).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Seccion("Nuevo dispensador") {
                if (!mostrarFormulario) {
                    OutlinedButton(onClick = { mostrarFormulario = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Agregar un dispensador")
                    }
                } else {
                    TextField(
                        value = nombreNuevo,
                        onValueChange = { nombreNuevo = it.take(40) },
                        label = { Text("Nombre (ej: Cocina)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = mascotaNueva,
                        onValueChange = { mascotaNueva = it.take(40) },
                        label = { Text("Nombre de la mascota (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            when {
                                !nombreNuevo.esNombreValido() -> estado = EstadoOperacion.Error("El nombre debe tener entre 2 y 40 caracteres.")
                                ids.size >= 5 -> estado = EstadoOperacion.Error("Puedes tener como máximo 5 dispensadores.")
                                else -> {
                                    estado = EstadoOperacion.Cargando
                                    ServicioDatos.crearDispensador(uid, nombreNuevo, mascotaNueva) { resultado ->
                                        estado = resultado
                                        if (resultado is EstadoOperacion.Exito) {
                                            seleccionId = resultado.dato
                                            nombreNuevo = ""
                                            mascotaNueva = ""
                                            mostrarFormulario = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = estado != EstadoOperacion.Cargando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear dispensador")
                    }
                }
            }
            MensajeEstado(estado)
        }
    }
}
