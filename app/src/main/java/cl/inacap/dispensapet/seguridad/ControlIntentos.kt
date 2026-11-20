package cl.inacap.dispensapet.seguridad

import android.content.Context

class ControlIntentos(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("control_acceso", Context.MODE_PRIVATE)

    val intentosFallidos: Int
        get() = preferencias.getInt(CLAVE_INTENTOS, 0)

    val intentosRestantes: Int
        get() = MAX_INTENTOS - intentosFallidos

    private val bloqueadoHasta: Long
        get() = preferencias.getLong(CLAVE_BLOQUEO, 0L)

    fun segundosRestantes(): Int {
        val resto = bloqueadoHasta - System.currentTimeMillis()
        return if (resto > 0) ((resto + 999) / 1000).toInt() else 0
    }

    fun registrarFallo(): Int {
        val intentos = intentosFallidos + 1
        if (intentos >= MAX_INTENTOS) {
            preferencias.edit()
                .putInt(CLAVE_INTENTOS, 0)
                .putLong(CLAVE_BLOQUEO, System.currentTimeMillis() + SEGUNDOS_BLOQUEO * 1000L)
                .commit()
            return SEGUNDOS_BLOQUEO
        }
        preferencias.edit().putInt(CLAVE_INTENTOS, intentos).commit()
        return 0
    }

    fun registrarExito() {
        preferencias.edit().clear().commit()
    }

    companion object {
        const val MAX_INTENTOS = 3
        const val SEGUNDOS_BLOQUEO = 30
        private const val CLAVE_INTENTOS = "intentos"
        private const val CLAVE_BLOQUEO = "bloqueado_hasta"
    }
}
