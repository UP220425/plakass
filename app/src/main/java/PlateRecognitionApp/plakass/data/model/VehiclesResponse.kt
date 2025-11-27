package PlateRecognitionApp.plakass.data.model

data class VehicleResponse(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val color: String,
    val year: Int
)

data class VehiclesResponse(
    val status: Boolean,
    val message: String,
    val data: List<VehicleResponse>
)
