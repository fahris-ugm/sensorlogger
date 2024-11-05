package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.Embedded
import androidx.room.Relation

data class RecordWithGyroscopeData(
    @Embedded val record: RecordData,
    @Relation(
        parentColumn = "recordId",
        entityColumn = "recordId"
    )
    val gyroscopeData: List<GyroscopeData>
)
