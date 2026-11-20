package cl.inacap.dispensapet.comunicacion

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.Comando
import cl.inacap.dispensapet.modelos.Dispensador
import cl.inacap.dispensapet.modelos.EstadoConexion
import cl.inacap.dispensapet.modelos.EstadoOperacion
import cl.inacap.dispensapet.modelos.EstadosComando
import cl.inacap.dispensapet.modelos.Horario
import cl.inacap.dispensapet.modelos.MAX_PORCIONES
import cl.inacap.dispensapet.modelos.Origenes
import cl.inacap.dispensapet.seguridad.PermisosBluetooth
import cl.inacap.dispensapet.utilidades.hoyComoTexto
import cl.inacap.dispensapet.utilidades.textoHoraSegundos
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import java.util.Date

object GestorPuente {
    const val VIGENCIA_COMANDO_MS = 45_000L
    private const val ESPERA_CONFIRMACION_MS = 10_000L
    private const val PAUSA_ENTRE_PORCIONES_MS = 4_000L
    private const val LATIDO_MS = 20_000L
    private const val REVISION_HORARIOS_MS = 15_000L
    private const val RESPUESTA_SIMULADA_MS = 1_500L

    var estado by mutableStateOf<EstadoConexion>(EstadoConexion.Desconectado)
        private set
    var nivel by mutableStateOf(-1)
        private set
    var ultimoEvento by mutableStateOf("")
        private set
    var dispensadorActivo by mutableStateOf("")
        private set
    var simulado by mutableStateOf(false)
        private set

    val activo: Boolean
        get() = estado is EstadoConexion.Conectado || estado is EstadoConexion.Conectando

    private val manejador = Handler(Looper.getMainLooper())
    private var canal: CanalBluetooth? = null
    private var nombreDispositivo = ""
    private var configuracion = Dispensador()
    private var horarios = listOf<Horario>()
    private val escuchas = mutableListOf<ListenerRegistration>()
    private val cola = mutableListOf<Pendiente>()
    private val idsVistos = mutableSetOf<String>()
    private val horariosEjecutados = mutableSetOf<String>()
    private var trabajo: Trabajo? = null

    private data class Pendiente(val comandoId: String, val porciones: Int, val origen: String, val creadoEn: Date?)

    private class Trabajo(val pendiente: Pendiente) {
        var confirmadas = 0
        var enviando = false
    }

    private val tiempoAgotado = Runnable {
        finalizarTrabajo(EstadosComando.SIN_CONFIRMAR, "El Arduino no confirmó a tiempo; no se reintenta.")
    }

    fun iniciar(dispensadorId: String, dispositivo: BluetoothDevice) {
        if (activo) return
        dispensadorActivo = dispensadorId
        nombreDispositivo = PermisosBluetooth.nombreDe(dispositivo).take(40)
        simulado = false
        cola.clear()
        idsVistos.clear()
        horariosEjecutados.clear()
        trabajo = null
        estado = EstadoConexion.Conectando(nombreDispositivo)
        anotar("Conectando con $nombreDispositivo...")

        val nuevo = CanalBluetooth(
            dispositivo,
            alRecibirLinea = { linea -> procesarLinea(linea) },
            alDesconectarse = { motivo -> perderConexion(motivo) }
        )
        canal = nuevo
        nuevo.conectar { resultado ->
            if (canal !== nuevo) return@conectar
            when (resultado) {
                is EstadoOperacion.Exito -> alConectar()
                is EstadoOperacion.Error -> {
                    canal = null
                    estado = EstadoConexion.Error(resultado.mensaje)
                    anotar(resultado.mensaje)
                }
                else -> {}
            }
        }
    }

    fun iniciarSimulacion(dispensadorId: String) {
        if (activo) return
        dispensadorActivo = dispensadorId
        nombreDispositivo = "Dispensador simulado"
        simulado = true
        canal = null
        cola.clear()
        idsVistos.clear()
        horariosEjecutados.clear()
        trabajo = null
        nivel = 100
        alConectar()
        anotar("Simulación iniciada: este teléfono responde como el Arduino.")
    }

    private fun alConectar() {
        estado = EstadoConexion.Conectado(nombreDispositivo)
        anotar("Conectado. Esperando órdenes.")
        publicarEstado(true)
        programarLatido()
        programarRevisionHorarios()
        canal?.pedirNivel()

        escuchas.add(ServicioDatos.escucharDispensador(dispensadorActivo,
            alCambio = { d -> configuracion = d },
            alError = { mensaje -> anotar("Configuración: $mensaje") }
        ))
        escuchas.add(ServicioDatos.escucharHorarios(dispensadorActivo,
            alCambio = { lista -> horarios = lista },
            alError = { mensaje -> anotar("Horarios: $mensaje") }
        ))
        escuchas.add(ServicioDatos.escucharComandosPendientes(dispensadorActivo,
            alCambio = { lista -> recibirComandos(lista) },
            alError = { mensaje -> anotar("Órdenes: $mensaje") }
        ))
    }

