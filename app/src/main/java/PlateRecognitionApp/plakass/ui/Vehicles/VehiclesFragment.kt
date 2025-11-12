package PlateRecognitionApp.plakass.ui.vehicles

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.DialogVehicleDetailsBinding
import PlateRecognitionApp.plakass.databinding.DialogVehicleFormBinding
import PlateRecognitionApp.plakass.databinding.FragmentVehiclesBinding

class VehiclesFragment : Fragment() {

    private lateinit var binding: FragmentVehiclesBinding
    private lateinit var vehiclesAdapter: VehiclesAdapter
    private val vehiclesList = mutableListOf<Vehicle>()
    private var selectedVehicle: Vehicle? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadVehicles()
    }

    private fun setupRecyclerView() {
        vehiclesAdapter = VehiclesAdapter(vehiclesList) { vehicle, action ->
            when (action) {
                "edit" -> showVehicleForm(vehicle)
                "delete" -> showDeleteConfirmation(vehicle)
                "view" -> showVehicleDetails(vehicle)
                "set_default" -> setAsDefaultVehicle(vehicle)
            }
        }

        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vehiclesAdapter
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardAddVehicle.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                showVehicleForm(null)
            }.start()
        }
    }

    private fun showVehicleForm(vehicle: Vehicle?) {
        selectedVehicle = vehicle

        val dialogBinding = DialogVehicleFormBinding.inflate(LayoutInflater.from(requireContext()))

        // CAMBIADO: Usar AlertDialog.Builder en lugar de MaterialAlertDialogBuilder
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        setupDialogViews(dialogBinding, dialog, vehicle)
        dialog.show()
    }

    private fun setupDialogViews(
        dialogBinding: DialogVehicleFormBinding,
        dialog: Dialog,
        vehicle: Vehicle?
    ) {
        // Si es edición, llenar los campos
        vehicle?.let {
            dialogBinding.etLicensePlate.setText(it.licensePlate)
            dialogBinding.etBrand.setText(it.brand)
            dialogBinding.etModel.setText(it.model)
            dialogBinding.etColor.setText(it.color)
            dialogBinding.etYear.setText(it.year.toString())
            dialogBinding.cbDefault.isChecked = it.isDefault
            dialogBinding.btnSave.text = "Actualizar"
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            if (validateForm(dialogBinding)) {
                saveVehicle(
                    licensePlate = dialogBinding.etLicensePlate.text.toString(),
                    brand = dialogBinding.etBrand.text.toString(),
                    model = dialogBinding.etModel.text.toString(),
                    color = dialogBinding.etColor.text.toString(),
                    year = dialogBinding.etYear.text.toString().toIntOrNull() ?: 0,
                    isDefault = dialogBinding.cbDefault.isChecked
                )
                dialog.dismiss()
                showSaveConfirmation()
            }
        }
    }

    private fun validateForm(dialogBinding: DialogVehicleFormBinding): Boolean {
        var isValid = true

        val fields = listOf(
            dialogBinding.etLicensePlate to "Placa",
            dialogBinding.etBrand to "Marca",
            dialogBinding.etModel to "Modelo",
            dialogBinding.etColor to "Color",
            dialogBinding.etYear to "Año"
        )

        fields.forEach { (field, fieldName) ->
            if (field.text.isNullOrEmpty()) {
                field.error = "$fieldName es requerido"
                isValid = false
            } else {
                field.error = null
            }
        }
        return isValid
    }

    private fun saveVehicle(licensePlate: String, brand: String, model: String, color: String, year: Int, isDefault: Boolean) {
        val vehicle = Vehicle(
            id = selectedVehicle?.id ?: System.currentTimeMillis(),
            licensePlate = licensePlate,
            brand = brand,
            model = model,
            color = color,
            year = year,
            isDefault = isDefault
        )

        if (selectedVehicle == null) {
            // Agregar nuevo vehículo
            vehiclesList.add(vehicle)
            vehiclesAdapter.notifyItemInserted(vehiclesList.size - 1)
            binding.rvVehicles.smoothScrollToPosition(vehiclesList.size - 1)
        } else {
            // Actualizar vehículo existente
            val index = vehiclesList.indexOfFirst { it.id == selectedVehicle?.id }
            if (index != -1) {
                vehiclesList[index] = vehicle
                vehiclesAdapter.notifyItemChanged(index)
            }
        }
        updateEmptyState()
    }

    private fun showDeleteConfirmation(vehicle: Vehicle) {
        // CAMBIADO: Usar AlertDialog.Builder en lugar de MaterialAlertDialogBuilder
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Vehículo")
            .setMessage("¿Estás seguro de que quieres eliminar el vehículo ${vehicle.licensePlate}?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                deleteVehicle(vehicle)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        val index = vehiclesList.indexOfFirst { it.id == vehicle.id }
        if (index != -1) {
            val deletedVehicle = vehiclesList.removeAt(index)
            vehiclesAdapter.notifyItemRemoved(index)
            showUndoSnackbar(deletedVehicle, index)
        }
        updateEmptyState()
    }

    private fun showUndoSnackbar(vehicle: Vehicle, index: Int) {
        val snackbar = Snackbar.make(binding.root, "Vehículo eliminado", Snackbar.LENGTH_LONG)
            .setAction("DESHACER") {
                vehiclesList.add(index, vehicle)
                vehiclesAdapter.notifyItemInserted(index)
                updateEmptyState()
            }
            .setActionTextColor(Color.YELLOW)

        snackbar.view.setBackgroundColor(Color.parseColor("#1A237E"))
        snackbar.show()
    }

    private fun showVehicleDetails(vehicle: Vehicle) {
        val dialogBinding = DialogVehicleDetailsBinding.inflate(LayoutInflater.from(requireContext()))

        // CAMBIADO: Usar AlertDialog.Builder en lugar de MaterialAlertDialogBuilder
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        setupVehicleDetails(dialogBinding, vehicle, dialog)
        dialog.show()
    }

    private fun setupVehicleDetails(
        binding: DialogVehicleDetailsBinding,
        vehicle: Vehicle,
        dialog: Dialog
    ) {
        // Header con placa
        binding.tvDetailLicensePlate.text = vehicle.licensePlate
        binding.tvDefaultBadge.visibility = if (vehicle.isDefault) View.VISIBLE else View.GONE

        // Información básica
        binding.tvDetailBrand.text = vehicle.brand
        binding.tvDetailModel.text = vehicle.model
        binding.tvDetailColor.text = vehicle.color
        binding.tvDetailYear.text = vehicle.year.toString()

        // Color indicator dinámico
        val colorMap = mapOf(
            "Rojo" to Color.RED,
            "Azul" to Color.BLUE,
            "Verde" to Color.GREEN,
            "Negro" to Color.BLACK,
            "Blanco" to Color.WHITE,
            "Gris" to Color.GRAY,
            "Plateado" to Color.LTGRAY,
            "Amarillo" to Color.YELLOW,
            "Naranja" to Color.parseColor("#FF9800"),
            "Morado" to Color.parseColor("#9C27B0"),
            "Rosa" to Color.parseColor("#E91E63"),
            "Marrón" to Color.parseColor("#795548"),
            "Azul Marino" to Color.parseColor("#1976D2"),
            "Verde Oscuro" to Color.parseColor("#388E3C")
        )
        binding.colorIndicator.setBackgroundColor(colorMap[vehicle.color] ?: Color.GRAY)

        // Botones de acción
        binding.btnCloseDetails.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnEditVehicle.setOnClickListener {
            dialog.dismiss()
            showVehicleForm(vehicle) // Navegar a edición
        }

        // Opcional: Configurar estadísticas reales si las tienes
        setupVehicleStatistics(binding, vehicle)
    }

    private fun setupVehicleStatistics(binding: DialogVehicleDetailsBinding, vehicle: Vehicle) {
        // Aquí puedes agregar estadísticas reales del vehículo si las tienes
        // Por ahora son datos de ejemplo
        val visits = 15
        val totalTime = "45h"
        val totalSpent = "$850"

        // Si tienes datos reales, reemplaza estos valores
        // Ejemplo: binding.tvVisitsCount.text = vehicle.visits.toString()
    }

    private fun setAsDefaultVehicle(vehicle: Vehicle) {
        vehiclesList.forEach { it.isDefault = false }
        vehicle.isDefault = true
        vehiclesAdapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "${vehicle.licensePlate} establecido como principal", Toast.LENGTH_SHORT).show()
    }

    private fun showSaveConfirmation() {
        val text = if (selectedVehicle == null) "Vehículo agregado" else "Vehículo actualizado"
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState() {
        if (vehiclesList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvVehicles.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvVehicles.visibility = View.VISIBLE
        }
    }

    private fun loadVehicles() {
        if (vehiclesList.isEmpty()) {
            vehiclesList.addAll(getSampleVehicles())
            vehiclesAdapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    private fun getSampleVehicles(): List<Vehicle> {
        return listOf(
            Vehicle(1, "ABC-123", "Toyota", "Corolla", "Rojo", 2022, true),
            Vehicle(2, "XYZ-789", "Honda", "Civic", "Azul", 2021, false),
            Vehicle(3, "DEF-456", "Ford", "Mustang", "Negro", 2023, false)
        )
    }
}

data class Vehicle(
    val id: Long,
    val licensePlate: String,
    val brand: String,
    val model: String,
    val color: String,
    val year: Int,
    var isDefault: Boolean = false
)