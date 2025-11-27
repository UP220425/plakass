package PlateRecognitionApp.plakass.ui.vehicles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import PlateRecognitionApp.plakass.R

class VehiclesAdapter(
    private val vehicles: List<Vehicle>,
    private val onItemAction: (Vehicle, String) -> Unit
) : RecyclerView.Adapter<VehiclesAdapter.VehicleViewHolder>() {

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
        private val tvBrand: TextView = itemView.findViewById(R.id.tvBrand)
        private val tvModel: TextView = itemView.findViewById(R.id.tvModel)
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvYear: TextView = itemView.findViewById(R.id.tvYear)
        private val ivOptions: ImageView = itemView.findViewById(R.id.ivOptions)
        private val layoutDefault: LinearLayout = itemView.findViewById(R.id.layoutDefault)

        fun bind(vehicle: Vehicle) {
            tvLicensePlate.text = vehicle.licensePlate
            tvBrand.text = vehicle.brand
            tvModel.text = vehicle.model
            tvColor.text = vehicle.color
            tvYear.text = vehicle.year.toString()

            layoutDefault.visibility = if (vehicle.isDefault) View.VISIBLE else View.GONE

            ivOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_vehicle_options, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> onItemAction(vehicle, "edit")
                        R.id.menu_delete -> onItemAction(vehicle, "delete")
                        R.id.menu_set_default -> onItemAction(vehicle, "set_default")
                    }
                    true
                }

                popup.show()
            }

            itemView.setOnClickListener {
                onItemAction(vehicle, "view")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(vehicles[position])
    }

    override fun getItemCount() = vehicles.size
}
