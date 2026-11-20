package cl.inacap.dispensapet.datos

import cl.inacap.dispensapet.modelos.EstadoOperacion
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

sealed class ResultadoAcceso {
    data class Correcto(val verificado: Boolean) : ResultadoAcceso()
    object Incorrecto : ResultadoAcceso()
    object SinConexion : ResultadoAcceso()
    object BloqueoServidor : ResultadoAcceso()
}

object ServicioAuth {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    fun nombreVisible(): String {
        val usuario = auth.currentUser ?: return ""
        val nombre = usuario.displayName
        return if (!nombre.isNullOrBlank()) nombre else (usuario.email ?: "").substringBefore("@")
    }

    fun iniciarSesion(correo: String, contrasena: String, alTerminar: (ResultadoAcceso) -> Unit) {
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { resultado ->
                alTerminar(ResultadoAcceso.Correcto(resultado.user?.isEmailVerified == true))
            }
            .addOnFailureListener { error ->
                val resultado = when (error) {
                    is FirebaseNetworkException -> ResultadoAcceso.SinConexion
                    is FirebaseTooManyRequestsException -> ResultadoAcceso.BloqueoServidor
                    else -> ResultadoAcceso.Incorrecto
                }
                alTerminar(resultado)
            }
    }

    fun registrar(nombre: String, correo: String, contrasena: String, alTerminar: (EstadoOperacion) -> Unit) {
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user
                if (usuario == null) {
                    alTerminar(EstadoOperacion.Error("No se pudo crear la cuenta. Intenta de nuevo."))
                    return@addOnSuccessListener
                }
                val perfil = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
                usuario.updateProfile(perfil).addOnCompleteListener {
                    usuario.sendEmailVerification().addOnCompleteListener { envio ->
                        if (envio.isSuccessful) {
                            alTerminar(EstadoOperacion.Exito("Cuenta creada. Revisa tu correo para verificarla."))
                        } else {
                            alTerminar(EstadoOperacion.Exito("Cuenta creada. No se pudo enviar el correo de verificación; puedes reenviarlo."))
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                val mensaje = when (error) {
                    is FirebaseAuthUserCollisionException ->
                        "No se pudo crear la cuenta con ese correo. Si ya tienes una, inicia sesión o recupera tu contraseña."
                    is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos de seguridad."
                    is FirebaseNetworkException -> "Sin conexión a Internet."
                    else -> "No se pudo crear la cuenta. Intenta de nuevo."
                }
                alTerminar(EstadoOperacion.Error(mensaje))
            }
    }

    fun reenviarVerificacion(alTerminar: (EstadoOperacion) -> Unit) {
        val usuario = auth.currentUser
        if (usuario == null) {
            alTerminar(EstadoOperacion.Error("Tu sesión expiró. Vuelve a iniciar sesión."))
            return
        }
        usuario.sendEmailVerification()
            .addOnSuccessListener { alTerminar(EstadoOperacion.Exito("Correo de verificación reenviado.")) }
            .addOnFailureListener { alTerminar(EstadoOperacion.Error("No se pudo reenviar. Espera un momento e intenta de nuevo.")) }
    }

    fun comprobarVerificacion(alTerminar: (Boolean, String) -> Unit) {
        val usuario = auth.currentUser
        if (usuario == null) {
            alTerminar(false, "Tu sesión expiró. Vuelve a iniciar sesión.")
            return
        }
        usuario.reload()
            .addOnSuccessListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    usuario.getIdToken(true).addOnCompleteListener { alTerminar(true, "") }
                } else {
                    alTerminar(false, "Aún no aparece verificado. Abre el enlace del correo e intenta de nuevo.")
                }
            }
            .addOnFailureListener { error ->
                if (error is FirebaseNetworkException) alTerminar(false, "Sin conexión a Internet.")
                else alTerminar(false, "No se pudo comprobar. Intenta de nuevo.")
            }
    }

    fun recuperarContrasena(correo: String, alTerminar: (EstadoOperacion) -> Unit) {
        auth.sendPasswordResetEmail(correo).addOnCompleteListener { tarea ->
            if (tarea.exception is FirebaseNetworkException) {
                alTerminar(EstadoOperacion.Error("Sin conexión a Internet."))
            } else {
                alTerminar(EstadoOperacion.Exito("Si el correo está registrado, te llegará un enlace para crear una nueva contraseña."))
            }
        }
    }

    fun validarSesion(alTerminar: (Boolean) -> Unit) {
        val usuario = auth.currentUser
        if (usuario == null) {
            alTerminar(false)
            return
        }
        usuario.reload()
            .addOnSuccessListener {
                usuario.getIdToken(true).addOnCompleteListener { alTerminar(true) }
            }
            .addOnFailureListener { error -> alTerminar(error !is FirebaseAuthInvalidUserException) }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}
