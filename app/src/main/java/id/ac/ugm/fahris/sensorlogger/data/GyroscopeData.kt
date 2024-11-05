package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "gyroscope_data",
    foreignKeys = [
        ForeignKey(
            entity = RecordData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GyroscopeData(
    @PrimaryKey(autoGenerate = true) val gyroscopeId: Int = 0,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    @ColumnInfo(index = true)
    val recordId: Int
)