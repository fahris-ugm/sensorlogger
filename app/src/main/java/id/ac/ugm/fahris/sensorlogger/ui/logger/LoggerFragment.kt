package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.SensorItem
import id.ac.ugm.fahris.sensorlogger.databinding.FragmentLoggerBinding

class LoggerFragment : Fragment(), SensorListAdapter.OnSensorClickListener {

    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var sensorListAdapter: SensorListAdapter

    private var isRecording = false
    private lateinit var recordButton: Button
    private var recordingDialog: AlertDialog? = null
    private var timer: CountDownTimer? = null
    private var elapsedTimeInSeconds = 0

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
    }
    private fun stopRecording() {
        Log.d("LoggerFragment", "stopRecording")
        isRecording = false
        recordButton.isEnabled = true

        // TODO

        // Stop timer and dismiss dialog
        timer?.cancel()
        recordingDialog?.dismiss()
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
}