    fun detener() {
        if (estado is EstadoConexion.Desconectado) return
        val anterior = canal
        canal = null
        estado = EstadoConexion.Desconectado
        finalizarTrabajo(EstadosComando.SIN_CONFIRMAR, "El puente se detuvo antes de la confirmación.")
        anterior?.cerrar()
        limpiar()
        simulado = false
        anotar("Puente detenido.")
    }

    private fun perderConexion(motivo: String) {
        canal = null
        estado = EstadoConexion.Error(motivo)
        anotar(motivo)
        finalizarTrabajo(EstadosComando.SIN_CONFIRMAR, "Se cortó el Bluetooth en medio de la orden.")
        limpiar()
    }

    private fun limpiar() {
        for (escucha in escuchas) escucha.remove()
        escuchas.clear()
        manejador.removeCallbacksAndMessages(null)
        cola.clear()
        if (dispensadorActivo.isNotEmpty()) publicarEstado(false)
    }

    private fun recibirComandos(lista: List<Comando>) {
        val ordenados = lista.sortedBy { it.creadoEn?.time ?: Long.MAX_VALUE }
        for (comando in ordenados) {
            if (comando.id !in idsVistos) {
                idsVistos.add(comando.id)
                cola.add(Pendiente(comando.id, comando.porciones, Origenes.APP, comando.creadoEn))
            }
        }
        procesarSiguiente()
    }

    private fun procesarSiguiente() {
        if (trabajo != null || estado !is EstadoConexion.Conectado || cola.isEmpty()) return
        val siguiente = cola.removeAt(0)

        if (siguiente.comandoId.isNotEmpty()) {
            val creado = siguiente.creadoEn
            val edad = if (creado == null) Long.MAX_VALUE else System.currentTimeMillis() - creado.time
            if (edad > VIGENCIA_COMANDO_MS) {
                cerrarComando(siguiente.comandoId, EstadosComando.VENCIDO, "Llegó con más de 45 s de atraso; no se dispensó.")
                anotar("Orden vencida descartada.")
                procesarSiguiente()
                return
            }
        }

        if (siguiente.porciones !in 1..MAX_PORCIONES) {
            if (siguiente.comandoId.isNotEmpty()) {
                cerrarComando(siguiente.comandoId, EstadosComando.RECHAZADO, "Cantidad de porciones no permitida.")
            }
            anotar("Orden rechazada: cantidad de porciones no permitida.")
            procesarSiguiente()
            return
        }

        val nuevo = Trabajo(siguiente)
        trabajo = nuevo
        if (siguiente.comandoId.isEmpty()) {
            enviarPorcion(nuevo)
            return
        }

        ServicioDatos.reclamarComando(dispensadorActivo, siguiente.comandoId) { reclamado ->
            if (trabajo !== nuevo) return@reclamarComando
            if (reclamado) {
                enviarPorcion(nuevo)
            } else {
                trabajo = null
                anotar("La orden ya había sido procesada; se ignora.")
                procesarSiguiente()
            }
        }
    }

    private fun enviarPorcion(actual: Trabajo) {
        if (trabajo !== actual) return
        if (simulado) {
            actual.enviando = true
            anotar("Dispensando porción ${actual.confirmadas + 1} de ${actual.pendiente.porciones} (simulado)...")
            manejador.postDelayed(tiempoAgotado, ESPERA_CONFIRMACION_MS)
            manejador.postDelayed({ responderSimulado(actual) }, RESPUESTA_SIMULADA_MS)
            return
        }
        val conexion = canal
        if (conexion == null) {
            finalizarTrabajo(EstadosComando.SIN_CONFIRMAR, "El dispensador no está conectado.")
            return
        }
        actual.enviando = true
        anotar("Dispensando porción ${actual.confirmadas + 1} de ${actual.pendiente.porciones}...")
        manejador.postDelayed(tiempoAgotado, ESPERA_CONFIRMACION_MS)
        conexion.solicitarDispensa(1) { resultado ->
            if (resultado is EstadoOperacion.Error) finalizarTrabajo(EstadosComando.SIN_CONFIRMAR, resultado.mensaje)
        }
    }

    private fun responderSimulado(actual: Trabajo) {
        if (!simulado || trabajo !== actual) return
        nivel = (nivel - 5).coerceAtLeast(0)
        procesarLinea("ENTREGA:APP")
        procesarLinea("NIVEL:$nivel")
    }

