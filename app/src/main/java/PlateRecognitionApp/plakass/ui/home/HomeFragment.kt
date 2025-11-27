package PlateRecognitionApp.plakass.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import PlateRecognitionApp.plakass.databinding.FragmentHomeBinding
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import io.socket.client.Socket
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var socket: Socket
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HOME", "HomeFragment cargado")

        setupClickListeners()
        loadUserDataFromBackend()   // ← USAMOS LA OPCIÓN 2 AQUÍ
        setupCardClickListeners()
        setupSocketListeners()
    }

    // -------------------------------------------------------
    //   🔥 OPCIÓN 2 — CARGAR USUARIO REAL DESDE BACKEND
    // -------------------------------------------------------
    private fun loadUserDataFromBackend() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.retrofit.getUser()

                if (response.isSuccessful && response.body()?.status == true) {

                    val user = response.body()?.data
                    val nombre = user?.name ?: "Usuario"

                    // Mostrar en pantalla
                    binding.tvUserName.text = nombre.split(" ")[0]

                    // Guardar en SharedPreferences (refresco global)
                    val prefs = requireContext().getSharedPreferences("user_prefs", 0).edit()
                    prefs.putString("user_name", nombre)
                    prefs.apply()

                    Log.d("HOME", "Nombre cargado desde backend: $nombre")

                } else {
                    binding.tvUserName.text = "Usuario"
                    Log.e("HOME", "Error backend: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e("HOME", "Error al cargar usuario: ${e.message}")
                binding.tvUserName.text = "Usuario"
            }
        }
    }

    // -------------------------------------------------------
    //        SOCKET LISTENERS (CORREGIDOS)
    // -------------------------------------------------------
    private fun setupSocketListeners() {
        socket = SocketHandler.getSocket()

        socket.off("joined")
        socket.off("parking_status")
        socket.off("stop_parking_status")
        socket.off("exit_parking")
        socket.off(Socket.EVENT_CONNECT)
        socket.off(Socket.EVENT_CONNECT_ERROR)

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET", "HomeFragment: CONNECT ✔")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SOCKET", "CONNECT_ERROR → ${args.joinToString()}")
        }

        socket.on("joined") { args ->
            requireActivity().runOnUiThread {
                val msg = (args[0] as JSONObject).getString("message")
                binding.tvEstadoActual.text = msg
            }
        }

        socket.on("parking_status") { args ->
            requireActivity().runOnUiThread {
                val json = args[0] as JSONObject
                updateStatusUI(json)
            }
        }

        socket.on("stop_parking_status") { args ->
            requireActivity().runOnUiThread {
                binding.tvEstadoActual.text = "Pago realizado"
                binding.tvTiempo.text = "00h 00m"
                binding.tvCosto.text = "$0.00"
                binding.tvFechaInicio.text = "--/--/----"
            }
        }

        socket.on("exit_parking") { args ->
            requireActivity().runOnUiThread {
                binding.tvEstadoActual.text = "Vehículo ha salido"
                binding.tvTiempo.text = "00h 00m"
                binding.tvCosto.text = "$0.00"
                binding.tvFechaInicio.text = "--/--/----"
            }
        }
    }

    // -------------------------------------------------------
    //                  UI CORREGIDA
    // -------------------------------------------------------
    private fun updateStatusUI(json: JSONObject) {
        val isParked = json.optBoolean("is_parked", false)
        val status = json.optString("status", "NONE")

        val entryTimeRaw = json.optString("entry_time", "")
        val fecha = if (entryTimeRaw.contains("T")) entryTimeRaw.substring(0, 10) else "--/--/----"
        binding.tvFechaInicio.text = fecha

        val totalMinutes = json.optDouble("elapsed_minutes", 0.0)
        val h = (totalMinutes / 60).toInt()
        val m = (totalMinutes % 60).toInt()
        binding.tvTiempo.text = if (isParked) "${h}h ${m}m" else "00h 00m"

        val cost = json.optDouble("current_cost", 0.0)
        binding.tvCosto.text = "$${String.format("%.2f", cost)}"

        binding.tvEstadoActual.text =
            when (status) {
                "IN_PROGRESS" -> "Estacionado"
                "PAID" -> "Pagado"
                "EXITED" -> "Finalizado"
                "NONE" -> "No estacionado"
                else -> "Desconocido"
            }
    }

    // -------------------------------------------------------
    //                 CONFIG GENERAL
    // -------------------------------------------------------
    private fun setupClickListeners() {
        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_userSettingsFragment)
        }
    }

    private fun setupCardClickListeners() {
        binding.cardEscanear.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scannerFragment)
        }

        binding.cardHistorial.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }

        binding.cardVehiculos.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_vehiclesFragment)
        }

        binding.cardConfiguracion.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_userSettingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        socket.off("joined")
        socket.off("parking_status")
        socket.off("stop_parking_status")
        socket.off("exit_parking")
        socket.off(Socket.EVENT_CONNECT)
        socket.off(Socket.EVENT_CONNECT_ERROR)

        _binding = null
    }
}
