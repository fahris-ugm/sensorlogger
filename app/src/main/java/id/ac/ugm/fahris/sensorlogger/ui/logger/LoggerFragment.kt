package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.AccelerometerData
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.GyroscopeData
import id.ac.ugm.fahris.sensorlogger.data.LightData
import id.ac.ugm.fahris.sensorlogger.data.LocationData
import id.ac.ugm.fahris.sensorlogger.data.RecordData
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentLoggerBinding
import id.ac.ugm.fahris.sensorlogger.utils.PermissionUtils
import id.ac.ugm.fahris.sensorlogger.utils.PermissionUtils.isPermissionGranted
import kotlinx.coroutines.launch

class LoggerFragment : Fragment(), SensorListAdapter.OnSensorClickListener, SensorEventListener {
    // auto binding view in layout
    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: LoggerViewModel by activityViewModels()
    private lateinit var sensorListAdapter: SensorListAdapter

    // for getting current location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // LocationRequest - Requirements for the location updates, i.e.,
    // how often you should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient
    // has a new Location
    private lateinit var locationCallback: LocationCallback

    // To read sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var lightSensor: Sensor? = null
    // flag to check if recording
    private var isRecording = false

    // SQLite Room database
    private lateinit var appDatabase: AppDatabase
    private lateinit var recordButton: Button
    // modal dialog while recording
    private var recordingDialog: AlertDialog? = null
    private var recordingTitleEditText: EditText? = null
    // for displaying elapsed time while recording
    private var timer: CountDownTimer? = null
    private var elapsedTimeInSeconds = 0

    // recordId of record data being edited
    private var currentRecordId: Long = -1
    private var currentRecordData: RecordData? = null

    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // bind fragment layout
        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize sensor list
        val sensorItems = listOf(
            SensorItem("Accelerometer", SensorItem.TYPE_ACCELEROMETER, viewModel.isRecordAccelerometer),
            SensorItem("Gyroscope", SensorItem.TYPE_GYROSCOPE, viewModel.isRecordGyroscope),
            SensorItem("Light", SensorItem.TYPE_LIGHT, viewModel.isRecordLight),
            SensorItem("Location", SensorItem.TYPE_LOCATION, viewModel.isRecordLocation)
        )

        // Initialize SensorListAdapter
        sensorListAdapter = SensorListAdapter(sensorItems, this) // 'this' implements OnSensorClickListener
        val sensorRecyclerView = binding.sensorRecyclerView
        sensorRecyclerView.adapter = sensorListAdapter

        // Initialize FusedLocationProviderClient and sensors
        initFusedLocationClient()
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
        updateRecordButtonState()

