package cl.inacap.dispensapet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cl.inacap.dispensapet.seguridad.ControlInactividad
import cl.inacap.dispensapet.ui.theme.TemaDispensaPet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarFirestore()
        setContent {
            TemaDispensaPet {
                NavegacionApp()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        ControlInactividad.registrarActividad()
    }

    private fun configurarFirestore() {
        try {
            val ajustes = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = ajustes
        } catch (e: IllegalStateException) {
        }
    }
}
