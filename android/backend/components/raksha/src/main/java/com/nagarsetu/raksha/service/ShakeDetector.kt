package com.nagarsetu.raksha.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * ShakeDetector detects rapid emergency shaking gestures via the device accelerometer.
 *
 * Algorithm:
 * - Computes magnitude of linear acceleration minus gravity:
 *   a_net = |sqrt(x^2 + y^2 + z^2) - 9.80665|
 * - Requires 3 severe acceleration spikes (> 24.0 m/s^2) separated by >= 150ms within a 1.5s window.
 * - Enforces a 5-second cooldown after a trigger to prevent duplicate SOS dispatches.
 */
class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeDetector"
        private const val GRAVITY = 9.80665f
        private const val ACCEL_THRESHOLD = 24.0f // m/s^2 jerk threshold
        private const val MIN_SPIKE_INTERVAL_MS = 150L
        private const val WINDOW_DURATION_MS = 1500L
        private const val COOLDOWN_DURATION_MS = 5000L
        private const val REQUIRED_SPIKE_COUNT = 3
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var spikeTimestamps = LongArray(REQUIRED_SPIKE_COUNT)
    private var spikeCount = 0
    private var lastSpikeTime = 0L
    private var lastTriggerTime = 0L

    val isSupported: Boolean get() = accelerometer != null

    fun start(): Boolean {
        if (accelerometer == null || sensorManager == null) {
            Log.w(TAG, "Accelerometer not available on this device")
            return false
        }
        spikeCount = 0
        lastSpikeTime = 0L
        return sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        spikeCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < COOLDOWN_DURATION_MS) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val totalMagnitude = sqrt(x * x + y * y + z * z)
        val netAcceleration = kotlin.math.abs(totalMagnitude - GRAVITY)

        if (netAcceleration >= ACCEL_THRESHOLD) {
            if (now - lastSpikeTime >= MIN_SPIKE_INTERVAL_MS) {
                lastSpikeTime = now

                // Clean up timestamps older than sliding window
                var validSpikes = 0
                for (i in 0 until spikeCount) {
                    if (now - spikeTimestamps[i] <= WINDOW_DURATION_MS) {
                        spikeTimestamps[validSpikes++] = spikeTimestamps[i]
                    }
                }
                spikeCount = validSpikes

                if (spikeCount < REQUIRED_SPIKE_COUNT) {
                    spikeTimestamps[spikeCount++] = now
                }

                if (spikeCount >= REQUIRED_SPIKE_COUNT) {
                    Log.w(TAG, "🚨 Emergency shake detected! Triggering SOS callback.")
                    lastTriggerTime = now
                    spikeCount = 0
                    onShakeDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
