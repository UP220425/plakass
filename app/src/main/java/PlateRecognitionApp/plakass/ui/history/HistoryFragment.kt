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
import PlateRecognitionApp.plakass.data.model.ParkingSession
import PlateRecognitionApp.plakass.databinding.FragmentHistoryBinding
import PlateRecognitionApp.plakass.ui.history.adapter.HistoryAdapter
import PlateRecognitionApp.plakass.ui.history.model.ParkingHistory
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupFilterButtons()

        loadHistory("all")
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
            loadHistory("all")
        }

        binding.btnFilterMonth.setOnClickListener {
            updateFilterSelection(binding.btnFilterMonth)
            loadHistory("month")
        }

        binding.btnFilterWeek.setOnClickListener {
            updateFilterSelection(binding.btnFilterWeek)
            loadHistory("week")
        }
    }

    private fun updateFilterSelection(selectedButton: com.google.android.material.button.MaterialButton) {
        listOf(binding.btnFilterAll, binding.btnFilterMonth, binding.btnFilterWeek).forEach { button ->
            button.setBackgroundColor(requireContext().getColor(R.color.button_filter_unselected))
            button.setTextColor(requireContext().getColor(R.color.primary_dark))
        }

        selectedButton.setBackgroundColor(requireContext().getColor(R.color.primary_dark))
        selectedButton.setTextColor(requireContext().getColor(android.R.color.white))
    }

    // ---------------------------------------------------------
    // 🚀  CARGA HISTORIAL DESDE EL BACKEND
    // ---------------------------------------------------------
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

                val data = response.body()!!.data
                val listUI = data.sessions.map { it.toUI() }

                if (listUI.isEmpty()) {
                    showEmptyState()
                } else {
                    showHistoryList()
                    historyAdapter.submitList(listUI)
                    updateStatistics(listUI)
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------------------------------------
    // 🚀 MAPEAR JSON DEL BACKEND → UI MODEL
    // ---------------------------------------------------------
    private fun ParkingSession.toUI(): ParkingHistory {

        // Formatear fecha
        val dateFormatted = entry_time?.substring(0, 10) ?: "Sin fecha"

        // Horario
        val start = entry_time?.let {
            try { it.substring(11, 16) } catch (e: Exception) { "?" }
        } ?: "?"

        val end = exit_time?.let {
            try { it.substring(11, 16) } catch (e: Exception) { "?" }
        } ?: "?"

        val timeRange = if (exit_time != null) "$start - $end" else "En progreso"

        // Duración
        val durationStr = duration_minutes?.let {
            val h = (it / 60).toInt()
            val m = (it % 60).toInt()
            "${h}h ${m}m"
        } ?: "N/A"

        // Costo
        val costStr = price?.let { "$$it" } ?: "$0"

        // Vehículo
        val location = vehicle_info?.brand ?: "Sin datos"

        // Estado
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
            location = location,
            duration = durationStr,
            cost = costStr,
            timeRange = timeRange,
            status = statusStr
        )
    }


    private fun updateStatistics(historyList: List<ParkingHistory>) {
        binding.tvTotalVisits.text = historyList.size.toString()

        val totalTime = historyList.sumOf { it.duration.toMinutes() }
        binding.tvTotalTime.text = "${totalTime / 60}h ${totalTime % 60}m"

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

// ---------------------------------------------------------
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
