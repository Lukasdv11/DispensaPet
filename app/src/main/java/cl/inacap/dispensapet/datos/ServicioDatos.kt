package cl.inacap.dispensapet.datos

import cl.inacap.dispensapet.modelos.Comando
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.modelos.EstadoPuenteRemoto
import cl.inacap.dispensapet.modelos.EstadosComando
import cl.inacap.dispensapet.modelos.EventoHistorial
import cl.inacap.dispensapet.modelos.Horario
import cl.inacap.dispensapet.utilidades.esPermisoDenegado
import cl.inacap.dispensapet.utilidades.mensajeDeError
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Date

object ServicioDatos {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun refUsuario(uid: String) = db.collection("usuarios").document(uid)
    private fun refDispensador(id: String) = db.collection("dispensadores").document(id)

    private fun exito(mensaje: String = "", dato: String = "") = EstadoOperacion.Exito(mensaje, dato)
    private fun fallo(error: Exception) = EstadoOperacion.Error(mensajeDeError(error))

    fun asegurarPerfil(uid: String, nombre: String, alTerminar: (EstadoOperacion) -> Unit) {
        refUsuario(uid).set(mapOf("nombre" to nombre.take(40)), SetOptions.merge())
            .addOnSuccessListener { alTerminar(exito()) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun escucharPerfil(uid: String, alCambio: (List<String>) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refUsuario(uid).addSnapshotListener { documento, error ->
            if (error != null) {
                alError(mensajeDeError(error))
                return@addSnapshotListener
            }
            val ids = (documento?.get("dispensadores") as? List<*>)?.mapNotNull { it as? String } ?: listOf()
            alCambio(ids)
        }

    fun cargarDispensadores(ids: List<String>, alTerminar: (List<Dispensador>) -> Unit) {
        if (ids.isEmpty()) {
            alTerminar(listOf())
            return
        }
        val encontrados = mutableListOf<Dispensador>()
        var pendientes = ids.size
        for (id in ids) {
            refDispensador(id).get().addOnCompleteListener { tarea ->
                val documento = if (tarea.isSuccessful) tarea.result else null
                if (documento != null && documento.exists()) {
                    encontrados.add(documento.aDispensador())
                }
                pendientes--
                if (pendientes == 0) alTerminar(encontrados.sortedBy { it.nombre })
            }
        }
    }

    fun crearDispensador(uid: String, nombre: String, mascota: String, alTerminar: (EstadoOperacion) -> Unit) {
        val referencia = db.collection("dispensadores").document()
        val datos = mapOf(
            "nombre" to nombre.trim(),
            "mascota" to mascota.trim(),
            "propietarioUid" to uid,
            "modoAutomatico" to false,
            "creadoEn" to FieldValue.serverTimestamp()
        )
        val lote = db.batch()
        lote.set(referencia, datos)
        lote.set(refUsuario(uid), mapOf("dispensadores" to FieldValue.arrayUnion(referencia.id)), SetOptions.merge())
        lote.commit()
            .addOnSuccessListener { alTerminar(exito("Dispensador creado.", referencia.id)) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun escucharDispensador(id: String, alCambio: (Dispensador) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refDispensador(id).addSnapshotListener { documento, error ->
            if (error != null) {
                if (esPermisoDenegado(error)) alError("Ya no tienes acceso a este dispensador.")
                else alError(mensajeDeError(error))
                return@addSnapshotListener
            }
            if (documento == null || !documento.exists()) {
                alError("Este dispensador ya no existe.")
                return@addSnapshotListener
            }
            alCambio(documento.aDispensador())
        }

    fun actualizarDatos(id: String, nombre: String, mascota: String, alTerminar: (EstadoOperacion) -> Unit) {
        refDispensador(id).update(mapOf("nombre" to nombre.trim(), "mascota" to mascota.trim()))
            .addOnSuccessListener { alTerminar(exito("Datos guardados.")) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun cambiarModoAutomatico(id: String, activo: Boolean, alTerminar: (EstadoOperacion) -> Unit) {
        refDispensador(id).update("modoAutomatico", activo)
            .addOnSuccessListener {
                alTerminar(exito(if (activo) "Modo automático activado." else "Modo automático desactivado."))
            }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun crearComando(id: String, uid: String, porciones: Int, alTerminar: (EstadoOperacion) -> Unit) {
        val refDisp = refDispensador(id)
        val refComando = refDisp.collection("comandos").document()
        db.runTransaction { transaccion ->
            transaccion.get(refDisp)
            transaccion.set(
                refComando,
                mapOf(
                    "tipo" to "DISPENSAR",
                    "porciones" to porciones,
                    "creadoPor" to uid,
                    "creadoEn" to FieldValue.serverTimestamp(),
                    "estado" to EstadosComando.PENDIENTE,
                    "detalle" to ""
                )
            )
            true
        }
            .addOnSuccessListener { alTerminar(exito("Orden enviada.", refComando.id)) }
            .addOnFailureListener { error ->
                if (esPermisoDenegado(error)) {
                    alTerminar(EstadoOperacion.Error("El servidor rechazó la orden: revisa la cantidad de porciones."))
                } else {
                    alTerminar(fallo(error))
                }
            }
    }

    fun escucharComando(id: String, comandoId: String, alCambio: (Comando?) -> Unit): ListenerRegistration =
        refDispensador(id).collection("comandos").document(comandoId).addSnapshotListener { documento, error ->
            if (error != null || documento == null || !documento.exists()) {
                alCambio(null)
                return@addSnapshotListener
            }
            alCambio(documento.aComando())
        }

    fun escucharComandosPendientes(id: String, alCambio: (List<Comando>) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refDispensador(id).collection("comandos")
            .whereEqualTo("estado", EstadosComando.PENDIENTE)
            .addSnapshotListener { resultado, error ->
                if (error != null) {
                    alError(mensajeDeError(error))
                    return@addSnapshotListener
                }
                alCambio(resultado?.documents?.map { it.aComando() } ?: listOf())
            }

    fun reclamarComando(id: String, comandoId: String, alTerminar: (Boolean) -> Unit) {
        val referencia = refDispensador(id).collection("comandos").document(comandoId)
        db.runTransaction { transaccion ->
            val actual = transaccion.get(referencia)
            if (actual.getString("estado") == EstadosComando.PENDIENTE) {
                transaccion.update(referencia, mapOf("estado" to EstadosComando.ENVIADO, "detalle" to "Recibido por el puente"))
                true
            } else {
                false
            }
        }
            .addOnSuccessListener { reclamado -> alTerminar(reclamado == true) }
            .addOnFailureListener { alTerminar(false) }
    }

    fun actualizarComando(id: String, comandoId: String, estado: String, detalle: String, alError: (String) -> Unit) {
        refDispensador(id).collection("comandos").document(comandoId)
            .update(mapOf("estado" to estado, "detalle" to detalle.take(80)))
            .addOnFailureListener { alError(mensajeDeError(it)) }
    }

    fun escucharEstadoPuente(id: String, alCambio: (EstadoPuenteRemoto?) -> Unit): ListenerRegistration =
        refDispensador(id).collection("estado").document("puente").addSnapshotListener { documento, error ->
            if (error != null || documento == null || !documento.exists()) {
                alCambio(null)
                return@addSnapshotListener
            }
            alCambio(documento.aEstadoPuente())
        }

    fun publicarEstadoPuente(id: String, enLinea: Boolean, nivel: Int, dispositivo: String, alError: (String) -> Unit) {
        val datos = mapOf(
            "enLinea" to enLinea,
            "ultimaSenal" to FieldValue.serverTimestamp(),
            "nivel" to nivel.coerceIn(-1, 100),
            "dispositivo" to dispositivo.take(40)
        )
        refDispensador(id).collection("estado").document("puente").set(datos)
            .addOnFailureListener { alError(mensajeDeError(it)) }
    }

    fun escucharHorarios(id: String, alCambio: (List<Horario>) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refDispensador(id).collection("horarios").addSnapshotListener { resultado, error ->
            if (error != null) {
                alError(mensajeDeError(error))
                return@addSnapshotListener
            }
            alCambio(resultado?.documents?.map { it.aHorario() } ?: listOf())
        }

    fun agregarHorario(id: String, hora: Int, minuto: Int, porciones: Int, alTerminar: (EstadoOperacion) -> Unit) {
        val datos = mapOf(
            "hora" to hora,
            "minuto" to minuto,
            "porciones" to porciones,
            "activo" to true,
            "ultimaEjecucion" to ""
        )
        refDispensador(id).collection("horarios").add(datos)
            .addOnSuccessListener { alTerminar(exito("Horario agregado.")) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun actualizarHorario(id: String, horarioId: String, cambios: Map<String, Any>, alTerminar: (EstadoOperacion) -> Unit) {
        refDispensador(id).collection("horarios").document(horarioId).update(cambios)
            .addOnSuccessListener { alTerminar(EstadoOperacion.Inactivo) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun borrarHorario(id: String, horarioId: String, alTerminar: (EstadoOperacion) -> Unit) {
        refDispensador(id).collection("horarios").document(horarioId).delete()
            .addOnSuccessListener { alTerminar(exito("Horario eliminado.")) }
            .addOnFailureListener { alTerminar(fallo(it)) }
    }

    fun marcarHorarioEjecutado(id: String, horarioId: String, hoy: String, alError: (String) -> Unit) {
        refDispensador(id).collection("horarios").document(horarioId).update("ultimaEjecucion", hoy)
            .addOnFailureListener { alError(mensajeDeError(it)) }
    }

    fun escucharHistorial(id: String, desde: Date, hasta: Date, alCambio: (List<EventoHistorial>) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refDispensador(id).collection("historial")
            .whereGreaterThanOrEqualTo("fecha", Timestamp(desde))
            .whereLessThan("fecha", Timestamp(hasta))
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { resultado, error ->
                if (error != null) {
                    alError(mensajeDeError(error))
                    return@addSnapshotListener
                }
                alCambio(resultado?.documents?.map { it.aEvento() } ?: listOf())
            }

    fun escucharOrdenes(id: String, desde: Date, hasta: Date, alCambio: (List<Comando>) -> Unit, alError: (String) -> Unit): ListenerRegistration =
        refDispensador(id).collection("comandos")
            .whereGreaterThanOrEqualTo("creadoEn", Timestamp(desde))
            .whereLessThan("creadoEn", Timestamp(hasta))
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { resultado, error ->
                if (error != null) {
                    alError(mensajeDeError(error))
                    return@addSnapshotListener
                }
                alCambio(resultado?.documents?.map { it.aComando() } ?: listOf())
            }

    fun registrarHistorial(id: String, porciones: Int, origen: String, comandoId: String, alError: (String) -> Unit) {
        val datos = mapOf(
            "porciones" to porciones,
            "origen" to origen,
            "fecha" to FieldValue.serverTimestamp(),
            "comandoId" to comandoId
        )
        refDispensador(id).collection("historial").add(datos)
            .addOnFailureListener { alError(mensajeDeError(it)) }
    }
}
