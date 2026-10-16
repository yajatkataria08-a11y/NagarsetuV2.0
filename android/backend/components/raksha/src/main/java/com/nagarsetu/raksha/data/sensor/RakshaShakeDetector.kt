package com.nagarsetu.raksha.data.sensor

import android.content.Context
import com.nagarsetu.emergencyai.data.sensor.ShakeDetector

/**
 * Fix #7: Previously a full duplicate of [ShakeDetector] (same 2.5g threshold,
 * 3-shake window, 3s cooldown), causing two concurrent accelerometer listeners
 * when both Emergency and Raksha screens were active simultaneously.
 *
 * Now delegates entirely to [ShakeDetector] — one listener, one code path.
 * RakshaViewModel continues to use this class by name; only the implementation
 * changed.
 */
class RakshaShakeDetector(
    context: Context,
    onShake: () -> Unit
) {
    private val delegate = ShakeDetector(context, onShake)

    fun start() = delegate.start()
    fun stop()  = delegate.stop()
}
