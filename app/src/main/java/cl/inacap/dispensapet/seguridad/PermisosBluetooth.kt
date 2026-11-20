package cl.inacap.dispensapet.seguridad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermisosBluetooth {
    val permiso: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null

    fun concedido(contexto: Context): Boolean {
        val requerido = permiso ?: return true
        return ContextCompat.checkSelfPermission(contexto, requerido) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun emparejados(contexto: Context, adaptador: BluetoothAdapter?): List<BluetoothDevice> {
        if (!concedido(contexto) || adaptador == null) return listOf()
        return try {
            adaptador.bondedDevices?.toList() ?: listOf()
        } catch (e: SecurityException) {
            listOf()
        }
    }

    @SuppressLint("MissingPermission")
    fun nombreDe(dispositivo: BluetoothDevice): String = try {
        val nombre = dispositivo.name
        if (nombre.isNullOrBlank()) dispositivo.address else "$nombre (${dispositivo.address})"
    } catch (e: SecurityException) {
        dispositivo.address
    }
}
