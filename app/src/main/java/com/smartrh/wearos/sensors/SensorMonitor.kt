package com.smartrh.wearos.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smartrh.wearos.model.AccelerometerPayload
import kotlin.math.sqrt

class SensorMonitor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var accelerometerPayload by mutableStateOf(AccelerometerPayload())
        private set

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values.getOrNull(0) ?: 0f
        val y = event.values.getOrNull(1) ?: 0f
        val z = event.values.getOrNull(2) ?: 0f
        val movement = sqrt((x * x + y * y + z * z).toDouble())

        accelerometerPayload = AccelerometerPayload(
            x = x,
            y = y,
            z = z,
            movement = movement
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
