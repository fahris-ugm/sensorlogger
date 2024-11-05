package id.ac.ugm.fahris.sensorlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecordData::class,
        AccelerometerData::class,
        GyroscopeData::class,
        LightData::class,
        LocationData::class
    ],
    version = 1, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDataDao(): RecordDataDAO

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "record_data_db"
                ).build().also { instance = it }
            }
    }
}