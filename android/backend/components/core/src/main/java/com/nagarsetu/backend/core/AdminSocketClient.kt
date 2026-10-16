package com.nagarsetu.backend.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminSocketClient @Inject constructor(
    @ApplicationContext private val context: Context
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
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 3000
            }
            socket = IO.socket(serverUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "Connected to admin server")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connection error: ${args?.firstOrNull()}")
            }

            socket?.on("civic_broadcast") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val title = data.optString("title", "Civic Alert")
                val message = data.optString("message", "")
                val ward = data.optString("ward", "")
                showNotification(title, if (ward.isNotBlank()) "[$ward] $message" else message)
                Log.i(TAG, "Civic broadcast received: $title")
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
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(channel)

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
