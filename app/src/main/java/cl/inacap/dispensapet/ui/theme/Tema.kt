package cl.inacap.dispensapet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    secondary = Color(0xFF6D5A4A),
    tertiary = Color(0xFFC77D1A),
    error = Color(0xFFB3261E)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF7FD1A8),
    secondary = Color(0xFFD8C2AE),
    tertiary = Color(0xFFF2B866),
    error = Color(0xFFF2B8B5)
)

@Composable
fun TemaDispensaPet(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) EsquemaOscuro else EsquemaClaro,
        content = contenido
    )
}
