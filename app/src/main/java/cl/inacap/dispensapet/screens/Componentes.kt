package cl.inacap.dispensapet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.inacap.dispensapet.modelos.EstadoOperacion

@Composable
fun MensajeEstado(estado: EstadoOperacion, modifier: Modifier = Modifier) {
    when (estado) {
        EstadoOperacion.Inactivo -> {}
        EstadoOperacion.Cargando -> Row(modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Procesando...")
        }
        is EstadoOperacion.Exito -> if (estado.mensaje.isNotBlank()) {
            Text(estado.mensaje, color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(vertical = 4.dp))
        }
        is EstadoOperacion.Error -> Text(
            estado.mensaje,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun CampoContrasena(valor: String, alCambiar: (String) -> Unit, etiqueta: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    TextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            Icon(
                imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                modifier = Modifier
                    .clickable { visible = !visible }
                    .padding(8.dp)
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun IndicadorFortaleza(nivel: Int) {
    val color = when (nivel) {
        0, 1 -> Color(0xFFD32F2F)
        2 -> Color(0xFFF57C00)
        3 -> Color(0xFFC0A000)
        else -> Color(0xFF388E3C)
    }
    val texto = when (nivel) {
        0 -> "Muy débil"
        1 -> "Débil"
        2 -> "Regular"
        3 -> "Buena"
        else -> "Segura"
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (segmento in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(6.dp)
                        .background(if (segmento <= nivel) color else Color.LightGray)
                )
            }
        }
        Text("Fortaleza: $texto", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun SelectorDesplegable(
    etiqueta: String,
    opciones: List<String>,
    seleccion: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = habilitado) { abierto = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelSmall)
                Text(opciones.getOrNull(seleccion) ?: "Selecciona una opción")
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir lista")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { indice, texto ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        abierto = false
                        alElegir(indice)
                    }
                )
            }
        }
    }
}

@Composable
fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        contenido()
    }
}

@Composable
fun SelectorPorciones(maximo: Int, seleccion: Int, alElegir: (Int) -> Unit, habilitado: Boolean = true) {
    val puedeBajar = habilitado && seleccion > 1
    val puedeSubir = habilitado && seleccion < maximo
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = "Menos porciones",
            tint = if (puedeBajar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable(enabled = puedeBajar) { alElegir(seleccion - 1) }
                .padding(8.dp)
        )
        Text("$seleccion", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
        Icon(
            Icons.Default.KeyboardArrowUp,
            contentDescription = "Más porciones",
            tint = if (puedeSubir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable(enabled = puedeSubir) { alElegir(seleccion + 1) }
                .padding(8.dp)
        )
        Text("máx. $maximo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun PasoSeguimiento(texto: String, hecho: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (hecho) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (hecho) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(texto)
    }
}

@Composable
fun OpcionBarra(icono: ImageVector, texto: String, seleccionada: Boolean, modifier: Modifier, alTocar: () -> Unit) {
    val color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable { alTocar() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, contentDescription = texto, tint = color)
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
