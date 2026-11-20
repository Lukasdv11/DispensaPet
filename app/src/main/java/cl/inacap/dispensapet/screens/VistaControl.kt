package cl.inacap.dispensapet.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ServicioAuth
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaControl(dispensadorId: String, alVolver: () -> Unit, alCerrarSesion: (String) -> Unit) {
    val contexto = LocalContext.current
    val uid = ServicioAuth.usuarioActual?.uid ?: ""
    var dispensador by remember { mutableStateOf<Dispensador?>(null) }
    var errorAcceso by remember { mutableStateOf("") }
    var pestana by rememberSaveable { mutableStateOf(0) }
    var mensajeHorario by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    DisposableEffect(dispensadorId) {
        val escucha = ServicioDatos.escucharDispensador(dispensadorId,
            alCambio = { datos ->
                dispensador = datos
                errorAcceso = ""
            },
            alError = { mensaje -> errorAcceso = mensaje }
        )
        onDispose { escucha.remove() }
    }

    val actual = dispensador

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(actual?.titulo ?: "Dispensador") },
                navigationIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.clickable { alVolver() }.padding(12.dp)
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                OpcionBarra(Icons.Default.Home, "Panel", pestana == 0, Modifier.weight(1f)) { pestana = 0 }
                OpcionBarra(Icons.Default.Schedule, "Horarios", pestana == 1, Modifier.weight(1f)) { pestana = 1 }
                OpcionBarra(Icons.Default.History, "Historial", pestana == 2, Modifier.weight(1f)) { pestana = 2 }
                OpcionBarra(Icons.Default.Settings, "Ajustes", pestana == 3, Modifier.weight(1f)) { pestana = 3 }
            }
        },
        floatingActionButton = {
            if (pestana == 1) {
                FloatingActionButton(onClick = {
                    val calendario = Calendar.getInstance()
                    TimePickerDialog(
                        contexto,
                        { _, hora, minuto ->
                            mensajeHorario = EstadoOperacion.Cargando
                            ServicioDatos.agregarHorario(dispensadorId, hora, minuto, 1) { resultado -> mensajeHorario = resultado }
                        },
                        calendario.get(Calendar.HOUR_OF_DAY),
                        calendario.get(Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar horario")
                }
            }
        }
    ) { relleno ->
        Box(modifier = Modifier.fillMaxSize().padding(relleno)) {
            when {
                errorAcceso.isNotEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorAcceso, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = alVolver) { Text("Volver") }
                }
                actual == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                pestana == 0 -> VistaPanel(actual, uid)
                pestana == 1 -> VistaHorarios(actual, mensajeHorario)
                pestana == 2 -> VistaHistorial(actual)
                else -> VistaAjustes(dispensador = actual, alCerrarSesion = { alCerrarSesion("") })
            }
        }
    }
}
