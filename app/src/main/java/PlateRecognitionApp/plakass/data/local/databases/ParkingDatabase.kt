package PlateRecognitionApp.plakass.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import PlateRecognitionApp.plakass.data.local.dao.ParkingDao
import PlateRecognitionApp.plakass.data.local.entities.ActiveSessionEntity
import PlateRecognitionApp.plakass.data.local.entities.HistorySessionEntity

@Database(
    entities = [ActiveSessionEntity::class, HistorySessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ParkingDatabase : RoomDatabase() {

    abstract fun parkingDao(): ParkingDao

    companion object {
        @Volatile
        private var INSTANCE: ParkingDatabase? = null

        fun getDatabase(context: Context): ParkingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ParkingDatabase::class.java,
                    "parking_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
