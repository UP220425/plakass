package PlateRecognitionApp.plakass.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.data.local.entities.HistorySessionEntity
import PlateRecognitionApp.plakass.data.local.repository.ParkingLocalRepository
import PlateRecognitionApp.plakass.data.model.ParkingSession
import PlateRecognitionApp.plakass.databinding.FragmentHistoryBinding
import PlateRecognitionApp.plakass.ui.history.adapter.HistoryAdapter
import PlateRecognitionApp.plakass.ui.history.model.ParkingHistory
import PlateRecognitionApp.plakass.utils.NetworkUtils
import PlateRecognitionApp.plakass.utils.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var localRepo: ParkingLocalRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        localRepo = ParkingLocalRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupFilterButtons()

        // ---------------------------------------------------------------
        // 🔥 MONITOREAR INTERNET — actualizar historial cuando vuelva
        // ---------------------------------------------------------------
        lifecycleScope.launchWhenStarted {
            NetworkMonitor.isConnected.collectLatest { connected ->
                if (connected) loadHistory("all")
            }
        }

        // 🔥 Si no hay internet → cargar offline
        lifecycleScope.launch {
            if (!NetworkUtils.isConnected(requireContext())) {
                loadOfflineHistory()
            } else {
                loadHistory("all")
            }
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFilterButtons() {

        binding.btnFilterAll.setOnClickListener {
            updateFilterSelection(binding.btnFilterAll)

            lifecycleScope.launch {
                if (!NetworkUtils.isConnected(requireContext())) loadOfflineHistory()
                else loadHistory("all")
            }
        }

        binding.btnFilterMonth.setOnClickListener {
            updateFilterSelection(binding.btnFilterMonth)

            lifecycleScope.launch {
                if (!NetworkUtils.isConnected(requireContext())) loadOfflineHistory()
                else loadHistory("month")
            }
        }

        binding.btnFilterWeek.setOnClickListener {
            updateFilterSelection(binding.btnFilterWeek)

            lifecycleScope.launch {
                if (!NetworkUtils.isConnected(requireContext())) loadOfflineHistory()
                else loadHistory("week")
            }
        }
    }

    private fun updateFilterSelection(selectedButton: com.google.android.material.button.MaterialButton) {
        listOf(binding.btnFilterAll, binding.btnFilterMonth, binding.btnFilterWeek).forEach { btn ->
            btn.setBackgroundColor(requireContext().getColor(R.color.button_filter_unselected))
            btn.setTextColor(requireContext().getColor(R.color.primary_dark))
        }

        selectedButton.setBackgroundColor(requireContext().getColor(R.color.primary_dark))
        selectedButton.setTextColor(requireContext().getColor(android.R.color.white))
    }

    // ==========================================================
    // 🔥 OFFLINE MODE — CARGAR HISTORIAL DESDE SQLITE
    // ==========================================================
    private suspend fun loadOfflineHistory() {
        val list = localRepo.getHistory()

        if (list.isEmpty()) {
            showEmptyState()
            return
        }

        val uiList = list.map { entity ->
            ParkingHistory(
                id = entity.id,
                date = entity.entry_time.substring(0, 10),
                plateNumber = entity.plate,
                location = "Offline",
                duration = formatDuration(entity.duration_minutes),
                cost = "$${entity.price ?: 0.0}",
                timeRange = entity.exit_time ?: "Sin datos",
                status = entity.status
            )
        }

        showHistoryList()
        historyAdapter.submitList(uiList)
        updateStatistics(uiList)
    }

    private fun formatDuration(minutes: Double?): String {
        if (minutes == null) return "N/A"
        val total = minutes.toInt()
        val h = total / 60
        val m = total % 60
        return "${h}h ${m}m"
    }

    // ==========================================================
    // 🔵 ONLINE MODE — CARGAR DEL BACKEND Y GUARDAR LOCALMENTE
    // ==========================================================
    private fun loadHistory(filter: String) {
        lifecycleScope.launch {
            try {
                val response = when (filter) {
                    "week" -> ApiClient.retrofit.getHistoryWeek()
                    "month" -> ApiClient.retrofit.getHistoryMonth()
                    else -> ApiClient.retrofit.getHistoryAll()
                }

                if (!response.isSuccessful || response.body()?.status != true) {
                    Toast.makeText(requireContext(), "Error cargando historial", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val sessions = response.body()!!.data.sessions
                val uiList = sessions.map { it.toUI() }

                // Guardar en SQLite 🔥
                saveHistoryLocal(sessions)

                if (uiList.isEmpty()) {
                    showEmptyState()
                } else {
                    showHistoryList()
                    historyAdapter.submitList(uiList)
                    updateStatistics(uiList)
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ==========================================================
    // 🔥 GUARDAR HISTORIAL DEL BACKEND EN SQLITE
    // ==========================================================
    private suspend fun saveHistoryLocal(list: List<ParkingSession>) {
        localRepo.clearHistory()

        val entities = list.map { s ->
            HistorySessionEntity(
                id = s.id,
                plate = s.plate ?: "",
                entry_time = s.entry_time ?: "",
                exit_time = s.exit_time,
                duration_minutes = s.duration_minutes,
                duration_hours = s.duration_hours,
                price = s.price,
                status = when (s.status) {
                    "EXITED" -> "Completado"
                    "PAID" -> "Pagado"
                    "IN_PROGRESS" -> "Activo"
                    else -> "Desconocido"
                }
            )
        }

        localRepo.saveHistorySessions(entities)
    }

    // ==========================================================
    // MAPEO BACKEND → UI
    // ==========================================================
    private fun ParkingSession.toUI(): ParkingHistory {

        val dateFormatted = entry_time?.substring(0, 10) ?: "Sin fecha"

        val start = entry_time?.substring(11, 16) ?: "?"
        val end = exit_time?.substring(11, 16) ?: "?"
        val timeRange = if (exit_time != null) "$start - $end" else "En progreso"

        val durationStr = duration_minutes?.let {
            val h = (it / 60).toInt()
            val m = (it % 60).toInt()
            "${h}h ${m}m"
        } ?: "N/A"

        val costStr = price?.let { "$$it" } ?: "$0"

        val vehicleBrand = vehicle_info?.brand ?: "Sin datos"

        val statusStr = when (status) {
            "EXITED" -> "Completado"
            "PAID" -> "Pagado"
            "IN_PROGRESS" -> "Activo"
            else -> "Desconocido"
        }

        return ParkingHistory(
            id = id,
            date = dateFormatted,
            plateNumber = plate ?: "N/A",
            location = vehicleBrand,
            duration = durationStr,
            cost = costStr,
            timeRange = timeRange,
            status = statusStr
        )
    }

    // ==========================================================
    // UI HELPERS
    // ==========================================================
    private fun updateStatistics(historyList: List<ParkingHistory>) {
        binding.tvTotalVisits.text = historyList.size.toString()

        val totalMinutes = historyList.sumOf { it.duration.toMinutes() }
        binding.tvTotalTime.text = "${totalMinutes / 60}h ${totalMinutes % 60}m"

        val totalCost = historyList.sumOf { it.cost.toMoney() }
        binding.tvTotalSpent.text = "$${"%.2f".format(totalCost)}"
    }

    private fun showEmptyState() {
        binding.rvHistory.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showHistoryList() {
        binding.rvHistory.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==========================================================
// HELPERS
// ==========================================================
fun String.toMinutes(): Int {
    return when {
        contains("h") && contains("m") -> {
            val p = split("h", "m")
            p[0].trim().toInt() * 60 + p[1].trim().toInt()
        }
        contains("h") -> replace("h", "").trim().toInt() * 60
        contains("m") -> replace("m", "").trim().toInt()
        else -> 0
    }
}

fun String.toMoney(): Double {
    return replace("$", "").trim().toDoubleOrNull() ?: 0.0
}
