package cl.inacap.dispensapet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.comunicacion.CanalFirestore
import cl.inacap.dispensapet.comunicacion.GestorPuente
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Comando
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.modelos.EstadoPuenteRemoto
import cl.inacap.dispensapet.modelos.EstadosComando
import cl.inacap.dispensapet.modelos.MAX_PORCIONES
import cl.inacap.dispensapet.utilidades.diaSiguiente
import cl.inacap.dispensapet.utilidades.hayInternet
import cl.inacap.dispensapet.utilidades.inicioDelDia
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun VistaPanel(dispensador: Dispensador, uid: String) {
    val contexto = LocalContext.current
    val maximo = MAX_PORCIONES
    val canal = remember(dispensador.id) { CanalFirestore(dispensador.id, uid) }

    var puente by remember { mutableStateOf<EstadoPuenteRemoto?>(null) }
    var porcionesHoy by remember { mutableStateOf(0) }
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    var porciones by rememberSaveable { mutableStateOf(1) }
    var envio by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }
    var comandoId by rememberSaveable { mutableStateOf("") }
    var enviadoEnMs by rememberSaveable { mutableStateOf(0L) }
    var comando by remember { mutableStateOf<Comando?>(null) }

    DisposableEffect(dispensador.id) {
        val escuchaPuente = ServicioDatos.escucharEstadoPuente(dispensador.id) { datos -> puente = datos }
        val inicio = inicioDelDia(Date())
        val escuchaHoy = ServicioDatos.escucharHistorial(dispensador.id, inicio, diaSiguiente(inicio),
            alCambio = { lista -> porcionesHoy = lista.sumOf { it.porciones } },
            alError = {}
        )
        onDispose {
            escuchaPuente.remove()
            escuchaHoy.remove()
        }
    }

    DisposableEffect(comandoId) {
        if (comandoId.isEmpty()) {
            onDispose {}
        } else {
            val escucha = ServicioDatos.escucharComando(dispensador.id, comandoId) { datos -> comando = datos }
            onDispose { escucha.remove() }
        }
    }

    LaunchedEffect(comandoId) {
        if (comandoId.isNotEmpty()) {
            val restante = enviadoEnMs + GestorPuente.VIGENCIA_COMANDO_MS - System.currentTimeMillis()
            if (restante > 0) delay(restante)
            if (comando?.estado == EstadosComando.PENDIENTE) {
                ServicioDatos.actualizarComando(
                    dispensador.id, comandoId, EstadosComando.VENCIDO,
                    "Nadie la recibió en 45 s; no se dispensó."
                ) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(5_000)
        }
    }

    val enLinea = puente?.estaEnLinea(ahora) == true
    val enCamino = comando?.estado == EstadosComando.PENDIENTE || comando?.estado == EstadosComando.ENVIADO
    val esperandoConfirmacion = enCamino && ahora - enviadoEnMs < 60_000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Seccion("Estado del dispensador") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (enLinea) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (enLinea) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (enLinea) "Puente en línea" else "Puente sin conexión", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                if (enLinea) "El teléfono de casa está conectado al dispensador."
                else "El teléfono Puente no está enviando señal. Las órdenes no se ejecutarán y las atrasadas caducan solas.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            NivelComida(puente?.nivel ?: -1)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hoy: $porcionesHoy porciones entregadas")
        }

        Seccion("Dispensar ahora") {
            Text("Porciones")
            SelectorPorciones(maximo, porciones.coerceIn(1, maximo), { porciones = it })
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val cantidad = porciones.coerceIn(1, maximo)
                    when {
                        !contexto.hayInternet() -> envio = EstadoOperacion.Error("Sin conexión a Internet. La orden no se envió.")
                        else -> {
                            val puenteEnLinea = enLinea
                            canal.solicitarDispensa(cantidad) { resultado ->
                                envio = if (resultado is EstadoOperacion.Exito && !puenteEnLinea) {
                                    EstadoOperacion.Exito(
                                        "Orden enviada, pero el puente está desconectado: si nadie la recibe en 45 s, caduca y NO se dispensará.",
                                        resultado.dato
                                    )
                                } else {
                                    resultado
                                }
                                if (resultado is EstadoOperacion.Exito) {
                                    comando = null
                                    comandoId = resultado.dato
                                    enviadoEnMs = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                },
                enabled = envio != EstadoOperacion.Cargando && !esperandoConfirmacion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dispensar")
            }
            MensajeEstado(envio)
            comando?.let { orden -> SeguimientoOrden(orden, ahora - enviadoEnMs) }
        }
    }
}

@Composable
fun SeguimientoOrden(orden: Comando, msDesdeEnvio: Long) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        PasoSeguimiento("Enviada al servidor", hecho = true)
        PasoSeguimiento(
            "Recibida por el teléfono Puente",
            hecho = orden.estado == EstadosComando.ENVIADO || orden.estado == EstadosComando.CONFIRMADO
        )
        PasoSeguimiento("Confirmada por el Arduino", hecho = orden.estado == EstadosComando.CONFIRMADO)
        when (orden.estado) {
            EstadosComando.RECHAZADO -> Text("Rechazada: ${orden.detalle}", color = MaterialTheme.colorScheme.error)
            EstadosComando.VENCIDO -> Text("Vencida: ${orden.detalle}", color = MaterialTheme.colorScheme.error)
            EstadosComando.SIN_CONFIRMAR -> Text("Sin confirmar: ${orden.detalle}", color = MaterialTheme.colorScheme.error)
            EstadosComando.PENDIENTE -> if (msDesdeEnvio > GestorPuente.VIGENCIA_COMANDO_MS) {
                Text(
                    "Nadie la recibió en 45 s: caducó y NO se dispensará más tarde.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NivelComida(nivel: Int) {
    Column {
        if (nivel < 0) {
            Text("Nivel de comida: sin dato del sensor")
        } else {
            Text("Nivel de comida: $nivel %")
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (segmento in 1..10) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(1.dp)
                            .height(14.dp)
                            .background(if (nivel >= segmento * 10) MaterialTheme.colorScheme.primary else Color.LightGray)
                    )
                }
            }
            if (nivel < 20) Text("Queda poca comida: rellena la tolva.", color = MaterialTheme.colorScheme.error)
        }
    }
}
