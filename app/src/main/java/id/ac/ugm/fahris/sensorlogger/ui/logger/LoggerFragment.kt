package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.ac.ugm.fahris.sensorlogger.data.Sensor
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentLoggerBinding

class LoggerFragment : Fragment(), SensorListAdapter.OnSensorClickListener {

    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var sensorListAdapter: SensorListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize sensor list
        val sensors = listOf(
            Sensor("Accelerometer", Sensor.TYPE_ACCELEROMETER),
            Sensor("Gyroscope", Sensor.TYPE_GYROSCOPE),
            Sensor("Light", Sensor.TYPE_LIGHT),
            Sensor("Location", Sensor.TYPE_LOCATION)
        )

        // Initialize SensorListAdapter
        sensorListAdapter = SensorListAdapter(sensors, this) // 'this' implements OnSensorClickListener
        val sensorRecyclerView = binding.sensorRecyclerView
        sensorRecyclerView.adapter = sensorListAdapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSensorToggle(sensor: Sensor, isChecked: Boolean) {

        Log.d("LoggerFragment", "onSensorToggle: $sensor, $isChecked")
    }

    override fun onSensorDetailsClick(sensor: Sensor) {
        Log.d("LoggerFragment", "onSensorDetailsClick: $sensor")
    }
}