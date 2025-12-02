package PlateRecognitionApp.plakass.data.model

data class EntryQRResponse(
    val status: Boolean,
    val message: String,
    val data: EntryQRData
)

data class EntryQRData(
    val qr: String,
    val code: String,
    val expires_in: Int
)
