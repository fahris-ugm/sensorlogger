package id.ac.ugm.fahris.sensorlogger.ui.recordings

import android.graphics.Color
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
import id.ac.ugm.fahris.sensorlogger.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsAdapter(
    private val onItemClickListener: (RecordData) -> Unit,
    private val onToggleSelection: (Int) -> Unit
) : ListAdapter<RecordData, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback())
{
    val selectedItemsIndex = mutableSetOf<Int>()  // Set of selected item positions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.record_list_item, parent, false)
        return RecordingViewHolder(view)
    }
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = getItem(position)
        holder.bind(recording)
        holder.itemView.isSelected = selectedItemsIndex.contains(position)
        holder.itemView.setBackgroundColor(
            if (selectedItemsIndex.contains(position)) Color.LTGRAY else Color.TRANSPARENT
        )
        holder.itemView.setOnClickListener {
            if (selectedItemsIndex.isNotEmpty()) {
                onToggleSelection(position)
            } else {
                onItemClickListener(recording)
            }
        }
        holder.itemView.setOnLongClickListener {
            Log.d("RecordingsAdapter", "Long click detected")
            onToggleSelection(position)
            true
        }
    }

    // Clear all selected items
    fun clearSelection() {
        val selectedPositions = selectedItemsIndex.toList()
        selectedItemsIndex.clear()
        selectedPositions.forEach { notifyItemChanged(it) }
    }
    // Return the selected items
    fun getSelectedItems(): List<RecordData> {
        return selectedItemsIndex.map { getItem(it) }
    }

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.recordTitleTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.startTimestampTextView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)

        fun bind(recording: RecordData) {
            Log.d("RecordingsAdapter", "Binding recording start: ${recording.startTimestamp} |end: ${recording.endTimestamp}")
            titleTextView.text = recording.title
            timestampTextView.text = "Start: ${TimeUtils.formatTimestamp(recording.startTimestamp)}"
            durationTextView.text = "Duration: ${TimeUtils.formatDuration(recording.endTimestamp - recording.startTimestamp)}"
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