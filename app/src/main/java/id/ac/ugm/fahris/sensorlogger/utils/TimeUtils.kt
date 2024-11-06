package id.ac.ugm.fahris.sensorlogger.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeUtils {
    companion object {
        fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
            return format.format(date)
        }
        // Helper function to format the duration from milliseconds to HH:MM:SS

        fun formatFullDuration(durationMillis: Long): String {
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60)) % 24
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
        fun formatDuration(durationMillis: Long): String {
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
}