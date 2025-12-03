package PlateRecognitionApp.plakass.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: String,
    val plate: String,
    val entry_time: String,
    val status: String,
    val elapsed_minutes: Double,
    val elapsed_hours: Double,
    val current_cost: Double,
    val rate_per_hour: Double
)
