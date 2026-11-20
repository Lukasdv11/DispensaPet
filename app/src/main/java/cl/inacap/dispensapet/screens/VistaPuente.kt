package cl.inacap.dispensapet.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.comunicacion.GestorPuente
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoConexion
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.seguridad.PermisosBluetooth
import cl.inacap.dispensapet.utilidades.hayInternet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaPuente(dispensadorId: String, alVolver: () -> Unit) {
    val contexto = LocalContext.current
    val vista = LocalView.current
    val adaptador: BluetoothAdapter? = remember { contexto.getSystemService(BluetoothManager::class.java)?.adapter }

    var dispensador by remember { mutableStateOf<Dispensador?>(null) }
    var errorAcceso by remember { mutableStateOf("") }
    var tienePermiso by remember { mutableStateOf(PermisosBluetooth.concedido(contexto)) }
    var encendido by remember { mutableStateOf(adaptador?.isEnabled == true) }
    var emparejados by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var elegido by rememberSaveable { mutableStateOf(-1) }
    var aviso by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    DisposableEffect(dispensadorId) {
        val escucha = ServicioDatos.escucharDispensador(dispensadorId,
            alCambio = { datos -> dispensador = datos },
            alError = { mensaje -> errorAcceso = mensaje }
        )
        onDispose { escucha.remove() }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        tienePermiso = concedido
        if (concedido) {
            aviso = EstadoOperacion.Inactivo
            encendido = adaptador?.isEnabled == true
        } else {
            aviso = EstadoOperacion.Error(
                "Sin este permiso el modo Puente no puede conectarse. Si lo negaste dos veces, actívalo en Ajustes > Apps > DispensaPet."
            )
        }
    }

    LaunchedEffect(tienePermiso, encendido) {
        if (tienePermiso && encendido) emparejados = PermisosBluetooth.emparejados(contexto, adaptador)
    }

    val puenteActivo = GestorPuente.activo
    DisposableEffect(puenteActivo) {
        vista.keepScreenOn = puenteActivo
        onDispose { vista.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo Puente") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                errorAcceso.isNotEmpty() -> Text(errorAcceso, color = MaterialTheme.colorScheme.error)
                dispensador == null -> CircularProgressIndicator()
                else -> {
                    if (adaptador == null) {
                        Text(
                            "Este teléfono no tiene Bluetooth: solo puedes usar el modo simulación.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Seccion("1. Permiso de Bluetooth") {
                            if (tienePermiso) {
                                Text("Permiso concedido ✓")
                            } else {
                                Text(
                                    "La app necesita el permiso \"Dispositivos cercanos\" solo para conectarse al dispensador " +
                                        "que ya emparejaste. No usa tu ubicación ni busca otros equipos.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { PermisosBluetooth.permiso?.let { pedirPermiso.launch(it) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Conceder permiso") }
                            }
                        }

                        Seccion("2. Bluetooth del teléfono") {
                            if (encendido) {
                                Text("Bluetooth encendido ✓")
                            } else {
                                Text(
                                    "El Bluetooth está apagado. Enciéndelo desde los ajustes del teléfono y toca \"Actualizar\".",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    encendido = adaptador.isEnabled
                                    emparejados = PermisosBluetooth.emparejados(contexto, adaptador)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Actualizar") }
                        }

                        Seccion("3. Dispensador emparejado") {
                            if (emparejados.isEmpty()) {
                                Text(
                                    "No hay equipos emparejados. Ve a Ajustes > Bluetooth del teléfono, empareja el HC-05 " +
                                        "con el PIN que configuraste y toca \"Actualizar\".",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                SelectorDesplegable(
                                    etiqueta = "Equipo Bluetooth del dispensador",
                                    opciones = emparejados.map { PermisosBluetooth.nombreDe(it) },
                                    seleccion = elegido,
                                    alElegir = { indice -> elegido = indice },
                                    habilitado = !puenteActivo
                                )
                            }
                        }
                    }

                    Seccion("4. Puente") {
                        val textoEstado = when (val estado = GestorPuente.estado) {
                            EstadoConexion.Desconectado -> "Detenido"
                            is EstadoConexion.Conectando -> "Conectando con ${estado.dispositivo}..."
                            is EstadoConexion.Conectado -> "Funcionando: conectado a ${estado.dispositivo}"
                            is EstadoConexion.Error -> "Detenido por error: ${estado.mensaje}"
                        }
                        Text(textoEstado, style = MaterialTheme.typography.titleSmall)
                        if (GestorPuente.estado is EstadoConexion.Conectado) {
                            val nivel = GestorPuente.nivel
                            Text("Nivel de comida: ${if (nivel < 0) "sin dato" else "$nivel %"}")
                        }
                        if (GestorPuente.ultimoEvento.isNotEmpty()) {
                            Text(GestorPuente.ultimoEvento, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = {
                                    val dispositivo = emparejados.getOrNull(elegido)
                                    when {
                                        dispositivo == null -> aviso = EstadoOperacion.Error("Elige el equipo Bluetooth del dispensador.")
                                        !contexto.hayInternet() -> aviso = EstadoOperacion.Error("El puente necesita Internet para recibir las órdenes.")
                                        else -> {
                                            aviso = EstadoOperacion.Inactivo
                                            GestorPuente.iniciar(dispensadorId, dispositivo)
                                        }
                                    }
                                },
                                enabled = !puenteActivo && tienePermiso && encendido,
                                modifier = Modifier.weight(1f)
                            ) { Text("Iniciar") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { GestorPuente.detener() },
                                enabled = GestorPuente.estado != EstadoConexion.Desconectado,
                                modifier = Modifier.weight(1f)
                            ) { Text("Detener") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (!contexto.hayInternet()) {
                                    aviso = EstadoOperacion.Error("El puente necesita Internet para recibir las órdenes.")
                                } else {
                                    aviso = EstadoOperacion.Inactivo
                                    GestorPuente.iniciarSimulacion(dispensadorId)
                                }
                            },
                            enabled = !puenteActivo,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Iniciar en modo simulación (sin Arduino)") }
                        Text(
                            "En simulación, este teléfono responde como el Arduino: confirma cada porción y baja el nivel de comida.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Deja esta pantalla abierta y el teléfono cargando. Si sales de la app, Android puede pausar " +
                                "el puente; en ese caso las órdenes no se ejecutan y caducan solas (no se dispensa tarde).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    MensajeEstado(aviso)
                }
            }
        }
    }
}
