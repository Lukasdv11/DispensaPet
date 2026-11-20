package cl.inacap.dispensapet.utilidades

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestoreException

fun mensajeDeError(error: Exception?): String = when (error) {
    is FirebaseNetworkException -> "Sin conexión a Internet. Revisa tu WiFi o datos móviles."
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos e intenta de nuevo."
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permiso para realizar esta acción."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "No se pudo conectar con el servidor. Revisa tu conexión."
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Tu sesión expiró. Vuelve a iniciar sesión."
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "El servidor tardó demasiado en responder. Intenta de nuevo."
        FirebaseFirestoreException.Code.NOT_FOUND -> "El dato solicitado ya no existe."
        else -> "Ocurrió un problema con el servidor. Intenta de nuevo."
    }
    else -> "Ocurrió un error inesperado. Intenta de nuevo."
}

fun esPermisoDenegado(error: Exception?): Boolean =
    error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
