package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "location_data",
    foreignKeys = [
        ForeignKey(
            entity = RecordData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LocationData (
    @PrimaryKey(autoGenerate = true) val locationId: Long = 0,
    val timestamp: Long,
    val latitude: Float,
    val longitude: Float,
    val altitude: Float,
    @ColumnInfo(index = true)
    val recordId: Long
)
