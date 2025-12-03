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
import PlateRecognitionApp.plakass.data.local.entities.ActiveSessionEntity
import PlateRecognitionApp.plakass.data.local.repository.ParkingLocalRepository
import PlateRecognitionApp.plakass.utils.NetworkUtils
import PlateRecognitionApp.plakass.utils.NetworkMonitor
import PlateRecognitionApp.plakass.data.socket.SocketHandler
import com.bumptech.glide.Glide
import io.socket.client.Socket
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var socket: Socket
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var localRepo: ParkingLocalRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HOME", "onCreateView → INICIANDO")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        localRepo = ParkingLocalRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("HOME", "onViewCreated → Vista OK")
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupPagarListener()
        setupCardClickListeners()
        setupSocketListeners()

        // ---------------------------
        // 🔥 MONITOR DE INTERNET
        // ---------------------------
        lifecycleScope.launchWhenStarted {
            NetworkMonitor.isConnected.collectLatest { connected ->
                Log.d("NET", "Conexión cambió → $connected")

                if (connected) {
                    Log.d("NET", "Internet restaurado → sincronizando backend...")
                    safely { loadUserDataFromBackend() }
                    safely { syncOnlineStatus() }
                } else {
                    Log.w("NET", "Modo offline detectado → cargando usuario y sesión local")
                    safely { loadUserOffline() }
                    safelySuspend { loadOfflineSession() }
                }
            }
        }

        // ---------------------------
        // 🔥 REVISAR INTERNET AL ABRIR
        // ---------------------------
        lifecycleScope.launch {
            val connected = NetworkUtils.isConnected(requireContext())
            Log.d("HOME", "Internet inicial: $connected")

            if (!connected) {
                safely { loadUserOffline() }
                safelySuspend { loadOfflineSession() }
            } else {
                safely { loadUserDataFromBackend() }
            }
        }
    }

    // ================================================================
    // 🔵 GUARDAR USUARIO OFFLINE
    // ================================================================
    private fun saveUserOffline(name: String?, photo: String?) {
        val prefs = requireContext().getSharedPreferences("offline_user", 0).edit()
        prefs.putString("name", name)
        prefs.putString("photo", photo)
        prefs.apply()
    }

    // ================================================================
    // 🔵 CARGAR USUARIO OFFLINE
    // ================================================================
    private fun loadUserOffline() {
        Log.d("USER_OFFLINE", "Cargando usuario offline...")

        val prefs = requireContext().getSharedPreferences("offline_user", 0)
        val name = prefs.getString("name", "Usuario")
        val photo = prefs.getString("photo", null)

        binding.tvUserName.text = name?.split(" ")?.get(0) ?: "Usuario"

        if (!photo.isNullOrBlank()) {
            Glide.with(requireContext())
                .load(photo)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_person)
        }
    }

    // ================================================================
    // 🔵 SYNC BACKEND
    // ================================================================
    private fun syncOnlineStatus() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.retrofit.parkingStatus()

                if (res.isSuccessful && res.body()?.status == true) {
                    val json = JSONObject(res.body()!!.data)

                    safely { updateStatusUI(json) }
                    safelySuspend { saveActiveSessionLocally(json) }
                }
            } catch (e: Exception) {
                Log.e("SYNC", "Error sincronizando:", e)
            }
        }
    }

    // ================================================================
    // 🔵 CARGAR SESIÓN OFFLINE
    // ================================================================
    private suspend fun loadOfflineSession() {
        Log.w("OFFLINE", "Cargando sesión local...")

        val session = localRepo.getActiveSession()

        if (session == null) {
            safely {
                binding.tvEstadoActual.text = "Sin conexión"
                binding.tvFechaInicio.text = "--/--/----"
                binding.tvTiempo.text = "00h 00m"
                binding.tvCosto.text = "$0.00"
                binding.btnPagar.visibility = View.GONE
            }
            return
        }

        safely {
            binding.tvEstadoActual.text = session.status
            binding.tvFechaInicio.text = session.entry_time.substring(0, 10)

            val h = (session.elapsed_minutes / 60).toInt()
            val m = (session.elapsed_minutes % 60).toInt()
            binding.tvTiempo.text = "${h}h ${m}m"

            binding.tvCosto.text = "$${session.current_cost}"
            binding.btnPagar.visibility =
                if (session.status == "IN_PROGRESS") View.VISIBLE else View.GONE
        }
    }

    // ================================================================
    // 🔵 Cargar usuario ONLINE y guardarlo local
    // ================================================================
    private fun loadUserDataFromBackend() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.retrofit.getUser()

                if (response.isSuccessful && response.body()?.status == true) {

                    val user = response.body()?.data

                    saveUserOffline(user?.name, user?.profile_picture)

                    safely {
                        val nombre = user?.name ?: "Usuario"
                        binding.tvUserName.text = nombre.split(" ")[0]

                        val fotoUrl = user?.profile_picture
                        if (!fotoUrl.isNullOrBlank()) {
                            Glide.with(requireContext())
                                .load(fotoUrl)
                                .circleCrop()
                                .into(binding.ivProfile)
                        } else {
                            binding.ivProfile.setImageResource(R.drawable.ic_person)
                        }

                        val isAdmin = user?.role == "admin"
                        binding.cardAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("USER", "Error cargando usuario:", e)
            }
        }
    }

    // ================================================================
    // 🔵 SOCKETS
    // ================================================================
    private fun setupSocketListeners() {
        try {
            socket = SocketHandler.getSocket()
        } catch (e: Exception) {
            Log.e("SOCKET", "Error obteniendo socket:", e)
            return
        }

        socket.off("parking_status")
        socket.off("stop_parking_status")
        socket.off("exit_parking")

        socket.on("parking_status") { args ->
            requireActivity().runOnUiThread {
                val json = args[0] as JSONObject
                safely { updateStatusUI(json) }
                lifecycleScope.launch { safelySuspend { saveActiveSessionLocally(json) } }
            }
        }

        socket.on("stop_parking_status") {
            requireActivity().runOnUiThread {
                safely {
                    binding.tvEstadoActual.text = "Pago realizado"
                    binding.btnPagar.visibility = View.GONE
                }
                lifecycleScope.launch { safelySuspend { localRepo.clearActiveSession() } }
            }
        }

        socket.on("exit_parking") {
            requireActivity().runOnUiThread {
                safely {
                    binding.tvEstadoActual.text = "Vehículo ha salido"
                    binding.btnPagar.visibility = View.GONE
                }
                lifecycleScope.launch { safelySuspend { localRepo.clearActiveSession() } }
            }
        }
    }

    // ================================================================
    // Guardar sesión local
    // ================================================================
    private suspend fun saveActiveSessionLocally(json: JSONObject) {
        val entity = ActiveSessionEntity(
            id = json.optString("session_id", "offline"),
            plate = json.optString("plate", ""),
            entry_time = json.optString("entry_time", ""),
            status = json.optString("status", "NONE"),
            elapsed_minutes = json.optDouble("elapsed_minutes", 0.0),
            elapsed_hours = json.optDouble("elapsed_hours", 0.0),
            current_cost = json.optDouble("current_cost", 0.0),
            rate_per_hour = json.optDouble("rate_per_hour", 0.0)
        )

        localRepo.saveActiveSession(entity)
    }

    // ================================================================
    // 🔵 Actualizar UI
    // ================================================================
    private fun updateStatusUI(json: JSONObject) {

        safely {
            val isParked = json.optBoolean("is_parked", false)
            val status = json.optString("status", "NONE")

            val entryTimeRaw = json.optString("entry_time", "")
            val fecha = if ("T" in entryTimeRaw) entryTimeRaw.substring(0, 10)
            else "--/--/----"
            binding.tvFechaInicio.text = fecha

            val totalMinutes = json.optDouble("elapsed_minutes", 0.0)
            val h = (totalMinutes / 60).toInt()
            val m = (totalMinutes % 60).toInt()
            binding.tvTiempo.text = if (isParked) "${h}h ${m}m" else "00h 00m"

            val cost = json.optDouble("current_cost", 0.0)
            binding.tvCosto.text = "$${String.format("%.2f", cost)}"

            binding.tvEstadoActual.text = when (status) {
                "IN_PROGRESS" -> "Estacionado"
                "PAID" -> "Pagado"
                "EXITED" -> "Finalizado"
                "NONE" -> "No estacionado"
                else -> "Desconocido"
            }

            binding.btnPagar.visibility =
                if (isParked && status == "IN_PROGRESS") View.VISIBLE else View.GONE
        }
    }

    // ================================================================
    // PAGAR
    // ================================================================
    private fun setupPagarListener() {
        binding.btnPagar.setOnClickListener {
            pagarEstacionamiento()
        }
    }

    private fun pagarEstacionamiento() {
        lifecycleScope.launch {
            try {
                val statusRes = ApiClient.retrofit.parkingStatus()

                if (!statusRes.isSuccessful) return@launch

                val body = statusRes.body()?.data ?: return@launch
                val sessionId = body["session_id"] as? String ?: return@launch

                val map = hashMapOf<String, Any>("session_id" to sessionId)
                val res = ApiClient.retrofit.payParking(map)

                if (res.isSuccessful && res.body()?.status == true) {
                    binding.tvEstadoActual.text = "Pago realizado"
                    binding.btnPagar.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("PAGO", "Error pagando", e)
            }
        }
    }

    // ================================================================
    // Navegación
    // ================================================================
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

    // ================================================================
    // Helpers
    // ================================================================
    private inline fun safely(block: () -> Unit) {
        try {
            if (_binding != null) block()
        } catch (e: Exception) {
            Log.e("SAFE", "Error al ejecutar:", e)
        }
    }

    private suspend inline fun safelySuspend(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("SAFE", "Error suspend:", e)
        }
    }

    override fun onDestroyView() {
        socket.off("parking_status")
        socket.off("stop_parking_status")
        socket.off("exit_parking")
        _binding = null
        super.onDestroyView()
    }
}
