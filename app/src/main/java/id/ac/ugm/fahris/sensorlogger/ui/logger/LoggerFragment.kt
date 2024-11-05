package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.app.AlertDialog
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.AccelerometerData
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.GyroscopeData
import id.ac.ugm.fahris.sensorlogger.data.LightData
import id.ac.ugm.fahris.sensorlogger.data.RecordData
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentLoggerBinding
import kotlinx.coroutines.launch

class LoggerFragment : Fragment(), SensorListAdapter.OnSensorClickListener, SensorEventListener {

    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var sensorListAdapter: SensorListAdapter

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var lightSensor: Sensor? = null
    private var isRecording = false
    private lateinit var appDatabase: AppDatabase
    private lateinit var recordButton: Button
    private var recordingDialog: AlertDialog? = null
    private var timer: CountDownTimer? = null
    private var elapsedTimeInSeconds = 0
    private var currentRecordId: Long = -1
    private var currentRecordData: RecordData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize sensor list
        val sensorItems = listOf(
            SensorItem("Accelerometer", SensorItem.TYPE_ACCELEROMETER),
            SensorItem("Gyroscope", SensorItem.TYPE_GYROSCOPE),
            SensorItem("Light", SensorItem.TYPE_LIGHT),
            SensorItem("Location", SensorItem.TYPE_LOCATION)
        )

        // Initialize SensorListAdapter
        sensorListAdapter = SensorListAdapter(sensorItems, this) // 'this' implements OnSensorClickListener
        val sensorRecyclerView = binding.sensorRecyclerView
        sensorRecyclerView.adapter = sensorListAdapter

        //
        sensorManager = requireActivity().getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        //

        appDatabase = AppDatabase.getDatabase(requireContext())

        recordButton = binding.recordButton
        recordButton.setOnClickListener {
            showRecordingDialog()
        }

        return root
    }

    private fun showRecordingDialog() {
        // Set up the dialog view
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_recording, null)
        val elapsedTimeTextView: TextView = dialogView.findViewById(R.id.elapsedTimeTextView)
        val stopRecordingButton: Button = dialogView.findViewById(R.id.stopRecordingButton)

        // Create the dialog
        recordingDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Start recording and show the dialog
        startRecording()
        recordingDialog?.show()

        // Set up the timer to update elapsed time
        elapsedTimeInSeconds = 0
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTimeInSeconds++
                val minutes = elapsedTimeInSeconds / 60
                val seconds = elapsedTimeInSeconds % 60
                elapsedTimeTextView.text = String.format("Time Elapsed: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {}
        }.start()

        // Stop recording when the stop button in the dialog is clicked
        stopRecordingButton.setOnClickListener {
            stopRecording()
        }
    }
    private fun startRecording() {
        Log.d("LoggerFragment", "startRecording")
        isRecording = true
        recordButton.isEnabled = false
        // TODO

        val recordData = RecordData(title = "Untitled", startTimestamp = System.currentTimeMillis(), endTimestamp = System.currentTimeMillis())
        currentRecordData = recordData
        val thisFragment = this
        lifecycleScope.launch {
            currentRecordId = appDatabase.recordDataDao().insertRecordData(recordData)
            currentRecordData?.recordId = currentRecordId
            accelerometer?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
            gyroscope?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
            lightSensor?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }
    private fun stopRecording() {
        Log.d("LoggerFragment", "stopRecording")
        isRecording = false
        recordButton.isEnabled = true

        // TODO
        sensorManager.unregisterListener(this)
        lifecycleScope.launch {
            currentRecordData?.endTimestamp = System.currentTimeMillis()
            currentRecordData?.let { appDatabase.recordDataDao().updateRecordData(it) }
        }

        // Stop timer and dismiss dialog
        timer?.cancel()
        recordingDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        _binding = null
    }

    override fun onSensorToggle(sensorItem: SensorItem, isChecked: Boolean) {

        Log.d("LoggerFragment", "onSensorToggle: $sensorItem, $isChecked")
    }

    override fun onSensorDetailsClick(sensorItem: SensorItem) {
        Log.d("LoggerFragment", "onSensorDetailsClick: $sensorItem")
        if (sensorItem.type == SensorItem.TYPE_LOCATION) {
            val intent = Intent(context, LocationDetailActivity::class.java)
            intent.putExtra("sensor_name", sensorItem.name)
            intent.putExtra("sensor_type", sensorItem.type)
            startActivity(intent)
        } else {
            val intent = Intent(context, SensorDetailActivity::class.java)
            intent.putExtra("sensor_name", sensorItem.name)
            intent.putExtra("sensor_type", sensorItem.type)
            startActivity(intent)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val sensorData = AccelerometerData(
                timestamp = System.currentTimeMillis(),
                x = event.values.getOrNull(0) ?: 0f,
                y = event.values.getOrNull(1) ?: 0f,
                z = event.values.getOrNull(2) ?: 0f,
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                appDatabase.recordDataDao().insertAccelerometerData(sensorData)
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val sensorData = GyroscopeData(
                timestamp = System.currentTimeMillis(),
                x = event.values.getOrNull(0) ?: 0f,
                y = event.values.getOrNull(1) ?: 0f,
                z = event.values.getOrNull(2) ?: 0f,
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                appDatabase.recordDataDao().insertGyroscopeData(sensorData)
            }
        } else if (event.sensor.type == Sensor.TYPE_LIGHT) {
            val sensorData = LightData(
                timestamp = System.currentTimeMillis(),
                lum = event.values.getOrNull(0) ?: 0f,
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                appDatabase.recordDataDao().insertLightData(sensorData)
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}