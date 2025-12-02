package PlateRecognitionApp.plakass.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import kotlinx.coroutines.launch

class NoAppPayFragment : Fragment() {

    private val args: NoAppPayFragmentArgs by navArgs()

    private lateinit var txtPlate: TextView
    private lateinit var txtPrice: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnPay: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_noapp_pay, container, false)

        txtPlate = view.findViewById(R.id.txtPlate)
        txtPrice = view.findViewById(R.id.txtPrice)
        txtStatus = view.findViewById(R.id.txtStatus)
        btnPay = view.findViewById(R.id.btnPay)

        txtPlate.text = "Placa: ${args.plate}"
        txtPrice.text = "Total estimado: $${args.price} MXN"

        btnPay.setOnClickListener { pagar() }

        return view
    }

    private fun pagar() {
        lifecycleScope.launch {
            txtStatus.text = "Procesando pago..."

            val body = hashMapOf<String, Any>(
                "session_id" to args.sessionId
            )

            val res = ApiClient.retrofit.payNoApp(body)

            if (res.isSuccessful && res.body()?.status == true) {
                txtStatus.text = "Pago realizado correctamente"
            } else {
                txtStatus.text = "Error al pagar"
            }
        }
    }
}
