package PlateRecognitionApp.plakass.data.model

data class PendingVehicle(
    val id: String,
    val plate: String,
    val entry_time: String,
    val status: String,
    val elapsed_minutes: Double,
    val elapsed_hours: Double,
    val current_price: Int,
    val rate_per_hour: Int
)
