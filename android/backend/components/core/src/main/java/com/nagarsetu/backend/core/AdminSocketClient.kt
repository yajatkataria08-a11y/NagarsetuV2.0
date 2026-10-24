package com.nagarsetu.backend.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nagarsetu.core.data.local.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminSocketClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private var socket: Socket? = null

    companion object {
        private const val TAG = "AdminSocketClient"
        private const val CHANNEL_ID = "nagarsetu_civic"
        private const val CHANNEL_NAME = "Civic Alerts"
    }

    fun connect(serverUrl: String) {
        if (socket?.connected() == true) return
        try {
            Log.i(TAG, "Connecting to admin server: $serverUrl")
            val token = preferencesManager.getAuthToken() ?: "nagarsetu_citizen_token"
            
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 3000
                auth = mapOf("token" to token)
                query = "token=$token"
            }
            socket = IO.socket(serverUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "Connected to admin server via Socket.IO")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connection error: ${args?.firstOrNull()}")
            }

            // 1. General Civic Broadcasts
            socket?.on("civic_broadcast") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val title = data.optString("title", "Civic Alert")
                val message = data.optString("message", "")
                val ward = data.optString("ward", "")
                showNotification(title, if (ward.isNotBlank()) "[$ward] $message" else message)
                Log.i(TAG, "Civic broadcast received: $title")
            }

            // 2. Real-Time Hazard Zones (Manual or AI Sensor Triggered)
            val handleHazard = { args: Array<Any>? ->
                val data = args?.firstOrNull() as? JSONObject
                if (data != null) {
                    val name = data.optString("name", "Active Danger Zone")
                    val ward = data.optString("ward", "Local Area")
                    val risk = data.optDouble("risk", 1.5)
                    val title = "⚠️ New Hazard Zone: $name"
                    val message = "Ward: $ward | Risk Multiplier: ${risk}x. Safe corridor active."
                    showNotification(title, message)
                    Log.i(TAG, "Hazard event received: $name in $ward")
                }
            }
            socket?.on("hazard:created") { handleHazard(it) }
            socket?.on("hazard:auto_created") { handleHazard(it) }

            // 3. SOS Event Broadcast (For nearby responders & wards)
            socket?.on("sos:created") { args ->
                val data = args?.firstOrNull() as? JSONObject ?: return@on
                val user = data.optString("userName", "Citizen")
                val ward = data.optString("ward", "")
                val triage = data.optString("triageLevel", "HIGH")
                val title = "🚨 Emergency SOS Alert ($triage)"
                val message = "Citizen $user requested emergency assistance in $ward."
                showNotification(title, message)
                Log.w(TAG, "SOS alert broadcast received: $title")
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Socket connect failed: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    private fun showNotification(title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