        return root
    }
    private fun updateRecordButtonState() {
        recordButton.isEnabled = !isRecording &&
                        (
                        viewModel.isRecordAccelerometer ||
                        viewModel.isRecordGyroscope ||
                        viewModel.isRecordLight ||
                        viewModel.isRecordLocation
                        )
    }
    @SuppressLint("MissingPermission")
    private fun initFusedLocationClient() {
        Log.d("LoggerFragment", "initFusedLocationClient")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update every 5 seconds
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }
    private fun updateLocation(location: Location) {
        if (!isRecording) return
        if (!viewModel.isRecordLocation) return
        val locationData = LocationData(
            timestamp = System.currentTimeMillis(),
            latitude = location.latitude.toFloat(),
            longitude = location.longitude.toFloat(),
            altitude = location.altitude.toFloat(),
            recordId = currentRecordId
        )
        // TODO timing
        Log.d("LoggerFragment", "updateLocation: $locationData")
        lifecycleScope.launch {
            appDatabase.recordDataDao().insertLocationData(locationData)
        }
    }

    private fun showRecordingDialog() {
        // Set up the dialog view
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_recording, null)
        val elapsedTimeTextView: TextView = dialogView.findViewById(R.id.elapsedTimeTextView)
        val stopRecordingButton: Button = dialogView.findViewById(R.id.stopRecordingButton)
        recordingTitleEditText = dialogView.findViewById(R.id.recordingTitleEditText)

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

        val recordData = RecordData(
            title = "Untitled",
            startTimestamp = System.currentTimeMillis(),
            endTimestamp = System.currentTimeMillis(),
            flagAccelerometer = viewModel.isRecordAccelerometer,
            flagGyroscope = viewModel.isRecordGyroscope,
            flagLight = viewModel.isRecordLight,
            flagLocation = viewModel.isRecordLocation
        )
        currentRecordData = recordData
        val thisFragment = this
        lifecycleScope.launch {
            // save initial record data to Room database and get recordId
            Log.d("LoggerFragment", "startRecording insertRecordData: ${recordData.startTimestamp}")
            currentRecordId = appDatabase.recordDataDao().insertRecordData(recordData)
            currentRecordData?.recordId = currentRecordId
            accelerometer?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
            gyroscope?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
            lightSensor?.let { sensorManager.registerListener(thisFragment, it, SensorManager.SENSOR_DELAY_NORMAL) }
            enableMyLocation()
            startLocationUpdates()
        }
    }
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 2. If if a permission rationale dialog should be shown
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(this.parentFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LoggerFragment.LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
            startLocationUpdates()
        } else {

        }
    }
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }
    private fun stopRecording() {
        Log.d("LoggerFragment", "stopRecording")
        isRecording = false
        recordButton.isEnabled = true

        val removeTask = fusedLocationClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("LoggerFragment", "Location Callback removed successfully.")
            } else {
                Log.d("LoggerFragment", "Failed to remove Location Callback.")
            }
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
        lifecycleScope.launch {
            currentRecordData?.title = recordingTitleEditText?.text.toString()
            // update end timestamp of record data
            currentRecordData?.endTimestamp = System.currentTimeMillis()
            Log.d("LoggerFragment", "stopRecording updateRecordData: ${currentRecordData?.endTimestamp}")
            currentRecordData?.let { appDatabase.recordDataDao().updateRecordData(it) }
            Toast.makeText(requireContext(), "Recording stopped, data saved", Toast.LENGTH_SHORT).show()
        }

        // Stop timer and dismiss dialog
        timer?.cancel()
        recordingDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isRecording = false
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _binding = null
    }

    override fun onSensorToggle(sensorItem: SensorItem, isChecked: Boolean) {
        Log.d("LoggerFragment", "onSensorToggle: $sensorItem, $isChecked")
        if (sensorItem.type == SensorItem.TYPE_ACCELEROMETER) {
            viewModel.isRecordAccelerometer = isChecked
        } else if (sensorItem.type == SensorItem.TYPE_GYROSCOPE) {
            viewModel.isRecordGyroscope = isChecked
        } else if (sensorItem.type == SensorItem.TYPE_LIGHT) {
            viewModel.isRecordLight = isChecked
        } else if (sensorItem.type == SensorItem.TYPE_LOCATION) {
            viewModel.isRecordLocation = isChecked
        }
        updateRecordButtonState()
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
        Log.d("LoggerFragment", "type: ${event.sensor.type} | onSensorChanged: $event")
        if (!isRecording) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (!viewModel.isRecordAccelerometer) return

            // Apply a low-pass filter to isolate the gravity component
            val alpha = 0.8f
            val valueX = event.values.getOrNull(0) ?: 0f
            val valueY = event.values.getOrNull(1) ?: 0f
            val valueZ = event.values.getOrNull(2) ?: 0f

            gravity[0] = alpha * gravity[0] + (1 - alpha) * valueX
            gravity[1] = alpha * gravity[1] + (1 - alpha) * valueY
            gravity[2] = alpha * gravity[2] + (1 - alpha) * valueZ

            // Calculate linear acceleration (excluding gravity)
            linearAcceleration[0] = valueX - gravity[0]
            linearAcceleration[1] = valueY - gravity[1]
            linearAcceleration[2] = valueZ - gravity[2]

            val sensorData = AccelerometerData(
                timestamp = System.currentTimeMillis(),
                x = linearAcceleration[0],
                y = linearAcceleration[1],
                z = linearAcceleration[2],
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                Log.d("LoggerFragment", "onSensorChanged insertAccelerometerData: $sensorData")
                appDatabase.recordDataDao().insertAccelerometerData(sensorData)
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            if (!viewModel.isRecordGyroscope) return
            val sensorData = GyroscopeData(
                timestamp = System.currentTimeMillis(),
                x = event.values.getOrNull(0) ?: 0f,
                y = event.values.getOrNull(1) ?: 0f,
                z = event.values.getOrNull(2) ?: 0f,
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                Log.d("LoggerFragment", "onSensorChanged insertGyroscopeData: $sensorData")
                appDatabase.recordDataDao().insertGyroscopeData(sensorData)
            }
        } else if (event.sensor.type == Sensor.TYPE_LIGHT) {
            if (!viewModel.isRecordLight) return
            val sensorData = LightData(
                timestamp = System.currentTimeMillis(),
                lum = event.values.getOrNull(0) ?: 0f,
                recordId = currentRecordId
            )
            lifecycleScope.launch {
                Log.d("LoggerFragment", "onSensorChanged insertLightData: $sensorData")
                appDatabase.recordDataDao().insertLightData(sensorData)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}