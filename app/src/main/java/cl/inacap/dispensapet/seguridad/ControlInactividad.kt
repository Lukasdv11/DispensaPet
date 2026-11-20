package cl.inacap.dispensapet.seguridad

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ControlInactividad {
    private const val LIMITE_MS = 5 * 60 * 1000L
    private var ultimaActividad = SystemClock.elapsedRealtime()

    var motivoCierre by mutableStateOf("")

    fun registrarActividad() {
        ultimaActividad = SystemClock.elapsedRealtime()
    }

    fun superoLimite(): Boolean = SystemClock.elapsedRealtime() - ultimaActividad > LIMITE_MS
}
