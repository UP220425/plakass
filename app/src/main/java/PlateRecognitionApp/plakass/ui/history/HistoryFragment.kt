package PlateRecognitionApp.plakass.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.FragmentHistoryBinding
import PlateRecognitionApp.plakass.ui.history.adapter.HistoryAdapter
import PlateRecognitionApp.plakass.ui.history.model.ParkingHistory

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadHistoryData()
        setupFilterButtons()
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
            loadHistoryData() // Recargar con filtro "todos"
        }

        binding.btnFilterMonth.setOnClickListener {
            updateFilterSelection(binding.btnFilterMonth)
            loadHistoryData() // Recargar con filtro "mes"
        }

        binding.btnFilterWeek.setOnClickListener {
            updateFilterSelection(binding.btnFilterWeek)
            loadHistoryData() // Recargar con filtro "semana"
        }
    }

    private fun updateFilterSelection(selectedButton: com.google.android.material.button.MaterialButton) {
        // Reset all buttons
        listOf(binding.btnFilterAll, binding.btnFilterMonth, binding.btnFilterWeek).forEach { button ->
            // Usar setBackgroundColor o ColorStateList
            button.setBackgroundColor(requireContext().getColor(R.color.button_filter_unselected))
            button.setTextColor(requireContext().getColor(R.color.primary_dark))
        }

        // Set selected button
        selectedButton.setBackgroundColor(requireContext().getColor(R.color.primary_dark))
        selectedButton.setTextColor(requireContext().getColor(android.R.color.white))
    }
    private fun loadHistoryData() {
        // Datos de ejemplo - en una app real esto vendría de una base de datos o API
        val historyList = listOf(
            ParkingHistory(
                id = "1",
                date = "15 Nov 2024",
                plateNumber = "ABC-123",
                location = "A-25",
                duration = "2h 15m",
                cost = "$120",
                timeRange = "14:30 - 16:45",
                status = "Completado"
            ),
            ParkingHistory(
                id = "2",
                date = "14 Nov 2024",
                plateNumber = "XYZ-789",
                location = "B-12",
                duration = "1h 30m",
                cost = "$80",
                timeRange = "10:15 - 11:45",
                status = "Completado"
            ),
            ParkingHistory(
                id = "3",
                date = "12 Nov 2024",
                plateNumber = "DEF-456",
                location = "C-08",
                duration = "4h 00m",
                cost = "$200",
                timeRange = "09:00 - 13:00",
                status = "Completado"
            )
        )

        if (historyList.isEmpty()) {
            showEmptyState()
        } else {
            showHistoryList()
            historyAdapter.submitList(historyList)
            updateStatistics(historyList)
        }
    }

    private fun showEmptyState() {
        binding.rvHistory.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showHistoryList() {
        binding.rvHistory.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    private fun updateStatistics(historyList: List<ParkingHistory>) {
        binding.tvTotalVisits.text = historyList.size.toString()

        // Calcular tiempo total (ejemplo simplificado)
        val totalMinutes = historyList.sumOf { it.duration.toMinutes() }
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        binding.tvTotalTime.text = "${hours}h ${minutes}m"

        // Calcular gasto total
        val totalCost = historyList.sumOf { it.cost.toDouble() }
        binding.tvTotalSpent.text = "$${"%.2f".format(totalCost)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Extension functions para conversión (deberían ir en un archivo separado)
fun String.toMinutes(): Int {
    return when {
        this.contains("h") && this.contains("m") -> {
            val parts = this.split("h", "m")
            parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
        }
        this.contains("h") -> this.replace("h", "").trim().toInt() * 60
        this.contains("m") -> this.replace("m", "").trim().toInt()
        else -> 0
    }
}

fun String.toDouble(): Double {
    return this.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
}