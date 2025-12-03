package PlateRecognitionApp.plakass.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_sessions")
data class HistorySessionEntity(
    @PrimaryKey val id: String,
    val plate: String,
    val entry_time: String,
    val exit_time: String?,
    val duration_minutes: Double?,
    val duration_hours: Double?,
    val price: Double?,
    val status: String
)
