package cl.inacap.dispensapet.modelos

import java.util.Date

const val MAX_PORCIONES = 10

data class Dispensador(
    val id: String = "",
    val nombre: String = "",
    val mascota: String = "",
    val propietarioUid: String = "",
    val modoAutomatico: Boolean = false
) {
    val titulo: String
        get() = if (mascota.isBlank()) nombre else "$nombre ($mascota)"
}

data class Horario(
    val id: String = "",
    val hora: Int = 0,
    val minuto: Int = 0,
    val porciones: Int = 1,
    val activo: Boolean = true,
    val ultimaEjecucion: String = ""
) {
    val textoHora: String
        get() = "%02d:%02d".format(hora, minuto)
}

data class EventoHistorial(
    val id: String = "",
    val porciones: Int = 1,
    val origen: String = "",
    val fecha: Date? = null,
    val comandoId: String = ""
)

data class Comando(
    val id: String = "",
    val porciones: Int = 1,
    val creadoPor: String = "",
    val creadoEn: Date? = null,
    val estado: String = "",
    val detalle: String = ""
) {
    val textoEstado: String
        get() = when (estado) {
            EstadosComando.PENDIENTE -> "Pendiente"
            EstadosComando.ENVIADO -> "Recibida por el puente"
            EstadosComando.CONFIRMADO -> "Entregada"
            EstadosComando.RECHAZADO -> "Rechazada"
            EstadosComando.VENCIDO -> "Vencida: no se dispensó"
            EstadosComando.SIN_CONFIRMAR -> "Sin confirmar"
            else -> "Desconocido"
        }
}

data class EstadoPuenteRemoto(
    val enLinea: Boolean = false,
    val ultimaSenal: Date? = null,
    val nivel: Int = -1,
    val dispositivo: String = ""
) {
    fun estaEnLinea(ahoraMs: Long): Boolean {
        val senal = ultimaSenal ?: return false
        return enLinea && ahoraMs - senal.time < 60_000
    }
}

sealed class EstadoOperacion {
    object Inactivo : EstadoOperacion()
    object Cargando : EstadoOperacion()
    data class Exito(val mensaje: String, val dato: String = "") : EstadoOperacion()
    data class Error(val mensaje: String) : EstadoOperacion()
}

sealed class EstadoConexion {
    object Desconectado : EstadoConexion()
    data class Conectando(val dispositivo: String) : EstadoConexion()
    data class Conectado(val dispositivo: String) : EstadoConexion()
    data class Error(val mensaje: String) : EstadoConexion()
}

object EstadosComando {
    const val PENDIENTE = "PENDIENTE"
    const val ENVIADO = "ENVIADO"
    const val CONFIRMADO = "CONFIRMADO"
    const val RECHAZADO = "RECHAZADO"
    const val VENCIDO = "VENCIDO"
    const val SIN_CONFIRMAR = "SIN_CONFIRMAR"
}

object Origenes {
    const val APP = "APP"
    const val BOTON = "BOTON"
    const val HORARIO = "HORARIO"
}
