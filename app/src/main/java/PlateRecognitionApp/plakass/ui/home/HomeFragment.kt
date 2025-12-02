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
        setupPagarListener()
        loadUserDataFromBackend()
        setupCardClickListeners()
        setupSocketListeners()
    }

    // -------------------------------------------------------
    //   🔥 CARGAR USUARIO REAL DESDE EL BACKEND
    // -------------------------------------------------------
    private fun loadUserDataFromBackend() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.retrofit.getUser()

                if (response.isSuccessful && response.body()?.status == true) {

                    val user = response.body()?.data
                    val role = user?.role ?: "user"

                    // PANEL ADMIN
                    binding.cardAdmin.visibility =
                        if (role == "admin") View.VISIBLE else View.GONE

                    val nombre = user?.name ?: "Usuario"
                    binding.tvUserName.text = nombre.split(" ")[0]

                    // Guardar en SharedPreferences
                    val prefs = requireContext().getSharedPreferences("user_prefs", 0).edit()
                    prefs.putString("user_name", nombre)
                    prefs.putString("role", role)
                    prefs.apply()

                    Log.d("HOME", "Usuario cargado: $nombre ($role)")

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
    //        SOCKET LISTENERS
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

        socket.on("stop_parking_status") { _ ->
            requireActivity().runOnUiThread {
                binding.tvEstadoActual.text = "Pago realizado"
                binding.tvTiempo.text = "00h 00m"
                binding.tvCosto.text = "$0.00"
                binding.tvFechaInicio.text = "--/--/----"
                binding.btnPagar.visibility = View.GONE
            }
        }

        socket.on("exit_parking") { _ ->
            requireActivity().runOnUiThread {
                binding.tvEstadoActual.text = "Vehículo ha salido"
                binding.tvTiempo.text = "00h 00m"
                binding.tvCosto.text = "$0.00"
                binding.tvFechaInicio.text = "--/--/----"
                binding.btnPagar.visibility = View.GONE
            }
        }
    }

    // -------------------------------------------------------
    //                  UI DE ESTADO
    // -------------------------------------------------------
    private fun updateStatusUI(json: JSONObject) {
        val isParked = json.optBoolean("is_parked", false)
        val status = json.optString("status", "NONE")

        // Fecha
        val entryTimeRaw = json.optString("entry_time", "")
        val fecha = if (entryTimeRaw.contains("T"))
            entryTimeRaw.substring(0, 10)
        else "--/--/----"

        binding.tvFechaInicio.text = fecha

        // Tiempo
        val totalMinutes = json.optDouble("elapsed_minutes", 0.0)
        val h = (totalMinutes / 60).toInt()
        val m = (totalMinutes % 60).toInt()
        binding.tvTiempo.text = if (isParked) "${h}h ${m}m" else "00h 00m"

        // Costo
        val cost = json.optDouble("current_cost", 0.0)
        binding.tvCosto.text = "$${String.format("%.2f", cost)}"

        // Estado
        binding.tvEstadoActual.text =
            when (status) {
                "IN_PROGRESS" -> "Estacionado"
                "PAID" -> "Pagado"
                "EXITED" -> "Finalizado"
                "NONE" -> "No estacionado"
                else -> "Desconocido"
            }

        binding.btnPagar.visibility =
            if (isParked && status == "IN_PROGRESS") View.VISIBLE else View.GONE
    }

    // -------------------------------------------------------
    //           LISTENER PARA EL BOTÓN DE PAGAR
    // -------------------------------------------------------
    private fun setupPagarListener() {
        binding.btnPagar.setOnClickListener {
            pagarEstacionamiento()
        }
    }

    private fun pagarEstacionamiento() {
        lifecycleScope.launch {
            try {
                // 1) Obtener estado del backend
                val statusRes = ApiClient.retrofit.parkingStatus()

                if (!statusRes.isSuccessful) return@launch

                val statusBody = statusRes.body() ?: return@launch
                val data = statusBody.data ?: return@launch

                // 2) Extraer el session_id
                val sessionId = data["session_id"] as? String ?: return@launch

                // 3) Construir body
                val body = hashMapOf<String, Any>(
                    "session_id" to sessionId
                )

                // 4) Ejecutar pago
                val res = ApiClient.retrofit.payParking(body)

                if (res.isSuccessful && res.body()?.status == true) {
                    binding.tvEstadoActual.text = "Pago realizado"
                    binding.btnPagar.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("HOME", "Error pagando: ${e.message}")
            }
        }
    }


    // -------------------------------------------------------
    //                 CLICK LISTENERS
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


        binding.cardAdmin.setOnClickListener {
            findNavController().navigate(R.id.adminPanelFragment)
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
