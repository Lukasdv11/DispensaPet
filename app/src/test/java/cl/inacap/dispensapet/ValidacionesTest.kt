package cl.inacap.dispensapet

import cl.inacap.dispensapet.comunicacion.ProtocoloArduino
import cl.inacap.dispensapet.comunicacion.RespuestaArduino
import cl.inacap.dispensapet.modelos.EstadoPuenteRemoto
import cl.inacap.dispensapet.utilidades.esCorreoValido
import cl.inacap.dispensapet.utilidades.nivelFortaleza
import cl.inacap.dispensapet.utilidades.normalizarCorreo
import cl.inacap.dispensapet.utilidades.problemasContrasena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ValidacionesTest {
    @Test
    fun correosValidosEInvalidos() {
        assertTrue("ana.perez@gmail.com".esCorreoValido())
        assertTrue("  ana@inacap.cl  ".esCorreoValido())
        assertFalse("ana@".esCorreoValido())
        assertFalse("ana@gmail".esCorreoValido())
        assertFalse("ana..perez@gmail.com".esCorreoValido())
        assertFalse("ana perez@gmail.com".esCorreoValido())
        assertEquals("ana@gmail.com", "  Ana@Gmail.com ".normalizarCorreo())
    }

    @Test
    fun contrasenaSeguraSeAcepta() {
        assertTrue("Tolva#2026xy".problemasContrasena("ana@gmail.com", "Ana").isEmpty())
        assertEquals(4, "Tolva#2026xy".nivelFortaleza())
    }

    @Test
    fun contrasenaDebilSeRechaza() {
        assertFalse("corta1!A".problemasContrasena("ana@gmail.com", "Ana").isEmpty())
        assertFalse("sinmayuscula1!".problemasContrasena("ana@gmail.com", "Ana").isEmpty())
        assertFalse("SinSimbolo1234".problemasContrasena("ana@gmail.com", "Ana").isEmpty())
        assertFalse("Con Espacio1!x".problemasContrasena("ana@gmail.com", "Ana").isEmpty())
        assertFalse("Password123!".problemasContrasena("x@gmail.com", "Luis").isEmpty())
    }

    @Test
    fun contrasenaNoPuedeContenerCorreoNiNombre() {
        assertTrue("Carolina#2026".problemasContrasena("carolina@gmail.com", "Pedro").contains("No debe contener tu correo"))
        assertTrue("Pedrito#2026x".problemasContrasena("ana@gmail.com", "Pedrito").contains("No debe contener tu nombre"))
    }

    @Test
    fun puenteEnLineaSoloConSenalReciente() {
        val ahora = 1_000_000L
        assertTrue(EstadoPuenteRemoto(enLinea = true, ultimaSenal = Date(ahora - 30_000)).estaEnLinea(ahora))
        assertFalse(EstadoPuenteRemoto(enLinea = true, ultimaSenal = Date(ahora - 90_000)).estaEnLinea(ahora))
        assertFalse(EstadoPuenteRemoto(enLinea = false, ultimaSenal = Date(ahora)).estaEnLinea(ahora))
        assertFalse(EstadoPuenteRemoto(enLinea = true, ultimaSenal = null).estaEnLinea(ahora))
    }

    @Test
    fun protocoloArduinoInterpretaRespuestas() {
        assertEquals(RespuestaArduino.Nivel(75), ProtocoloArduino.interpretar("NIVEL:75\r"))
        assertEquals(RespuestaArduino.Entrega("APP"), ProtocoloArduino.interpretar("ENTREGA:APP"))
        assertEquals(RespuestaArduino.Entrega("BOTON"), ProtocoloArduino.interpretar("ENTREGA:BOTON"))
        assertEquals(RespuestaArduino.Rechazo("INTERVALO"), ProtocoloArduino.interpretar("RECHAZO:INTERVALO"))
        assertEquals(RespuestaArduino.Listo, ProtocoloArduino.interpretar("LISTO"))
    }

    @Test
    fun protocoloArduinoRechazaRespuestasCorruptas() {
        assertTrue(ProtocoloArduino.interpretar("NIVEL:abc") is RespuestaArduino.Desconocida)
        assertTrue(ProtocoloArduino.interpretar("NIVEL:250") is RespuestaArduino.Desconocida)
        assertTrue(ProtocoloArduino.interpretar("ENTREGA:XYZ") is RespuestaArduino.Desconocida)
        assertTrue(ProtocoloArduino.interpretar("%%basura%%") is RespuestaArduino.Desconocida)
    }
}
