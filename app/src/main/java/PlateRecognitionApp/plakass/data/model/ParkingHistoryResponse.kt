package PlateRecognitionApp.plakass.data.model

data class ParkingSession(
    val id: String,
    val plate: String,
    val status: String,
    val entry_time: String?,
    val exit_time: String?,
    val price: Double?,
    val paid_at: String?,
    val duration_minutes: Double?,
    val duration_hours: Double?,
    val vehicle_info: VehicleInfo?
)

data class VehicleInfo(
    val plate: String,
    val brand: String?,
    val model: String?,
    val color: String?,
    val year: Int?
)

data class ParkingHistoryResponse(
    val status: Boolean,
    val message: String,
    val data: ParkingHistoryData
)

data class ParkingHistoryData(
    val total_spent: Double,
    val total_visits: Int,
    val sessions: List<ParkingSession>
)
