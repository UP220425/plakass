package PlateRecognitionApp.plakass.data.local.repository

import android.content.Context
import PlateRecognitionApp.plakass.data.local.database.ParkingDatabase
import PlateRecognitionApp.plakass.data.local.entities.ActiveSessionEntity
import PlateRecognitionApp.plakass.data.local.entities.HistorySessionEntity

class ParkingLocalRepository(context: Context) {

    private val dao = ParkingDatabase.getDatabase(context).parkingDao()

    // ACTIVE SESSION
    suspend fun saveActiveSession(session: ActiveSessionEntity) =
        dao.saveActiveSession(session)

    suspend fun getActiveSession(): ActiveSessionEntity? =
        dao.getActiveSession()

    suspend fun clearActiveSession() =
        dao.clearActiveSession()


    // HISTORY
    suspend fun saveHistorySessions(list: List<HistorySessionEntity>) =
        dao.saveHistorySessions(list)

    suspend fun getHistory(): List<HistorySessionEntity> =
        dao.getHistory()

    suspend fun clearHistory() =
        dao.clearHistory()
}
