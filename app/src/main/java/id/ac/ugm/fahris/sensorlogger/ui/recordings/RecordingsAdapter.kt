package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.ac.ugm.fahris.sensorlogger.R
import id.ac.ugm.fahris.sensorlogger.data.RecordData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsAdapter : ListAdapter<RecordData, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.record_list_item, parent, false)
        return RecordingViewHolder(view)
    }
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = getItem(position)
        holder.bind(recording)
    }

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.recordTitleTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.startTimestampTextView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)

        fun bind(recording: RecordData) {
            Log.d("RecordingsAdapter", "Binding recording start: ${recording.startTimestamp} |end: ${recording.endTimestamp}")
            titleTextView.text = recording.title
            timestampTextView.text = "Start: ${formatTimestamp(recording.startTimestamp)}"
            durationTextView.text = "Duration: ${formatDuration(recording.endTimestamp - recording.startTimestamp)}"
        }
        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
            return format.format(date)
        }
        // Helper function to format the duration from milliseconds to HH:MM:SS
        /*
        private fun formatDuration(durationMillis: Long): String {
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60)) % 24
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }*/
        private fun formatDuration(durationMillis: Long): String {
            val totalSeconds = durationMillis / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600

            return when {
                hours > 0 -> "$hours hour ${minutes} minutes ${seconds} seconds"
                minutes > 0 -> "$minutes minutes ${seconds} seconds"
                else -> "$seconds seconds"
            }
        }
    }
    // DiffUtil class to optimize RecyclerView performance
    class RecordingDiffCallback : DiffUtil.ItemCallback<RecordData>() {
        override fun areItemsTheSame(oldItem: RecordData, newItem: RecordData): Boolean {
            return oldItem.recordId == newItem.recordId
        }

        override fun areContentsTheSame(oldItem: RecordData, newItem: RecordData): Boolean {
            return oldItem == newItem
        }
    }
}