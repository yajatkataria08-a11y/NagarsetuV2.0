package com.nagarsetu.raksha.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.nagarsetu.raksha.data.wearos.WatchSosManager
import dagger.hilt.android.AndroidEntryPoint
import com.nagarsetu.raksha.data.incident.IncidentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SosWearListenerService
 *
 * Receives Wear OS messages from a paired WearOS companion app and forwards
 * them to [WatchSosManager].
 */
@AndroidEntryPoint
class SosWearListenerService : WearableListenerService() {

    @Inject lateinit var watchSosManager: WatchSosManager
    @Inject lateinit var incidentRepository: IncidentRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SosWearListenerService"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.e(TAG, "!!! WEARABLE MESSAGE RECEIVED !!! Path: ${messageEvent.path}")
        
        when (messageEvent.path) {
            WatchSosManager.SOS_PATH -> {
                Log.w(TAG, "🚨 SOS received from WearOS node")
                
                // 1. Notify the UI if it's alive
                watchSosManager.simulateWatchSos()
                
                // 2. Trigger Background SOS directly (Fail-safe)
                serviceScope.launch {
                    try {
                        incidentRepository.submitSos(
                            latitude = 0.0, // We'll need to fetch location here for better precision
                            longitude = 0.0, 
                            contact = "WATCH TRIGGERED SOS",
                            userId = "watch_user"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Background SOS submission failed: ${e.message}")
                    }
                }

                watchSosManager.sendAcknowledgement()
            }
            WatchSosManager.HEARTBEAT_PATH -> {
                val bpm = messageEvent.data
                    ?.let { runCatching { String(it).trim().toInt() }.getOrNull() }
                    ?: 0
                Log.d(TAG, "Heartbeat received: $bpm bpm")
                watchSosManager.onHeartRateUpdate(bpm)
            }
            else -> Log.d(TAG, "Unhandled path: ${messageEvent.path}")
        }
    }
}
