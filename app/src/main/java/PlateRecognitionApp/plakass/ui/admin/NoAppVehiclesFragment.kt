package PlateRecognitionApp.plakass.ui.admin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.data.model.PendingVehicle
import kotlinx.coroutines.launch

class NoAppVehiclesFragment : Fragment() {

    private lateinit var listView: ListView
    private var pendingList = listOf<PendingVehicle>()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadData()
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_noapp_vehicles, container, false)
        listView = view.findViewById(R.id.listNoApp)

        loadData()
        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val res = ApiClient.retrofit.getPendingNoApp()

            if (!res.isSuccessful || res.body() == null) return@launch

            pendingList = res.body()!!.data

            val adapter = VehicleNoAppAdapter(requireContext(), pendingList)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val item = pendingList[position]

                val action =
                    NoAppVehiclesFragmentDirections
                        .actionNoAppVehiclesFragmentToNoAppPayFragment(
                            sessionId = item.id,
                            plate = item.plate,
                            price = item.current_price.toFloat()
                        )

                findNavController().navigate(action)
            }
        }
    }
}
