package PlateRecognitionApp.plakass.data.local.dao

import androidx.room.*
import PlateRecognitionApp.plakass.data.local.entities.ActiveSessionEntity
import PlateRecognitionApp.plakass.data.local.entities.HistorySessionEntity

@Dao
interface ParkingDao {

    // ACTIVE SESSION
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: ActiveSessionEntity)

    @Query("SELECT * FROM active_session LIMIT 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Query("DELETE FROM active_session")
    suspend fun clearActiveSession()


    // HISTORY
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistorySessions(list: List<HistorySessionEntity>)

    @Query("SELECT * FROM history_sessions ORDER BY entry_time DESC")
    suspend fun getHistory(): List<HistorySessionEntity>

    @Query("DELETE FROM history_sessions")
    suspend fun clearHistory()
}
