package PlateRecognitionApp.plakass.data.model

data class PendingNoAppItem(
    val id: String,
    val plate: String,
    val entry_time: String,
    val status: String,
    val elapsed_minutes: Double,
    val elapsed_hours: Double,
    val current_price: Double,
    val rate_per_hour: Int
)

data class PendingNoAppResponse(
    val status: Boolean,
    val message: String,
    val data: List<PendingNoAppItem>
)
