package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.BuildConfig
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.AppDatabase
import id.ac.ugm.fahris.sensorlogger.data.RecordData
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
import id.ac.ugm.fahris.sensorlogger.utils.FileUtils
import id.ac.ugm.fahris.sensorlogger.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RecordDetailActivity : AppCompatActivity(), RecordedSensorListAdapter.OnSensorClickListener {
    private lateinit var recordTitleEditText: EditText
    private lateinit var editTitleButton: ImageView
    private lateinit var startTimestampTextView: TextView
    private lateinit var endTimestampTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var recordedSensorsTextView: TextView
    private lateinit var sensorsRecyclerView: RecyclerView

    private var isEditMode = false
    private lateinit var sensorsAdapter: RecordedSensorListAdapter
    private var recordData: RecordData? = null
    private var recordId: Long = -1

    // SQLite Room database
    private lateinit var appDatabase: AppDatabase

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Details"

        progressDialog = ProgressDialog(this).apply {
            setTitle("Exporting Files")
            setMessage("Please wait...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }

        appDatabase = AppDatabase.getDatabase(this)
        // Retrieve recording ID from the intent
        recordId = intent.getLongExtra("RECORDING_ID", -1)

        if (recordId >= 0) {
            // Fetch and display the recording details based on the ID
            // Initialize views
            recordTitleEditText = findViewById(R.id.recordTitleEditText)
            editTitleButton = findViewById(R.id.editTitleButton)
            startTimestampTextView = findViewById(R.id.startTimestampTextView)
            endTimestampTextView = findViewById(R.id.endTimestampTextView)
            durationTextView = findViewById(R.id.durationTextView)
            recordedSensorsTextView = findViewById(R.id.recordedSensorsTextView)
            sensorsRecyclerView = findViewById(R.id.sensorsRecyclerView)

            val thisActivity = this
            lifecycleScope.launch {
                val recordData = appDatabase.recordDataDao().getRecordDataById(recordId)
                if (recordData != null) {
                    this@RecordDetailActivity.recordData = recordData
                    recordTitleEditText.setText(recordData.title)
                    startTimestampTextView.text = "Start: ${TimeUtils.formatTimestamp(recordData.startTimestamp)}"
                    endTimestampTextView.text = "End: ${TimeUtils.formatTimestamp(recordData.endTimestamp)}"
                    durationTextView.text = "Duration: ${TimeUtils.formatDuration(recordData.endTimestamp - recordData.startTimestamp)}"
                    // Set up RecyclerView

                    val sensorItems : MutableList<SensorItem> = mutableListOf<SensorItem>()
                    if (recordData.flagAccelerometer) {
                        sensorItems.add(SensorItem("Accelerometer", SensorItem.TYPE_ACCELEROMETER, true ))
                    }
                    if (recordData.flagGyroscope) {
                        sensorItems.add(SensorItem("Gyroscope", SensorItem.TYPE_GYROSCOPE, true ))
                    }
                    if (recordData.flagLight) {
                        sensorItems.add(SensorItem("Light Sensor", SensorItem.TYPE_LIGHT, true ))
                    }
                    if (recordData.flagLocation) {
                        sensorItems.add(SensorItem("Location", SensorItem.TYPE_LOCATION, true ))
                    }
                    if (sensorItems.count() > 0) {
                        recordedSensorsTextView.visibility = View.VISIBLE
                    } else {
                        recordedSensorsTextView.visibility = View.GONE
                    }

                    sensorsAdapter = RecordedSensorListAdapter(sensorItems, thisActivity)
                    sensorsRecyclerView.adapter = sensorsAdapter

                    // Edit button logic
                    editTitleButton.setOnClickListener {
                        isEditMode = !isEditMode
                        recordTitleEditText.isEnabled = isEditMode

                        val resId = if (isEditMode) R.drawable.ic_check_green_24dp else R.drawable.ic_pencil_black_24dp
                        editTitleButton.setImageResource(resId)

                        if (!isEditMode) {
                            saveTitleChanges()
                        }
                    }
                }

            }

        } else {
            Toast.makeText(this, "Invalid recording ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveTitleChanges() {
        if (recordData != null) {
            lifecycleScope.launch {
                val newTitle = recordTitleEditText.text.toString()
                recordData!!.title = newTitle
                appDatabase.recordDataDao().updateRecordData(recordData!!)
                Toast.makeText(this@RecordDetailActivity, "Title saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.record_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {  // The ID of the back button
                finish()  // Close this activity and go back
                true
            }
            R.id.action_delete -> {
                deleteRecord()
                true
            }
            R.id.action_export -> {
                showExportDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteRecord() {
        // Show confirmation dialog
        AlertDialog.Builder(this).apply {
            setTitle("Delete Confirmation")
            setMessage("Are you sure you want to delete record ${recordData?.title}?")

            setPositiveButton("Delete") { _, _ ->
                // Proceed with deletion if confirmed
                performDeletion()
            }

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()  // Close the dialog if canceled
            }

            create()
            show()
        }

    }
    private fun performDeletion() {
        if (recordData != null) {
            lifecycleScope.launch {
                appDatabase.recordDataDao().deleteRecordData(recordData!!)
                Toast.makeText(this@RecordDetailActivity, "Record deleted", Toast.LENGTH_SHORT).show()
                finish() // Close activity after deletion
            }
        }
    }

    private suspend fun exportAccelerometerData(recordId: Long, prefix: String, uniqueId: Long): Boolean {
        val result = appDatabase.recordDataDao().getRecordWithAccelerometerData(recordId)
        val subDir = "${prefix}_${uniqueId}"
        val fileName = "${prefix}_accelerometer_${uniqueId}.csv"
        //val zipFileName = "$_fileName.zip"
        val csvUri = FileUtils.createFileInSubDirectory(this@RecordDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordDetailActivity, "Error creating CSV file for accelerometer", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    // Write CSV header
                    writer.append("ID,Timestamp,X,Y,Z\n")
                    result.forEach { data ->
                        data.accelerometerData.forEach { accelerometerData ->
                            writer.append(
                                "${accelerometerData.accelerometerId},${accelerometerData.timestamp},${accelerometerData.x},${accelerometerData.y},${accelerometerData.z}\n"
                            )
                        }
                    }
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
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

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordDetailActivity, "Error creating CSV file for gyroscope", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    // Write CSV header
                    writer.append("ID,Timestamp,X,Y,Z\n")
                    result.forEach { data ->
                        data.gyroscopeData.forEach { gyroscopeData ->
                            writer.append(
                                "${gyroscopeData.gyroscopeId},${gyroscopeData.timestamp},${gyroscopeData.x},${gyroscopeData.y},${gyroscopeData.z}\n"
                            )
                        }
                    }
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
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

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordDetailActivity, "Error creating CSV file for light", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    // Write CSV header
                    writer.append("ID,Timestamp,Luminance\n")
                    result.forEach { data ->
                        data.lightData.forEach { lightData ->
                            writer.append(
                                "${lightData.lightId},${lightData.timestamp},${lightData.lum}\n"
                            )
                        }
                    }
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
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

        val csvUri = FileUtils.createFileInSubDirectory(this@RecordDetailActivity, fileName, "text/csv", subDir)
        if (csvUri == null) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(this@RecordDetailActivity, "Error creating CSV file for location", Toast.LENGTH_SHORT).show()
            }
            return false
        } else {
            try {
                contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    // Write CSV header
                    writer.append("ID,Timestamp,Lat,Long,Alt\n")
                    result.forEach { data ->
                        data.locationData.forEach { locationData ->
                            writer.append(
                                "${locationData.locationId},${locationData.timestamp},${locationData.latitude},${locationData.longitude},${locationData.altitude}\n"
                            )
                        }
                    }
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordDetailActivity, "Error exporting file: {$e.message}", Toast.LENGTH_LONG).show()
                }
                return false
            }
        }
        return true
    }
    private fun showExportDialog() {
        val dialog = ExportOptionsDialogFragment.newInstance()
        dialog.onConfirmListener = { prefix, shareFile ->
            exportRecord(prefix, shareFile)
        }
        dialog.show(supportFragmentManager, "export_options_dialog")
    }
    private fun exportRecord(prefix: String, shareFile: Boolean) {
        recordData?.let {
            // Show ProgressDialog
            progressDialog.show()

            // Launch a coroutine to handle the export process in the background
            CoroutineScope(Dispatchers.IO).launch {
                val uniqueId = System.currentTimeMillis()
                if (it.flagAccelerometer) {
                    if (!exportAccelerometerData(it.recordId, prefix, uniqueId)) {
                        return@launch
                    }
                }
                if (it.flagGyroscope) {
                    if (!exportGyroscopeData(it.recordId, prefix, uniqueId)) {
                        return@launch
                    }
                }
                if (it.flagLight) {
                    if (!exportLightData(it.recordId, prefix, uniqueId)) {
                        return@launch
                    }
                }
                if (it.flagLocation) {
                    if (!exportLocationData(it.recordId, prefix, uniqueId)) {
                        return@launch
                    }
                }

                if (shareFile) {
                    val directoryName = "${prefix}_${uniqueId}"
                    val zipFileName = "${directoryName}.zip"
                    val zipUri = FileUtils.zipDirectoryAndSaveToMediaStore(
                        this@RecordDetailActivity,
                        directoryName,
                        zipFileName
                    )
                    if (zipUri != null) {
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            FileUtils.shareZipFile(this@RecordDetailActivity, zipUri)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@RecordDetailActivity,
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

    override fun onSensorDetailsClick(sensorItem: SensorItem) {
        //TODO
    }
}