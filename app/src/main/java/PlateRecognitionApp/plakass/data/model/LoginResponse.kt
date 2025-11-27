package PlateRecognitionApp.plakass.data.model

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String?,
    val data: UserData
)