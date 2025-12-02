package PlateRecognitionApp.plakass.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.data.model.PendingVehicle

class VehicleNoAppAdapter(
    private val ctx: Context,
    private val list: List<PendingVehicle>
) : android.widget.BaseAdapter() {

    override fun getCount() = list.size
    override fun getItem(position: Int) = list[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view = convertView ?: LayoutInflater.from(ctx)
            .inflate(R.layout.item_pending_vehicle, parent, false)

        val plate = view.findViewById<TextView>(R.id.txtPlate)
        val price = view.findViewById<TextView>(R.id.txtPrice)

        val item = list[position]

        plate.text = item.plate
        price.text = "$${item.current_price} MXN"

        return view
    }
}
