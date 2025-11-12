package PlateRecognitionApp.plakass.ui.history.model

data class ParkingHistory(
    val id: String,
    val date: String,
    val plateNumber: String,
    val location: String,
    val duration: String,
    val cost: String,
    val timeRange: String,
    val status: String
)