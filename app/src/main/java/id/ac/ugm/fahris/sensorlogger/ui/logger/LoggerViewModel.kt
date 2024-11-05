package id.ac.ugm.fahris.sensorlogger.ui.logger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoggerViewModel : ViewModel() {
/*
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

 */
    var isRecordAccelerometer = false
    var isRecordGyroscope = false
    var isRecordLight = false
    var isRecordLocation = false
}