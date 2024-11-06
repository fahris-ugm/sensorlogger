package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.RecordData

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {

    private val recordDataDAO = AppDatabase.getDatabase(application).recordDataDao()
    val allRecordData: LiveData<List<RecordData>>

    init {
        allRecordData = recordDataDAO.getAllRecordDataLive()
    }
}