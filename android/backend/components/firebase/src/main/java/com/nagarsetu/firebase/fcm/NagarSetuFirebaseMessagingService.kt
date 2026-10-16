package com.nagarsetu.firebase.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for NagarSetu.
 *
 * Handles two scenarios in the hybrid model:
 *
 * 1. **Token refresh**: when Firebase rotates our device token we must
 *    update `profiles.fcm_token` in Supabase so future pushes reach us.
 *    [onNewToken] → [FcmTokenManager.syncTokenToSupabase] → Supabase REST.
 *
 * 2. **Incoming push**: messages originate from either:
 *    - Our backend (ward officer broadcasts via Supabase Edge Function → Firebase Admin SDK)
 *    - Firebase Console direct send (for testing / emergency broadcasts)
 *
 *    Message payload conventions:
 *    ```json
 *    {
 *      "data": {
 *        "type": "WARD_ALERT | SOS_ACK | INCIDENT_VERIFIED | CITY_BROADCAST",
 *        "title": "...",
 *        "body": "...",
 *        "ward": "42",          // optional
 *        "incident_id": "uuid", // optional
 *        "severity": "HIGH"     // optional
 *      }
 *    }
 *    ```
 *    Data-only messages are used so the app controls notification display
 *    in all foreground/background states.
 */
@AndroidEntryPoint
class NagarSetuFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NagarSetuFCM"

        // Notification channel IDs
        const val CHANNEL_WARD_ALERTS   = "ward_alerts"
        const val CHANNEL_SOS           = "sos_alerts"
        const val CHANNEL_INCIDENTS     = "incident_updates"
        const val CHANNEL_CITY_BROADCAST = "city_broadcast"

        // Message type constants (must match what the server sends)
        const val TYPE_WARD_ALERT       = "WARD_ALERT"
        const val TYPE_SOS_ACK          = "SOS_ACK"
        const val TYPE_INCIDENT_VERIFIED = "INCIDENT_VERIFIED"
        const val TYPE_CITY_BROADCAST   = "CITY_BROADCAST"
    }

    @Inject lateinit var fcmTokenManager: FcmTokenManager
    @Inject lateinit var supabaseTokenUploader: SupabaseTokenUploader

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Token lifecycle ────────────────────────────────────────────────────

    /**
     * Called when Firebase generates a new registration token.
     * Syncs the new token to Supabase profiles so the server can reach us.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed, syncing to Supabase")
        val prefs = getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("current_uid", null) ?: run {
            // Store token locally; sync will happen at next login
            prefs.edit().putString("pending_fcm_token", token).apply()
            Log.w(TAG, "No uid yet — token stored locally for deferred sync")
            return
        }
        serviceScope.launch {
            supabaseTokenUploader.upsertFcmToken(uid = uid, token = token)
        }
    }

    // ── Incoming messages ──────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM received from=${message.from} data=${message.data}")

        val data = message.data
        val type  = data["type"] ?: TYPE_CITY_BROADCAST
        val title = data["title"] ?: message.notification?.title ?: "NagarSetu"
        val body  = data["body"]  ?: message.notification?.body  ?: ""

        when (type) {
            TYPE_SOS_ACK          -> showNotification(title, body, CHANNEL_SOS,
                                        notifId = 1001, priority = NotificationCompat.PRIORITY_MAX)
            TYPE_WARD_ALERT       -> showNotification(title, body, CHANNEL_WARD_ALERTS,
                                        notifId = 2000 + (data["ward"]?.toIntOrNull() ?: 0))
            TYPE_INCIDENT_VERIFIED -> showNotification(title, body, CHANNEL_INCIDENTS,
                                        notifId = 3000)
            TYPE_CITY_BROADCAST   -> showNotification(title, body, CHANNEL_CITY_BROADCAST,
                                        notifId = 4000)
            else                  -> showNotification(title, body, CHANNEL_CITY_BROADCAST,
                                        notifId = 9000)
        }
    }

    // ── Notification helpers ───────────────────────────────────────────────

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        notifId: Int,
        priority: Int = NotificationCompat.PRIORITY_HIGH
    ) {
        ensureChannels()

        // Deep-link intent — opens NagarSetu main activity
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }

        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                this, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with R.drawable.ic_nagarsetu_notif
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification)
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            Triple(CHANNEL_WARD_ALERTS,    "Ward Alerts",          NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_SOS,            "SOS Confirmations",    NotificationManager.IMPORTANCE_MAX),
            Triple(CHANNEL_INCIDENTS,      "Incident Updates",     NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_CITY_BROADCAST, "City Broadcasts",      NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { (id, name, importance) ->
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(id, name, importance).apply {
                        enableVibration(importance >= NotificationManager.IMPORTANCE_HIGH)
                    }
                )
            }
        }
    }
}
