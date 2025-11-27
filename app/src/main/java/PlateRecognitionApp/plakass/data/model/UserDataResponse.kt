package PlateRecognitionApp.plakass.data.model

data class UserDataResponse(
    val status: Boolean,
    val message: String,
    val data: UserData?
)