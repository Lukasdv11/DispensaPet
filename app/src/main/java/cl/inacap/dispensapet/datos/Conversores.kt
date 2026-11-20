package cl.inacap.dispensapet.datos

import cl.inacap.dispensapet.modelos.Comando
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoPuenteRemoto
import cl.inacap.dispensapet.modelos.EventoHistorial
import cl.inacap.dispensapet.modelos.Horario
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.aDispensador(): Dispensador = Dispensador(
    id = id,
    nombre = getString("nombre") ?: "",
    mascota = getString("mascota") ?: "",
    propietarioUid = getString("propietarioUid") ?: "",
    modoAutomatico = getBoolean("modoAutomatico") ?: false
)

fun DocumentSnapshot.aHorario(): Horario = Horario(
    id = id,
    hora = entero("hora", 0),
    minuto = entero("minuto", 0),
    porciones = entero("porciones", 1),
    activo = getBoolean("activo") ?: false,
    ultimaEjecucion = getString("ultimaEjecucion") ?: ""
)

fun DocumentSnapshot.aEvento(): EventoHistorial = EventoHistorial(
    id = id,
    porciones = entero("porciones", 1),
    origen = getString("origen") ?: "",
    fecha = getTimestamp("fecha")?.toDate(),
    comandoId = getString("comandoId") ?: ""
)

fun DocumentSnapshot.aComando(): Comando = Comando(
    id = id,
    porciones = entero("porciones", 1),
    creadoPor = getString("creadoPor") ?: "",
    creadoEn = getTimestamp("creadoEn")?.toDate(),
    estado = getString("estado") ?: "",
    detalle = getString("detalle") ?: ""
)

fun DocumentSnapshot.aEstadoPuente(): EstadoPuenteRemoto = EstadoPuenteRemoto(
    enLinea = getBoolean("enLinea") ?: false,
    ultimaSenal = getTimestamp("ultimaSenal")?.toDate(),
    nivel = entero("nivel", -1),
    dispositivo = getString("dispositivo") ?: ""
)

private fun DocumentSnapshot.entero(campo: String, porDefecto: Int): Int =
    getLong(campo)?.toInt() ?: porDefecto
