package cl.inacap.dispensapet.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.modelos.Horario
import cl.inacap.dispensapet.modelos.MAX_PORCIONES

@Composable
fun VistaHorarios(dispensador: Dispensador, mensajeExterno: EstadoOperacion) {
    var horarios by remember { mutableStateOf(listOf<Horario>()) }
    var estado by remember { mutableStateOf<EstadoOperacion>(EstadoOperacion.Inactivo) }

    DisposableEffect(dispensador.id) {
        val escucha = ServicioDatos.escucharHorarios(dispensador.id,
            alCambio = { lista -> horarios = lista.sortedBy { it.hora * 60 + it.minuto } },
            alError = { mensaje -> estado = EstadoOperacion.Error(mensaje) }
        )
        onDispose { escucha.remove() }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Seccion("Modo automático") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dispensar según los horarios", modifier = Modifier.weight(1f))
                    Switch(
                        checked = dispensador.modoAutomatico,
                        onCheckedChange = { activo ->
                            ServicioDatos.cambiarModoAutomatico(dispensador.id, activo) { resultado -> estado = resultado }
                        }
                    )
                }
                Text(
                    "Los horarios los ejecuta el teléfono Puente. Si a esa hora está desconectado, esa porción " +
                        "NO se entrega después (así se evita alimentar dos veces).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            MensajeEstado(mensajeExterno)
            MensajeEstado(estado)
        }
        if (horarios.isEmpty()) {
            item {
                Text("No hay horarios. Toca + para agregar uno.")
            }
        }
        items(horarios) { horario ->
            FilaHorario(
                horario = horario,
                maximo = MAX_PORCIONES,
                alCambiarActivo = { activo ->
                    ServicioDatos.actualizarHorario(dispensador.id, horario.id, mapOf("activo" to activo)) { r -> estado = r }
                },
                alCambiarPorciones = { cantidad ->
                    ServicioDatos.actualizarHorario(dispensador.id, horario.id, mapOf("porciones" to cantidad)) { r -> estado = r }
                },
                alBorrar = {
                    ServicioDatos.borrarHorario(dispensador.id, horario.id) { r -> estado = r }
                }
            )
        }
    }
}

@Composable
fun FilaHorario(
    horario: Horario,
    maximo: Int,
    alCambiarActivo: (Boolean) -> Unit,
    alCambiarPorciones: (Int) -> Unit,
    alBorrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Text(
                horario.textoHora,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Switch(checked = horario.activo, onCheckedChange = alCambiarActivo)
            Icon(
                Icons.Default.Delete,
                contentDescription = "Borrar horario",
                modifier = Modifier.clickable { alBorrar() }.padding(8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Porciones:")
            SelectorPorciones(maximo, horario.porciones.coerceAtMost(maximo), alCambiarPorciones)
        }
    }
}
