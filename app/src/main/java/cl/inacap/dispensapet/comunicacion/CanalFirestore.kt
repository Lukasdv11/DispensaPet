package cl.inacap.dispensapet.comunicacion

import cl.inacap.dispensapet.datos.ServicioDatos
import cl.inacap.dispensapet.modelos.EstadoOperacion

class CanalFirestore(
    private val dispensadorId: String,
    private val uid: String
) : CanalDispensador {
    override val descripcion: String = "Internet (Firebase)"

    override fun solicitarDispensa(porciones: Int, alResultado: (EstadoOperacion) -> Unit) {
        alResultado(EstadoOperacion.Cargando)
        ServicioDatos.crearComando(dispensadorId, uid, porciones, alResultado)
    }

    override fun cerrar() {}
}
