package PlateRecognitionApp.plakass.ui.admin

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import kotlinx.coroutines.launch

class AdminQRFragment : Fragment() {

    private lateinit var imgQR: ImageView
    private lateinit var txtTimer: TextView
    private lateinit var txtStatus: TextView

    private var timer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_qr, container, false)

        imgQR = view.findViewById(R.id.imgQR)
        txtTimer = view.findViewById(R.id.txtTimer)
        txtStatus = view.findViewById(R.id.txtQRStatus)

        requestQR()

        return view
    }

    private fun requestQR() {
        lifecycleScope.launch {

            txtStatus.text = "Generando QR..."

            val res = ApiClient.retrofit.getEntryQR()

            if (res.isSuccessful && res.body() != null) {

                val qrData = res.body()!!.data
                val base64QR = qrData.qr
                val expiresIn = qrData.expires_in

                // Decodificar imagen
                val bytes = Base64.decode(base64QR, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgQR.setImageBitmap(bmp)

                // Timer del QR dinámico
                startTimer(expiresIn)

                txtStatus.text = "QR vigente"

            } else {
                txtStatus.text = "Error generando QR"
            }
        }
    }

    private fun startTimer(seconds: Int) {
        timer?.cancel()

        timer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(ms: Long) {
                txtTimer.text = (ms / 1000).toString()
            }

            override fun onFinish() {
                requestQR()   // generar QR nuevo
            }
        }.start()
    }

    override fun onDestroyView() {
        timer?.cancel()
        super.onDestroyView()
    }
}
