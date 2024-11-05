package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "record_data")
data class RecordData(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val title: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val flagAccelerometer: Boolean,
    val flagGyroscope: Boolean,
    val flagLight: Boolean,
    val flagLocation: Boolean

)
/*
foreignKeys = [
        ForeignKey(
            entity = AccelerometerData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GyroscopeData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LightData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
 */