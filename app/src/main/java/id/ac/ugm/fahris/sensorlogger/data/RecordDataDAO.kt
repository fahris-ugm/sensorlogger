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
    suspend fun insertRecordData(recordData: RecordData)

    @Insert
    suspend fun insertAccelerometerData(accelerometerData: AccelerometerData)

    @Insert
    suspend fun insertGyroscopeData(gyroscopeData: GyroscopeData)

    @Insert
    suspend fun insertLightData(lightData: LightData)

    @Insert
    suspend fun insertLocationData(locationData: LocationData)

    @Transaction
    @Query("SELECT * FROM record_data")
    suspend fun getAllRecordData(): List<RecordData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordDataById(recordId: Int): RecordData

    /*
    @Transaction
    @Query("SELECT * FROM accelerometer_data WHERE recordId = :recordId")
    suspend fun getAccelerometerDataByRecordId(recordId: Int): List<RecordWithAccelerometerData>

    @Transaction
    @Query("SELECT * FROM gyroscope_data WHERE recordId = :recordId")
    suspend fun getGyroscopeDataByRecordId(recordId: Int): List<RecordWithGyroscopeData>

    @Transaction
    @Query("SELECT * FROM light_data WHERE recordId = :recordId")
    suspend fun getLightDataByRecordId(recordId: Int): List<RecordWithLightData>

    @Transaction
    @Query("SELECT * FROM location_data WHERE recordId = :recordId")
    suspend fun getLocationDataByRecordId(recordId: Int): List<RecordWithLocationData>
*/
    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithAccelerometerData(recordId: Int): List<RecordWithAccelerometerData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithGyroscopeData(recordId: Int): List<RecordWithGyroscopeData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithLightData(recordId: Int): List<RecordWithLightData>

    @Transaction
    @Query("SELECT * FROM record_data WHERE recordId = :recordId")
    suspend fun getRecordWithLocationData(recordId: Int): List<RecordWithLocationData>

    @Update
    suspend fun updateRecordData(recordData: RecordData)

    @Delete
    suspend fun deleteRecordData(recordData: RecordData)

}