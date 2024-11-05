package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "light_data",
    foreignKeys = [
        ForeignKey(
            entity = RecordData::class,
            parentColumns = ["recordId"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LightData (
    @PrimaryKey(autoGenerate = true) val lightId: Int = 0,
    val timestamp: Long,
    val lum: Float,
    @ColumnInfo(index = true)
    val recordId: Int
)