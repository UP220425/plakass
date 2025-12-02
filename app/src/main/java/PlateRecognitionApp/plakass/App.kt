package PlateRecognitionApp.plakass

import android.app.Application
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import android.util.Log
import PlateRecognitionApp.plakass.utils.SessionManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        Log.d("APP_INIT", "App.kt se ejecutó correctamente")

        SocketHandler.init("http://192.168.1.94:5000")
        SocketHandler.connect()
    }

}
