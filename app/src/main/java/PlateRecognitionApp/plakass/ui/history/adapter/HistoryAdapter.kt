package PlateRecognitionApp.plakass.ui.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import PlateRecognitionApp.plakass.R
import PlateRecognitionApp.plakass.databinding.ItemParkingHistoryBinding
import PlateRecognitionApp.plakass.ui.history.model.ParkingHistory

class HistoryAdapter :
    ListAdapter<ParkingHistory, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemParkingHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemParkingHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: ParkingHistory) {
            binding.tvDate.text = history.date
            binding.tvPlate.text = history.plateNumber
            binding.tvLocation.text = history.location
            binding.tvDuration.text = history.duration
            binding.tvCost.text = history.cost
            binding.tvTime.text = history.timeRange
            binding.chipStatus.text = history.status

            when (history.status) {
                "Completado" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.green_light)
                    binding.chipStatus.setChipStrokeColorResource(R.color.green_dark)
                }
                "Activo" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.blue_light)
                    binding.chipStatus.setChipStrokeColorResource(R.color.blue_dark)
                }
                "Cancelado" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.red_light)
                    binding.chipStatus.setChipStrokeColorResource(R.color.red_dark)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ParkingHistory>() {
        override fun areItemsTheSame(oldItem: ParkingHistory, newItem: ParkingHistory) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ParkingHistory, newItem: ParkingHistory) =
            oldItem == newItem
    }
}
