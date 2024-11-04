package id.ac.ugm.fahris.sensorlogger.data

data class Sensor(
    val name: String,
    val type: Int, // Use Sensor.TYPE_ constants or -1 for custom types like Location
    var isRecording: Boolean = false // Add isRecording property
) {
    companion object {
        const val TYPE_ACCELEROMETER: Int = 0
        const val TYPE_GYROSCOPE: Int = 1
        const val TYPE_LIGHT: Int = 2
        const val TYPE_LOCATION: Int = 3
    }
}