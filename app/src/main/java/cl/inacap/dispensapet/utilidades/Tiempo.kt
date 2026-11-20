package cl.inacap.dispensapet.utilidades

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun inicioDelDia(fecha: Date): Date {
    val calendario = Calendar.getInstance()
    calendario.time = fecha
    calendario.set(Calendar.HOUR_OF_DAY, 0)
    calendario.set(Calendar.MINUTE, 0)
    calendario.set(Calendar.SECOND, 0)
    calendario.set(Calendar.MILLISECOND, 0)
    return calendario.time
}

fun inicioDelDia(anio: Int, mes: Int, dia: Int): Date {
    val calendario = Calendar.getInstance()
    calendario.set(anio, mes, dia, 0, 0, 0)
    calendario.set(Calendar.MILLISECOND, 0)
    return calendario.time
}

fun diaSiguiente(fecha: Date): Date {
    val calendario = Calendar.getInstance()
    calendario.time = fecha
    calendario.add(Calendar.DAY_OF_MONTH, 1)
    return calendario.time
}

fun diaAnterior(fecha: Date): Date {
    val calendario = Calendar.getInstance()
    calendario.time = fecha
    calendario.add(Calendar.DAY_OF_MONTH, -1)
    return calendario.time
}

fun hoyComoTexto(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun Date.textoFecha(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(this)

fun Date.textoHora(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(this)

fun Date.textoHoraSegundos(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(this)
