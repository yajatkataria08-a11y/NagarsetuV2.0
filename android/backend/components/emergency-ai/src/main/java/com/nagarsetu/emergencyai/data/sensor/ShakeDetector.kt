package com.nagarsetu.emergencyai.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Debounced shake detector.
 * Fires [onShake] only when 3 shakes are detected within 1500ms,
 * then enforces a 3s cooldown before it can fire again.
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val SHAKE_THRESHOLD_G  = 2.5f       // 2.5g — deliberate shake, ignores walking
    private val SHAKE_COUNT_NEEDED = 3           // require 3 shakes
    private val SHAKE_WINDOW_MS    = 1_500L      // within 1.5s
    private val COOLDOWN_MS        = 3_000L      // 3s between triggers

    private var shakeCount    = 0
    private var windowStart   = 0L
    private var lastTriggered = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        if (gForce > SHAKE_THRESHOLD_G) {
            val now = System.currentTimeMillis()

            // Reset window if too much time has passed since first shake
            if (now - windowStart > SHAKE_WINDOW_MS) {
                shakeCount = 0
                windowStart = now
            }

            shakeCount++

            if (shakeCount >= SHAKE_COUNT_NEEDED && now - lastTriggered > COOLDOWN_MS) {
                lastTriggered = now
                shakeCount = 0
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
