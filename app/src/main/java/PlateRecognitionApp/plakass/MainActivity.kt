package PlateRecognitionApp.plakass

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import PlateRecognitionApp.plakass.utils.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        SessionManager.init(this)
    }
}

