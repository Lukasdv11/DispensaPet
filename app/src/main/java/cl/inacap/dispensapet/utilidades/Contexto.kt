package cl.inacap.dispensapet.utilidades

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun Context.hayInternet(): Boolean {
    val administrador = getSystemService(ConnectivityManager::class.java) ?: return false
    val red = administrador.activeNetwork ?: return false
    val capacidades = administrador.getNetworkCapabilities(red) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
