package cl.inacap.dispensapet.comunicacion

sealed class RespuestaArduino {
    data class Nivel(val porcentaje: Int) : RespuestaArduino()
    data class Entrega(val origen: String) : RespuestaArduino()
    data class Rechazo(val motivo: String) : RespuestaArduino()
    object Listo : RespuestaArduino()
    data class Desconocida(val texto: String) : RespuestaArduino()
}

object ProtocoloArduino {
    const val COMANDO_DISPENSAR = "D"
    const val COMANDO_NIVEL = "N"

    fun interpretar(linea: String): RespuestaArduino {
        val texto = linea.trim()
        return when {
            texto.startsWith("NIVEL:") -> {
                val numero = texto.removePrefix("NIVEL:").trim().toIntOrNull()
                if (numero != null && numero in 0..100) RespuestaArduino.Nivel(numero)
                else RespuestaArduino.Desconocida(texto)
            }
            texto == "ENTREGA:APP" -> RespuestaArduino.Entrega("APP")
            texto == "ENTREGA:BOTON" -> RespuestaArduino.Entrega("BOTON")
            texto.startsWith("RECHAZO:") -> RespuestaArduino.Rechazo(texto.removePrefix("RECHAZO:").take(20))
            texto == "LISTO" -> RespuestaArduino.Listo
            else -> RespuestaArduino.Desconocida(texto)
        }
    }
}
