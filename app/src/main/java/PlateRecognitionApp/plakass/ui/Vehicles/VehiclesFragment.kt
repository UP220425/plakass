package PlateRecognitionApp.plakass.ui.vehicles

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import PlateRecognitionApp.plakass.data.api.ApiClient
import PlateRecognitionApp.plakass.databinding.DialogVehicleDetailsBinding
import PlateRecognitionApp.plakass.databinding.DialogVehicleFormBinding
import PlateRecognitionApp.plakass.databinding.FragmentVehiclesBinding
import kotlinx.coroutines.launch
import PlateRecognitionApp.plakass.R

class VehiclesFragment : Fragment() {

    private lateinit var binding: FragmentVehiclesBinding
    private lateinit var vehiclesAdapter: VehiclesAdapter
    private val vehiclesList = mutableListOf<Vehicle>()
    private var selectedVehicle: Vehicle? = null

    private val TAG = "VEHICLES_FRAGMENT"

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
        loadVehiclesFromAPI()
    }

    // -----------------------------------
    // RECYCLER
    // -----------------------------------
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

    // -----------------------------------
    // GET /vehicles/my
    // -----------------------------------
    private fun loadVehiclesFromAPI() {
        lifecycleScope.launch {
            try {
                Log.e(TAG, "Llamando GET /vehicles/my ...")

                val response = ApiClient.retrofit.getMyVehicles()

                Log.e(TAG, "GET /vehicles/my -> code=${response.code()}")
                Log.e(TAG, "RAW: ${response.raw()}")

                if (response.isSuccessful && response.body()?.status == true) {
                    val vehicles = response.body()!!.data
                    Log.e(TAG, "Vehículos recibidos: $vehicles")

                    vehiclesList.clear()
                    vehiclesList.addAll(
                        vehicles.map {
                            Vehicle(
                                id = it.id,
                                licensePlate = it.plate,
                                brand = it.brand,
                                model = it.model,
                                color = it.color,
                                year = it.year,
                                isDefault = false
                            )
                        }
                    )

                    vehiclesAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e(TAG, "Error en GET /vehicles/my: $errorStr")
                    Toast.makeText(requireContext(), "Error cargando vehículos", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN en loadVehiclesFromAPI", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // -----------------------------------
    // POST /vehicles/add
    // -----------------------------------
    private fun saveVehicle(
        plate: String,
        brand: String,
        model: String,
        color: String,
        year: Int,
        isDefault: Boolean
    ) {
        lifecycleScope.launch {
            try {

                val body = hashMapOf<String, Any>(
                    "plate" to plate,
                    "brand" to brand,
                    "model" to model,
                    "color" to color,
                    "year" to year
                )

                Log.e(TAG, "BODY ENVIADO: $body")

                val response = ApiClient.retrofit.addVehicle(body)

                Log.e(TAG, "RESPONSE: ${response.raw()}")

                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(requireContext(), "Vehículo agregado", Toast.LENGTH_SHORT).show()
                    loadVehiclesFromAPI()
                } else {
                    Toast.makeText(requireContext(), response.body()?.message ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "ERROR saveVehicle", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -----------------------------------
    // FORMULARIO (ADD / EDIT)
    // -----------------------------------
    private fun showVehicleForm(vehicle: Vehicle?) {
        selectedVehicle = vehicle

        val dialogBinding = DialogVehicleFormBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        if (vehicle != null) {
            dialogBinding.etLicensePlate.setText(vehicle.licensePlate)
            dialogBinding.etBrand.setText(vehicle.brand)
            dialogBinding.etModel.setText(vehicle.model)
            dialogBinding.etColor.setText(vehicle.color)
            dialogBinding.etYear.setText(vehicle.year.toString())
            dialogBinding.btnSave.text = "Actualizar"
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSave.setOnClickListener {
            if (validateForm(dialogBinding)) {
                saveVehicle(
                    dialogBinding.etLicensePlate.text.toString(),
                    dialogBinding.etBrand.text.toString(),
                    dialogBinding.etModel.text.toString(),
                    dialogBinding.etColor.text.toString(),
                    dialogBinding.etYear.text.toString().toInt(),
                    dialogBinding.cbDefault.isChecked
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateForm(dialogBinding: DialogVehicleFormBinding): Boolean {
        val fields = listOf(
            dialogBinding.etLicensePlate,
            dialogBinding.etBrand,
            dialogBinding.etModel,
            dialogBinding.etColor,
            dialogBinding.etYear
        )

        return fields.all { field ->
            if (field.text.isNullOrEmpty()) {
                field.error = "Requerido"
                false
            } else true
        }
    }

    // -----------------------------------
    // DELETE /vehicles/delete/{id}
    // -----------------------------------
    private fun showDeleteConfirmation(vehicle: Vehicle) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Vehículo")
            .setMessage("¿Seguro que quieres eliminar ${vehicle.licensePlate}?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                deleteVehicle(vehicle)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        lifecycleScope.launch {
            try {
                Log.e(TAG, "DELETE /vehicles/delete/${vehicle.id}")

                val response = ApiClient.retrofit.deleteVehicle(vehicle.id)

                Log.e(TAG, "DELETE -> code=${response.code()}")
                Log.e(TAG, "RAW: ${response.raw()}")

                val body = response.body()
                Log.e(TAG, "BODY PARSEADO DELETE: $body")

                if (response.isSuccessful && body?.status == true) {
                    Toast.makeText(requireContext(), "Vehículo eliminado", Toast.LENGTH_SHORT).show()
                    loadVehiclesFromAPI()
                } else {
                    val msg = body?.message ?: "Error al eliminar"
                    Log.e(TAG, "ERROR AL ELIMINAR VEHÍCULO: $msg")
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN en deleteVehicle", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -----------------------------------
    // DETALLES VEHÍCULO
    // -----------------------------------
    private fun showVehicleDetails(vehicle: Vehicle) {
        val dialogBinding = DialogVehicleDetailsBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvDetailLicensePlate.text = vehicle.licensePlate
        dialogBinding.tvDetailBrand.text = vehicle.brand
        dialogBinding.tvDetailModel.text = vehicle.model
        dialogBinding.tvDetailColor.text = vehicle.color
        dialogBinding.tvDetailYear.text = vehicle.year.toString()

        dialogBinding.btnCloseDetails.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // -----------------------------------
    // VEHÍCULO PRINCIPAL (SOLO LOCAL)
    // -----------------------------------
    private fun setAsDefaultVehicle(vehicle: Vehicle) {
        vehiclesList.forEach { it.isDefault = false }
        vehicle.isDefault = true
        vehiclesAdapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "${vehicle.licensePlate} ahora es principal", Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState() {
        binding.emptyState.visibility = if (vehiclesList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvVehicles.visibility = if (vehiclesList.isNotEmpty()) View.VISIBLE else View.GONE
    }
}

// -----------------------------------
// DATA CLASS
// -----------------------------------
data class Vehicle(
    val id: String,
    val licensePlate: String,
    val brand: String,
    val model: String,
    val color: String,
    val year: Int,
    var isDefault: Boolean = false
)
