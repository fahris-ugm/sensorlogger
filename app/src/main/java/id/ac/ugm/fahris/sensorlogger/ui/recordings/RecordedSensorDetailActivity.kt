package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.app.ProgressDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
import id.ac.ugm.fahris.sensorlogger.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class RecordedSensorDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var lineChart: LineChart
    private lateinit var titleTextView: TextView
    private var sensorType = -1
    private var recordId: Long = -1

    private val xEntries = mutableListOf<Entry>()
    private val yEntries = mutableListOf<Entry>()
    private val zEntries = mutableListOf<Entry>()

    // SQLite Room database
    private lateinit var appDatabase: AppDatabase

    private lateinit var progressDialog: ProgressDialog

    private lateinit var googleMap: GoogleMap
    private val pathPoints = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recorded_sensor_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        titleTextView = findViewById(R.id.recordedSensorTitle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recorded Sensor Data"
        intent.getStringExtra("sensor_name")?.let {
            titleTextView.text = it
        }
        intent.getIntExtra("sensor_type", -1).let {
            sensorType = it
        }
        intent.getLongExtra("record_id", -1).let {
            recordId = it
        }
        progressDialog = ProgressDialog(this).apply {
            setTitle("Exporting Files")
            setMessage("Please wait...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }
        appDatabase = AppDatabase.getDatabase(this)

        // Initialize the line chart
        lineChart = findViewById(R.id.recordedLineChart)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.recordedMap) as SupportMapFragment
        if (sensorType == SensorItem.TYPE_LOCATION) {
            lineChart.visibility = View.GONE
            mapFragment.getMapAsync(this)
        } else {
            mapFragment.view?.visibility  = View.GONE
            lineChart.axisRight.isEnabled = false
            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            lineChart.setTouchEnabled(true)
            lineChart.setPinchZoom(true)
            lineChart.description = Description().apply {
                text = ""
            }
            lineChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            lineChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            lineChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
            lineChart.legend.setDrawInside(false)
        }

        if (recordId >= 0) {
            lifecycleScope.launch {
                if (sensorType == SensorItem.TYPE_ACCELEROMETER) {
                    val sensorData = appDatabase.recordDataDao().getRecordWithAccelerometerData(recordId)
                    lineChart.data = LineData()

                    sensorData.forEach { data ->
                        val firstTimestamp = data.accelerometerData.firstOrNull()?.timestamp ?: 0L
                        var counter = 0
                        data.accelerometerData.forEach { accelerometerData ->
                            val timestamp = counter.toFloat() //(accelerometerData.timestamp - firstTimestamp).toFloat()
                            xEntries.add(Entry(timestamp, accelerometerData.x))
                            yEntries.add(Entry(timestamp, accelerometerData.y))
                            zEntries.add(Entry(timestamp, accelerometerData.z))
                            counter++
                        }
                    }
                } else if(sensorType == SensorItem.TYPE_GYROSCOPE) {
                    val sensorData = appDatabase.recordDataDao().getRecordWithGyroscopeData(recordId)
                    lineChart.data = LineData()

                    sensorData.forEach { data ->
                        val firstTimestamp = data.gyroscopeData.firstOrNull()?.timestamp ?: 0L
                        var counter = 0
                        data.gyroscopeData.forEach { gyroscopeData ->
                            val timestamp = counter.toFloat() //(gyroscopeData.timestamp - firstTimestamp).toFloat()
                            xEntries.add(Entry(timestamp, gyroscopeData.x))
                            yEntries.add(Entry(timestamp, gyroscopeData.y))
                            zEntries.add(Entry(timestamp, gyroscopeData.z))
                            counter++
                        }
                    }
                } else if(sensorType == SensorItem.TYPE_LIGHT) {
                    val sensorData = appDatabase.recordDataDao().getRecordWithLightData(recordId)
                    lineChart.data = LineData()

                    sensorData.forEach { data ->
                        val firstTimestamp = data.lightData.firstOrNull()?.timestamp ?: 0L
                        var counter = 0
                        data.lightData.forEach { lightData ->
                            val timestamp = counter.toFloat() //(gyroscopeData.timestamp - firstTimestamp).toFloat()
                            yEntries.add(Entry(timestamp, lightData.lum))
                            counter++
                        }
                    }
                } else if (sensorType == SensorItem.TYPE_LOCATION) {
                    // Do Nothing

                }
                updateChart()
            }
        }
    }
    private fun updateChart() {
        if (sensorType == SensorItem.TYPE_ACCELEROMETER || sensorType == SensorItem.TYPE_GYROSCOPE) {
            val xDataSet = LineDataSet(xEntries, "X").apply {
                lineWidth = 2f
                color = android.graphics.Color.RED
                setDrawCircles(false)
                setDrawValues(false)
            }
            val yDataSet = LineDataSet(yEntries, "Y").apply {
                lineWidth = 2f
                color = android.graphics.Color.GREEN
                setDrawCircles(false)
                setDrawValues(false)
            }
            val zDataSet = LineDataSet(zEntries, "Z").apply {
                lineWidth = 2f
                color = android.graphics.Color.BLUE
                setDrawCircles(false)
                setDrawValues(false)
            }
            lineChart.data = LineData(xDataSet, yDataSet, zDataSet)
        } else if (sensorType == SensorItem.TYPE_LIGHT) {
            val yDataSet = LineDataSet(yEntries, "Luminance").apply {
                lineWidth = 2f
                color = android.graphics.Color.BLUE
                setDrawCircles(false)
                setDrawValues(false)
            }
            lineChart.data = LineData(yDataSet)
        }

        lineChart.invalidate()  // Refresh chart
    }
    // Inflate the export button in the action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recorded_sensor_detail, menu)
        return true
    }
    // Handle export button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                showExportDialog()
                true
            }
            android.R.id.home -> {
                finish()  // Close this activity and go back
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        populatePathPoints()
    }
    private fun populatePathPoints() {
        lifecycleScope.launch {
            val sensorData = appDatabase.recordDataDao().getRecordWithLocationData(recordId)
            val latLng: LatLng? = sensorData.firstOrNull()?.locationData?.firstOrNull()?.let {
                LatLng(it.latitude, it.longitude)
            }
            sensorData.forEach { data ->
                data.locationData.forEach { locationData ->
                    pathPoints.add(LatLng(locationData.latitude, locationData.longitude))
                }
            }
            drawPath()
            // Move camera to the latest location
            latLng?.let {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            }
        }
    }
    private fun drawPath() {
        val polylineOptions = PolylineOptions().addAll(pathPoints).width(5f).color(android.graphics.Color.BLUE)
        googleMap.clear() // Clear previous path
        googleMap.addPolyline(polylineOptions) // Draw updated path
    }
    private fun showExportDialog() {
        val dialog = ExportOptionsDialogFragment.newInstance()
        dialog.onConfirmListener = { prefix, shareFile ->
            exportSensorData(prefix, shareFile)
        }
        dialog.show(supportFragmentManager, "export_options_dialog")
    }
    private suspend fun exportAccelerometerData(recordId: Long, prefix: String, uniqueId: Long): Boolean {
        val result = appDatabase.recordDataDao().getRecordWithAccelerometerData(recordId)
        val subDir = "${prefix}_${uniqueId}"
        val fileName = "${prefix}_accelerometer_${uniqueId}.csv"

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordedSensorDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordedSensorDetailActivity, "Error creating CSV file for accelerometer", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                FileUtils.writeAccelerometerCSV(this@RecordedSensorDetailActivity, csvUri, result)
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordedSensorDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }
    private suspend fun exportGyroscopeData(recordId: Long, prefix: String, uniqueId: Long): Boolean {
        val result = appDatabase.recordDataDao().getRecordWithGyroscopeData(recordId)
        val subDir = "${prefix}_${uniqueId}"
        val fileName = "${prefix}_gyroscope_${uniqueId}.csv"

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordedSensorDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordedSensorDetailActivity, "Error creating CSV file for gyroscope", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                FileUtils.writeGyroscopeCSV(this@RecordedSensorDetailActivity, csvUri, result)
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordedSensorDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }
    private suspend fun exportLightData(recordId: Long, prefix: String, uniqueId: Long): Boolean {
        val result = appDatabase.recordDataDao().getRecordWithLightData(recordId)
        val subDir = "${prefix}_${uniqueId}"
        val fileName = "${prefix}_light_${uniqueId}.csv"

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordedSensorDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordedSensorDetailActivity, "Error creating CSV file for light", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                FileUtils.writeLightCSV(this@RecordedSensorDetailActivity, csvUri, result)
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordedSensorDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }
    private suspend fun exportLocationData(recordId: Long, prefix: String, uniqueId: Long): Boolean {
        val result = appDatabase.recordDataDao().getRecordWithLocationData(recordId)
        val subDir = "${prefix}_${uniqueId}"
        val fileName = "${prefix}_location_${uniqueId}.csv"

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordedSensorDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordedSensorDetailActivity, "Error creating CSV file for location", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                FileUtils.writeLocationCSV(this@RecordedSensorDetailActivity, csvUri, result)
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordedSensorDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }
    // Function to export sensor data
    private fun exportSensorData(prefix: String, shareFile: Boolean) {
        // Show ProgressDialog
        progressDialog.show()
        // Launch a coroutine to handle the export process in the background
        CoroutineScope(Dispatchers.IO).launch {
            val uniqueId = System.currentTimeMillis()
            if (sensorType == SensorItem.TYPE_ACCELEROMETER) {
                if (!exportAccelerometerData(recordId, prefix, uniqueId)) {
                    return@launch
                }
            } else if (sensorType == SensorItem.TYPE_GYROSCOPE) {
                if (!exportGyroscopeData(recordId, prefix, uniqueId)) {
                    return@launch
                }
            } else if (sensorType == SensorItem.TYPE_LIGHT) {
                if (!exportLightData(recordId, prefix, uniqueId)) {
                    return@launch
                }
            } else if (sensorType == SensorItem.TYPE_LOCATION) {
                if (!exportLocationData(recordId, prefix, uniqueId)) {
                    return@launch
                }
            }
            if (shareFile) {
                val directoryName = "${prefix}_${uniqueId}"
                val zipFileName = "${directoryName}.zip"
                val zipUri = FileUtils.zipDirectoryAndSaveToMediaStore(
                    this@RecordedSensorDetailActivity,
                    directoryName,
                    zipFileName
                )
                if (zipUri != null) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        FileUtils.shareZipFile(this@RecordedSensorDetailActivity, zipUri)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordedSensorDetailActivity,
                            "Error creating ZIP file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }
}