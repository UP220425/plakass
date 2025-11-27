package PlateRecognitionApp.plakass.data.model

data class UpdateUserResponse(
    val status: Boolean,
    val message: String,
    val data: Map<String, String>?
)
