package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import id.ac.ugm.fahris.sensorlogger.R

class SensorDetailActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var lineChart: LineChart
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private lateinit var xValueTextView: TextView
    private lateinit var yValueTextView: TextView
    private lateinit var zValueTextView: TextView

    private val xEntries = mutableListOf<Entry>()
    private val yEntries = mutableListOf<Entry>()
    private val zEntries = mutableListOf<Entry>()
    private var timestamp = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize TextViews for real-time values
        xValueTextView = findViewById(R.id.xValueTextView)
        yValueTextView = findViewById(R.id.yValueTextView)
        zValueTextView = findViewById(R.id.zValueTextView)

        // Initialize the LineChart
        lineChart = findViewById(R.id.lineChart)
        lineChart.data = LineData()
        lineChart.description = Description().apply { text = "Real-Time Sensor Data" }

        // Initialize SensorManager and select the sensor (e.g., Accelerometer)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)  // Change as needed

        // Register listener for sensor updates
        sensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val xValue = timestamp  // Use timestamp as x-axis for real-time update
            val xAxisValue = it.values[0]  // Use the first axis value, adjust if needed
            val yAxisValue = it.values[1]  // Use the second axis value, adjust if needed
            val zAxisValue = it.values[2]  // Use the third axis value, adjust if needed

            xValueTextView.text = "X: $xAxisValue"
            yValueTextView.text = "Y: $yAxisValue"
            zValueTextView.text = "Z: $zAxisValue"

            xEntries.add(Entry(xValue, xAxisValue))
            yEntries.add(Entry(xValue, yAxisValue))
            zEntries.add(Entry(xValue, zAxisValue))
            timestamp += 1  // Increment timestamp for next entry

            updateChart()
        }
    }

    private fun updateChart() {
        val xDataSet = LineDataSet(xEntries, "X-Axis").apply {
            lineWidth = 2f
            color = android.graphics.Color.RED
            setDrawCircles(false)
            setDrawValues(false)
        }
        val yDataSet = LineDataSet(yEntries, "Y-Axis").apply {
            lineWidth = 2f
            color = android.graphics.Color.GREEN
            setDrawCircles(false)
            setDrawValues(false)
        }
        val zDataSet = LineDataSet(zEntries, "Z-Axis").apply {
            lineWidth = 2f
            color = android.graphics.Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
        }

        lineChart.data = LineData(xDataSet, yDataSet, zDataSet)
        lineChart.invalidate()  // Refresh chart
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something if sensor accuracy changes
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the sensor listener to stop receiving updates
        sensorManager.unregisterListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {  // The ID of the back button
                finish()  // Close this activity and go back
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}