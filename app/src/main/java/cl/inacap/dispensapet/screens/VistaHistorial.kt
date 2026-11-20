package cl.inacap.dispensapet.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.comunicacion.GestorPuente
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Comando
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.modelos.EstadosComando
import cl.inacap.dispensapet.modelos.EventoHistorial
import cl.inacap.dispensapet.modelos.Origenes
import cl.inacap.dispensapet.utilidades.diaAnterior
import cl.inacap.dispensapet.utilidades.diaSiguiente
import cl.inacap.dispensapet.utilidades.inicioDelDia
import cl.inacap.dispensapet.utilidades.textoFecha
import cl.inacap.dispensapet.utilidades.textoHora
import java.util.Calendar
import java.util.Date

private data class FilaHistorial(val fecha: Date?, val porciones: Int, val titulo: String, val detalle: String, val icono: ImageVector)

@Composable
fun VistaHistorial(dispensador: Dispensador) {
    val contexto = LocalContext.current
    val hoyMs = inicioDelDia(Date()).time
    var diaMs by rememberSaveable { mutableStateOf(hoyMs) }
    var ordenes by remember { mutableStateOf(listOf<Comando>()) }
    var entregasSinBoton by remember { mutableStateOf(listOf<EventoHistorial>()) }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    DisposableEffect(dispensador.id, diaMs) {
        val desde = Date(diaMs)
        val hasta = diaSiguiente(desde)
        val escuchaOrdenes = ServicioDatos.escucharOrdenes(dispensador.id, desde, hasta,
            alCambio = { lista -> ordenes = lista },
            alError = { mensaje -> estado = EstadoOperacion.Error(mensaje) }
        )
        val escuchaEntregas = ServicioDatos.escucharHistorial(dispensador.id, desde, hasta,
            alCambio = { lista -> entregasSinBoton = lista.filter { it.origen != Origenes.APP } },
            alError = { mensaje -> estado = EstadoOperacion.Error(mensaje) }
        )
        onDispose {
            escuchaOrdenes.remove()
            escuchaEntregas.remove()
        }
    }

    val automaticas = entregasSinBoton.count { it.origen == Origenes.HORARIO }
    val botonFisico = entregasSinBoton.count { it.origen == Origenes.BOTON }
    val filas = (ordenes.map { filaDeOrden(it) } + entregasSinBoton.map { filaDeEntrega(it) })
        .sortedByDescending { it.fecha?.time ?: Long.MAX_VALUE }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Día anterior",
                modifier = Modifier
                    .clickable { diaMs = inicioDelDia(diaAnterior(Date(diaMs))).time }
                    .padding(12.dp)
            )
            Text(
                textoDia(diaMs, hoyMs),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val calendario = Calendar.getInstance()
                        calendario.timeInMillis = diaMs
                        val dialogo = DatePickerDialog(
                            contexto,
                            { _, anio, mes, dia -> diaMs = inicioDelDia(anio, mes, dia).time },
                            calendario.get(Calendar.YEAR),
                            calendario.get(Calendar.MONTH),
                            calendario.get(Calendar.DAY_OF_MONTH)
                        )
                        dialogo.datePicker.maxDate = System.currentTimeMillis()
                        dialogo.show()
                    }
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Día siguiente",
                tint = if (diaMs < hoyMs) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .clickable { if (diaMs < hoyMs) diaMs = inicioDelDia(diaSiguiente(Date(diaMs))).time }
                    .padding(12.dp)
            )
        }
        Text(
            "Toca la fecha para elegir otro día.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Botón Dispensar: ${ordenes.size}  ·  Horario automático: $automaticas" +
                if (botonFisico > 0) "  ·  Botón físico: $botonFisico" else "",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        MensajeEstado(estado)

        if (filas.isEmpty()) {
            Text("Ese día no hubo entregas ni se usó el botón Dispensar.", modifier = Modifier.padding(top = 12.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filas) { fila -> FilaDelHistorial(fila) }
        }
    }
}

private fun textoDia(diaMs: Long, hoyMs: Long): String {
    val fecha = Date(diaMs).textoFecha()
    return when (diaMs) {
        hoyMs -> "Hoy · $fecha"
        inicioDelDia(diaAnterior(Date(hoyMs))).time -> "Ayer · $fecha"
        else -> fecha
    }
}

private fun filaDeOrden(orden: Comando): FilaHistorial {
    val creado = orden.creadoEn
    val caducada = orden.estado == EstadosComando.PENDIENTE && creado != null &&
        System.currentTimeMillis() - creado.time > GestorPuente.VIGENCIA_COMANDO_MS
    val resultado = if (caducada) "Vencida: nadie la recibió" else orden.textoEstado
    return FilaHistorial(creado, orden.porciones, "Botón Dispensar", resultado, Icons.Default.Pets)
}

private fun filaDeEntrega(evento: EventoHistorial): FilaHistorial =
    if (evento.origen == Origenes.HORARIO) {
        FilaHistorial(evento.fecha, evento.porciones, "Horario automático", "Entregada", Icons.Default.Schedule)
    } else {
        FilaHistorial(evento.fecha, evento.porciones, "Botón del dispensador", "Entregada", Icons.Default.TouchApp)
    }

@Composable
private fun FilaDelHistorial(fila: FilaHistorial) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Icon(fila.icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("${fila.fecha?.textoHora() ?: "--:--"}  ·  ${fila.porciones} porción(es)", style = MaterialTheme.typography.titleSmall)
            Text("${fila.titulo}  ·  ${fila.detalle}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
