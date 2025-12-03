package PlateRecognitionApp.plakass

import android.app.Application
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import android.util.Log
import PlateRecognitionApp.plakass.utils.SessionManager
import PlateRecognitionApp.plakass.utils.NetworkMonitor   // ← AGREGADO

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar sesión
        SessionManager.init(this)

        // 🔥 Inicializar monitor de red (offline/online)
        NetworkMonitor.start(this)

        Log.d("APP_INIT", "App.kt se ejecutó correctamente")

        // Inicializar socket
        SocketHandler.init("https://plakass-api-amhuasfpadhwdvfm.canadacentral-01.azurewebsites.net")
        SocketHandler.connect()
    }

}
