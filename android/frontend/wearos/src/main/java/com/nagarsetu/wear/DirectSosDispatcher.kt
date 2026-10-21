package com.nagarsetu.wear

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * NagarSetu V2.0 — Standalone Wear OS Cloud-Direct SOS & Fall-Detection Bridge (Phase 9)
 * ─────────────────────────────────────────────────────────────────────────────────────
 * When paired phone is connected: Uses Wearable Data Layer API (/raksha/sos).
 * When phone is disconnected: Falls back to direct HTTP transmission over smartwatch Wi-Fi/LTE.
 */
class DirectSosDispatcher(private val context: Context) {

    companion object {
        private const val TAG = "DirectSosDispatcher"
        private const val DEFAULT_BACKEND_URL = "http://10.0.2.2:4000/api/sos"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Dispatch SOS with biometric vitals and physical struggle / fall indicator
     */
    suspend fun dispatchSos(
        heartRate: Int = 112,
        fallDetected: Boolean = false,
        batteryPct: Int = 85,
        lat: Double = 23.2599,
        lng: Double = 77.4126
    ): Boolean = withContext(Dispatchers.IO) {
        val phoneNodes = getConnectedPhoneNodes()

        if (phoneNodes.isNotEmpty()) {
            // Mode 1: Bluetooth Data Layer to Phone
            Log.i(TAG, "Phone connected. Relaying SOS via Bluetooth Data Layer.")
            val payload = JSONObject().apply {
                put("heart_rate", heartRate)
                put("fall_detected", fallDetected)
                put("battery", batteryPct)
                put("lat", lat)
                put("lng", lng)
                put("timestamp", System.currentTimeMillis())
            }.toString().toByteArray()

            val msgClient = Wearable.getMessageClient(context)
            var success = false
            for (node in phoneNodes) {
                try {
                    msgClient.sendMessage(node.id, "/raksha/sos", payload).await()
                    success = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed sending to node ${node.displayName}: ${e.message}")
                }
            }
            if (success) {
                triggerHapticConfirmation()
                return@withContext true
            }
        }

        // Mode 2: Standalone Direct Wi-Fi / LTE Cloud Dispatch
        Log.i(TAG, "No phone reachable. Initiating Standalone Wear OS Cloud-Direct HTTP Dispatch.")
        try {
            val jsonPayload = JSONObject().apply {
                put("userId", "wear_user_${Build.MODEL}")
                put("userName", "Citizen (Smartwatch)")
                put("ward", "Bhopal Smart Safety Grid")
                put("lat", lat)
                put("lng", lng)
                put("batteryLevel", batteryPct)
                put("heartRate", heartRate)
                put("sirenType", if (fallDetected || heartRate > 120) "CRITICAL" else "HIGH")
                put("source", "wearos-direct-lte")
                put("fallDetected", fallDetected)
            }.toString()

            val request = Request.Builder()
                .url(DEFAULT_BACKEND_URL)
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Standalone Wear OS SOS received by NagarSetu Gateway! HTTP ${response.code}")
                    triggerHapticConfirmation()
                    return@withContext true
                } else {
                    Log.e(TAG, "Gateway rejected Wear OS SOS: HTTP ${response.code}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Standalone Cloud Dispatch error: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun getConnectedPhoneNodes(): List<Node> {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            nodeClient.connectedNodes.await().filter { !it.isNearby || it.displayName.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Dual-pulse waveform haptic feedback confirmation on successful emergency dispatch
     */
    private fun triggerHapticConfirmation() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 250), -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 80, 250), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic trigger notice: ${e.message}")
        }
    }
}
