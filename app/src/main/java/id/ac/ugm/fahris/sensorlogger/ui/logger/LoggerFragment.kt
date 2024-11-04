package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSensorToggle(sensorItem: SensorItem, isChecked: Boolean) {

        Log.d("LoggerFragment", "onSensorToggle: $sensorItem, $isChecked")
    }

    override fun onSensorDetailsClick(sensorItem: SensorItem) {
        Log.d("LoggerFragment", "onSensorDetailsClick: $sensorItem")
        val intent = Intent(context, SensorDetailActivity::class.java)
        intent.putExtra("sensor_name", sensorItem.name)
        intent.putExtra("sensor_type", sensorItem.type)
        startActivity(intent)
    }
}