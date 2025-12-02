package PlateRecognitionApp.plakass.data.model

data class ParkingStatusResponse(
    val status: Boolean,
    val message: String,
    val data: Map<String, Any>?
)
