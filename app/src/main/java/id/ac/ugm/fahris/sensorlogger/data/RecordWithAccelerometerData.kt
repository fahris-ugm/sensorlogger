package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.Embedded
import androidx.room.Relation

data class RecordWithAccelerometerData(
    @Embedded val record: RecordData,
    @Relation(
        parentColumn = "recordId",
        entityColumn = "recordId"
    )
    val accelerometerData: List<AccelerometerData>
)
