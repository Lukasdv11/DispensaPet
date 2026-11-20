package cl.inacap.dispensapet.utilidades

private val CONTRASENAS_COMUNES = setOf(
    "password123!", "password1234!", "p@ssw0rd123", "qwerty123!", "qwerty12345!",
    "admin12345!", "welcome123!", "contraseña1!", "contrasena1!", "contrasena123!",
    "abc123456789!", "iloveyou123!", "chile2025!", "chile2026!", "mascota123!",
    "perro12345!", "gato123456!", "inacap2026!", "123456789aa!", "holamundo1!"
)

fun String.esCorreoValido(): Boolean {
    val correo = trim()
    val patron = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return patron.matches(correo) && !correo.contains("..")
}

fun String.normalizarCorreo(): String = trim().lowercase()

fun String.esNombreValido(): Boolean = trim().length in 2..40

fun String.problemasContrasena(correo: String, nombre: String): List<String> {
    val problemas = mutableListOf<String>()
    if (length < 10) problemas.add("Mínimo 10 caracteres")
    if (none { it.isUpperCase() }) problemas.add("Al menos una mayúscula")
    if (none { it.isLowerCase() }) problemas.add("Al menos una minúscula")
    if (none { it.isDigit() }) problemas.add("Al menos un número")
    if (none { !it.isLetterOrDigit() && !it.isWhitespace() }) problemas.add("Al menos un símbolo (por ejemplo ! # \$ %)")
    if (any { it.isWhitespace() }) problemas.add("Sin espacios")

    val texto = lowercase()
    val usuarioCorreo = correo.trim().lowercase().substringBefore("@")
    if (usuarioCorreo.length >= 3 && texto.contains(usuarioCorreo)) problemas.add("No debe contener tu correo")
    val nombreLimpio = nombre.trim().lowercase()
    if (nombreLimpio.length >= 3 && texto.contains(nombreLimpio)) problemas.add("No debe contener tu nombre")
    if (texto in CONTRASENAS_COMUNES) problemas.add("Es una contraseña demasiado común")
    return problemas
}

fun String.nivelFortaleza(): Int {
    if (isEmpty()) return 0
    var puntos = 0
    if (length >= 10) puntos++
    if (any { it.isUpperCase() } && any { it.isLowerCase() }) puntos++
    if (any { it.isDigit() }) puntos++
    if (any { !it.isLetterOrDigit() && !it.isWhitespace() }) puntos++
    if (length < 10 && puntos > 2) puntos = 2
    if (lowercase() in CONTRASENAS_COMUNES) puntos = 1
    return puntos
}
