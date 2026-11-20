package cl.inacap.dispensapet.comunicacion

import cl.inacap.dispensapet.modelos.EstadoOperacion

interface CanalDispensador {
    val descripcion: String
    fun solicitarDispensa(porciones: Int, alResultado: (EstadoOperacion) -> Unit)
    fun cerrar()
}