    private fun procesarLinea(linea: String) {
        when (val respuesta = ProtocoloArduino.interpretar(linea)) {
            is RespuestaArduino.Nivel -> {
                nivel = respuesta.porcentaje
                publicarEstado(true)
            }
            is RespuestaArduino.Entrega -> alConfirmarEntrega(respuesta.origen)
            is RespuestaArduino.Rechazo -> {
                anotar("El Arduino rechazó la orden (${respuesta.motivo}).")
                if (trabajo?.enviando == true) {
                    finalizarTrabajo(EstadosComando.RECHAZADO, "El dispensador rechazó la orden (${respuesta.motivo}).")
                }
            }
            is RespuestaArduino.Listo -> anotar("Arduino listo.")
            is RespuestaArduino.Desconocida -> anotar("Respuesta no reconocida, se ignora: ${respuesta.texto.take(20)}")
        }
    }

    private fun alConfirmarEntrega(origen: String) {
        val actual = trabajo
        if (origen == "APP" && actual != null && actual.enviando) {
            manejador.removeCallbacks(tiempoAgotado)
            actual.enviando = false
            actual.confirmadas = actual.confirmadas + 1
            if (actual.confirmadas < actual.pendiente.porciones) {
                manejador.postDelayed({ enviarPorcion(actual) }, PAUSA_ENTRE_PORCIONES_MS)
            } else {
                finalizarTrabajo(EstadosComando.CONFIRMADO, "Confirmado por el Arduino: ${actual.confirmadas} porción(es).")
            }
            return
        }
        val origenReal = if (origen == "APP") Origenes.APP else Origenes.BOTON
        ServicioDatos.registrarHistorial(dispensadorActivo, 1, origenReal, "") { m -> anotar("Historial: $m") }
        anotar(if (origenReal == Origenes.BOTON) "Porción entregada con el botón físico." else "Entrega sin orden pendiente (registrada).")
    }

    private fun finalizarTrabajo(estadoFinal: String, detalle: String) {
        val actual = trabajo ?: return
        manejador.removeCallbacks(tiempoAgotado)
        trabajo = null
        val pendiente = actual.pendiente
        if (actual.confirmadas > 0) {
            ServicioDatos.registrarHistorial(dispensadorActivo, actual.confirmadas, pendiente.origen, pendiente.comandoId) { m ->
                anotar("Historial: $m")
            }
        }
        val detalleFinal =
            if (estadoFinal != EstadosComando.CONFIRMADO && actual.confirmadas > 0) "$detalle (salieron ${actual.confirmadas} de ${pendiente.porciones})"
            else detalle
        if (pendiente.comandoId.isNotEmpty()) cerrarComando(pendiente.comandoId, estadoFinal, detalleFinal)
        anotar(detalleFinal)
        procesarSiguiente()
    }

    private fun cerrarComando(comandoId: String, estadoFinal: String, detalle: String) {
        ServicioDatos.actualizarComando(dispensadorActivo, comandoId, estadoFinal, detalle) { m -> anotar("Orden: $m") }
    }

    private fun revisarHorarios() {
        if (!configuracion.modoAutomatico) return
        val ahora = Calendar.getInstance()
        val hora = ahora.get(Calendar.HOUR_OF_DAY)
        val minuto = ahora.get(Calendar.MINUTE)
        val hoy = hoyComoTexto()
        for (horario in horarios) {
            val clave = "${horario.id}-$hoy"
            val tocaAhora = horario.activo && horario.hora == hora && horario.minuto == minuto
            if (tocaAhora && horario.ultimaEjecucion != hoy && clave !in horariosEjecutados) {
                horariosEjecutados.add(clave)
                ServicioDatos.marcarHorarioEjecutado(dispensadorActivo, horario.id, hoy) { m -> anotar("Horario: $m") }
                anotar("Horario ${horario.textoHora}: ${horario.porciones} porción(es).")
                cola.add(Pendiente("", horario.porciones, Origenes.HORARIO, null))
            }
        }
        procesarSiguiente()
    }

    private fun programarLatido() {
        manejador.postDelayed({
            if (estado is EstadoConexion.Conectado) {
                publicarEstado(true)
                programarLatido()
            }
        }, LATIDO_MS)
    }

    private fun programarRevisionHorarios() {
        manejador.postDelayed({
            if (estado is EstadoConexion.Conectado) {
                revisarHorarios()
                programarRevisionHorarios()
            }
        }, REVISION_HORARIOS_MS)
    }

    private fun publicarEstado(enLinea: Boolean) {
        ServicioDatos.publicarEstadoPuente(dispensadorActivo, enLinea, nivel, nombreDispositivo) { m -> anotar("Estado: $m") }
    }

    private fun anotar(texto: String) {
        ultimoEvento = "${Date().textoHoraSegundos()}  $texto"
    }
}
