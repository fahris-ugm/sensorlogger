package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "record_data")
data class RecordData(
    @PrimaryKey(autoGenerate = true) var recordId: Long = 0,
    var title: String,
    val startTimestamp: Long,
    var endTimestamp: Long,
    val flagAccelerometer: Boolean = false,
    val flagGyroscope: Boolean = false,
    val flagLight: Boolean = false,
    val flagLocation: Boolean = false

)
