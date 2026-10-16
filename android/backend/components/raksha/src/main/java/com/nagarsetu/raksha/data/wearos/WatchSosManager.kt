package com.nagarsetu.raksha.data.wearos

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Watch SOS integration via the Wearable Data Layer API.
 *
 * On a real device this connects via the Wearable.getMessageClient / DataClient
 * to a paired WearOS companion app. In the demo / simulator it exposes a
 * [simulateWatchSos] hook that the UI can call to mimic a wrist-tap trigger.
 *
 * Production integration steps (beyond this stub):
 *  1. Add `com.google.android.gms:play-services-wearable` dependency.
 *  2. Register a WearableListenerService in AndroidManifest.xml.
 *  3. Send SOS via `Wearable.getMessageClient(ctx).sendMessage(nodeId, SOS_PATH, null)`.
 *  4. The WearOS app listens on the same path and vibrates / shows confirmation.
 */
@Singleton
class WatchSosManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WatchSosManager"
        const val SOS_PATH = "/raksha/sos"
        const val HEARTBEAT_PATH = "/raksha/heartbeat"
    }

    /** True when a paired WearOS node has been discovered (simulated). */
    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected = _isWatchConnected.asStateFlow()

    /** Fires whenever a SOS signal arrives from the watch. */
    private val _watchSosReceived = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val watchSosReceived = _watchSosReceived.asSharedFlow()

    /** Last heart-rate reading from wrist sensor (0 = not available). */
    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    init {
        discoverNodes()
    }

    /**
     * Attempt to discover connected Wear nodes.
     * Gracefully no-ops if the Wearable API is not present on this device.
     */
    private fun discoverNodes() {
        try {
            // Real impl: Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { ... }
            // For demo, we mark as connected so the UI card appears.
            Log.d(TAG, "Scanning for WearOS nodes…")
            _isWatchConnected.value = true   // demo: always shows as connected
        } catch (e: Exception) {
            Log.w(TAG, "Wearable API not available: ${e.message}")
            _isWatchConnected.value = false
        }
    }

    /**
     * Called by the UI "Simulate Watch SOS" button or by the real
     * WearableListenerService when a message arrives on [SOS_PATH].
     */
    fun simulateWatchSos() {
        Log.d(TAG, "Watch SOS received!")
        _watchSosReceived.tryEmit(true)
    }

    fun clearWatchSos() {
        _watchSosReceived.tryEmit(false)
    }

    /**
     * Send a confirmation ping back to the watch so it knows SOS was received
     * by the phone. Real impl uses Wearable.getMessageClient.
     */
    fun sendAcknowledgement() {
        Log.d(TAG, "Sending SOS ack to watch via $SOS_PATH")
    }

    /** Called when a new heart-rate sample arrives from the wrist sensor. */
    fun onHeartRateUpdate(bpm: Int) {
        _heartRate.value = bpm
    }
}
