package cl.inacap.dispensapet.comunicacion

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import cl.inacap.dispensapet.modelos.EstadoOperacion
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class CanalBluetooth(
    private val dispositivo: BluetoothDevice,
    private val alRecibirLinea: (String) -> Unit,
    private val alDesconectarse: (String) -> Unit
) : CanalDispensador {
    override val descripcion: String = "Bluetooth"

    private val manejador = Handler(Looper.getMainLooper())

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var cerradoPorUsuario = false

    @SuppressLint("MissingPermission")
    fun conectar(alResultado: (EstadoOperacion) -> Unit) {
        Thread {
            try {
                val nuevo = dispositivo.createRfcommSocketToServiceRecord(UUID_PUERTO_SERIE)
                nuevo.connect()
                socket = nuevo
                manejador.post { alResultado(EstadoOperacion.Exito("Conectado")) }
                escuchar(nuevo)
            } catch (e: IOException) {
                manejador.post {
                    alResultado(EstadoOperacion.Error("No se encontró el dispensador. Revisa que esté encendido y a menos de 10 metros."))
                }
            } catch (e: SecurityException) {
                manejador.post { alResultado(EstadoOperacion.Error("Falta el permiso de Bluetooth.")) }
            }
        }.start()
    }

    private fun escuchar(conectado: BluetoothSocket) {
        try {
            val lector = BufferedReader(InputStreamReader(conectado.inputStream))
            while (true) {
                val linea = lector.readLine() ?: break
                if (linea.length <= 64) manejador.post { alRecibirLinea(linea) }
            }
        } catch (e: IOException) {
        }
        socket = null
        if (!cerradoPorUsuario) {
            manejador.post { alDesconectarse("Se perdió la conexión Bluetooth con el dispensador.") }
        }
    }

    override fun solicitarDispensa(porciones: Int, alResultado: (EstadoOperacion) -> Unit) {
        enviarTexto(ProtocoloArduino.COMANDO_DISPENSAR, alResultado)
    }

    fun pedirNivel() {
        enviarTexto(ProtocoloArduino.COMANDO_NIVEL) {}
    }

    private fun enviarTexto(texto: String, alResultado: (EstadoOperacion) -> Unit) {
        val actual = socket
        if (actual == null) {
            alResultado(EstadoOperacion.Error("El dispensador no está conectado."))
            return
        }
        try {
            actual.outputStream.write(texto.toByteArray())
            actual.outputStream.flush()
            alResultado(EstadoOperacion.Exito("Enviado"))
        } catch (e: IOException) {
            alResultado(EstadoOperacion.Error("No se pudo enviar la orden por Bluetooth."))
        }
    }

    override fun cerrar() {
        cerradoPorUsuario = true
        try {
            socket?.close()
        } catch (e: IOException) {
        }
        socket = null
    }

    companion object {
        private val UUID_PUERTO_SERIE: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
