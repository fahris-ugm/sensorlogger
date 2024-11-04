package id.ac.ugm.fahris.sensorlogger.ui.logger

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import id.ac.ugm.fahris.sensorlogger.R

import id.ac.ugm.fahris.sensorlogger.data.Sensor

class SensorListAdapter(private val sensors: List<Sensor>, private val listener: OnSensorClickListener) :
    RecyclerView.Adapter<SensorListAdapter.SensorViewHolder>() {

    interface OnSensorClickListener {
        fun onSensorToggle(sensor: Sensor, isChecked: Boolean)
        fun onSensorDetailsClick(sensor: Sensor)
    }

    class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorNameTextView: TextView = itemView.findViewById(R.id.sensorNameTextView)
        val sensorToggle: SwitchCompat = itemView.findViewById(R.id.sensorToggle)
        val sensorDetailsButton: Button = itemView.findViewById(R.id.sensorDetailsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.sensor_list_item, parent, false)
        return SensorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensor = sensors[position]
        holder.sensorNameTextView.text = sensor.name
        Log.d("SensorListAdapter", "Binding sensor: ${sensor.name}")
        holder.sensorToggle.setOnCheckedChangeListener(null) // Remove previous listener
        holder.sensorToggle.isChecked = sensor.isRecording // Set initial state
        holder.sensorToggle.setOnCheckedChangeListener { _, isChecked ->
            listener.onSensorToggle(sensor, isChecked)
            sensor.isRecording = isChecked // Update sensor state
        }

        holder.sensorDetailsButton.setOnClickListener {
            listener.onSensorDetailsClick(sensor)
        }
    }

    override fun getItemCount(): Int {
        return sensors.size
    }
}