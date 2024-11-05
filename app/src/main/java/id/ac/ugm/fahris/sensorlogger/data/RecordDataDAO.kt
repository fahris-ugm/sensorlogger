package id.ac.ugm.fahris.sensorlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RecordDataDAO {
    @Insert
    suspend fun insertRecordData(recordData: RecordData): Long

    @Insert
    suspend fun insertAccelerometerData(accelerometerData: AccelerometerData): Long

    @Insert
    suspend fun insertGyroscopeData(gyroscopeData: GyroscopeData): Long

    @Insert
    suspend fun insertLightData(lightData: LightData): Long

    @Insert
    suspend fun insertLocationData(locationData: LocationData): Long

    @Transaction
    @Query("SELECT * FROM record_data")
    suspend fun getAllRecordData(): List<RecordData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordDataById(recordId: Long): RecordData

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithAccelerometerData(recordId: Long): List<RecordWithAccelerometerData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithGyroscopeData(recordId: Long): List<RecordWithGyroscopeData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithLightData(recordId: Long): List<RecordWithLightData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithLocationData(recordId: Long): List<RecordWithLocationData>

    @Update
    suspend fun updateRecordData(recordData: RecordData)

    @Delete
    suspend fun deleteRecordData(recordData: RecordData)

}