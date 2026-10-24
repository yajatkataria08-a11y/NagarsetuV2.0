package com.nagarsetu.raksha.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.core.data.local.PreferencesManager
import com.nagarsetu.raksha.data.incident.IncidentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Foreground emergency service managing background Shake-to-SOS monitoring
 * and rapid 1-tap SOS dispatches from tiles and widgets.
 */
@AndroidEntryPoint
class EmergencyTriggerService : Service() {

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var shakeDetector: ShakeDetector? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    companion object {
        private const val TAG = "EmergencyTriggerService"
        private const val CHANNEL_ID = "nagarsetu_emergency_channel"
        private const val CHANNEL_NAME = "NagarSetu Emergency Monitoring"
        private const val NOTIFICATION_ID = 9001
        private const val SOS_ALERT_NOTIFICATION_ID = 9002

        const val ACTION_START_MONITORING = "com.nagarsetu.emergency.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.nagarsetu.emergency.STOP_MONITORING"
        const val ACTION_TRIGGER_SOS = "com.nagarsetu.emergency.TRIGGER_SOS"

        fun startMonitoring(context: Context) {
            val intent = Intent(context, EmergencyTriggerService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun triggerSos(context: Context) {
            val intent = Intent(context, EmergencyTriggerService::class.java).apply {
                action = ACTION_TRIGGER_SOS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        shakeDetector = ShakeDetector(this) {
            Log.w(TAG, "Shake gesture detected in background. Dispatching SOS.")
            dispatchEmergencySos("Shake Gesture Triggered")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startForeground(NOTIFICATION_ID, buildMonitoringNotification())
                shakeDetector?.start()
                Log.i(TAG, "Emergency monitoring service running in foreground.")
            }
            ACTION_STOP_MONITORING -> {
                shakeDetector?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TRIGGER_SOS -> {
                startForeground(NOTIFICATION_ID, buildMonitoringNotification())
                dispatchEmergencySos("1-Tap Emergency Trigger")
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildMonitoringNotification())
                shakeDetector?.start()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun dispatchEmergencySos(reason: String) {
        serviceScope.launch {
            triggerSosHapticFeedback()

            var lat = CivicConstants.BHOPAL_LAT
            var lng = CivicConstants.BHOPAL_LNG

            try {
                val cts = CancellationTokenSource()
                val location = fusedLocationClient?.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                )?.await()

                if (location != null) {
                    lat = location.latitude
                    lng = location.longitude
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to retrieve precise GPS coordinates, using fallback: ${e.message}")
            }

            val user = preferencesManager.getUserSession()
            val userId = user?.uid ?: "emergency_user"
            val userName = user?.name?.ifBlank { "Citizen" } ?: "Citizen"
            val phone = user?.phone ?: ""
            val ward = user?.ward?.ifBlank { "Bhopal" } ?: "Bhopal"

            val sosId = incidentRepository.submitSos(
                latitude = lat,
                longitude = lng,
                contact = "[$reason] Lat: $lat, Lng: $lng",
                userId = userId,
                userName = userName,
                phone = phone,
                ward = ward,
                batteryLevel = 100,
                heartRate = 0
            )

            showSosDispatchedNotification(lat, lng, sosId)
        }
    }

    private fun triggerSosHapticFeedback() {
        try {
            // Morse code for SOS: ... --- ...
            val timings = longArrayOf(0, 150, 100, 150, 100, 150, 300, 400, 150, 400, 150, 400, 300, 150, 100, 150, 100, 150)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun buildMonitoringNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("NagarSetu Safety Shield Active")
            .setContentText("Background protection enabled. Shake phone 3x for instant SOS.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showSosDispatchedNotification(lat: Double, lng: Double, sosId: String?) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 EMERGENCY SOS BROADCASTED")
            .setContentText("Coordinates: $lat, $lng. Responders and police notified.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Emergency alert dispatched to Bhopal Municipal Control Room & Ward emergency responders.\n" +
                "Incident ID: ${sosId ?: "Queued"}\nLocation: $lat, $lng"
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        nm.notify(SOS_ALERT_NOTIFICATION_ID, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for NagarSetu emergency monitoring & SOS alerts"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector?.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
