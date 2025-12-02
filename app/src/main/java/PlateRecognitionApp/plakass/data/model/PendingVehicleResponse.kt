package PlateRecognitionApp.plakass.data.model

data class PendingVehicleResponse(
    val status: Boolean,
    val message: String,
    val data: List<PendingVehicle>
)