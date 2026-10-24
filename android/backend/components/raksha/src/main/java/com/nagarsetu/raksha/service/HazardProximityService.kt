package com.nagarsetu.raksha.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.nagarsetu.raksha.data.incident.HazardZoneFetcher
import com.nagarsetu.raksha.domain.model.HazardZone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time geofence proximity evaluation service with Voice (TTS) & heads-up alerts.
 * Periodically measures citizen distance against active municipal hazard zones.
 * Triggers spoken voice warnings and haptic sirens when approaching dangerous perimeters.
 */
@AndroidEntryPoint
class HazardProximityService : Service(), TextToSpeech.OnInitListener {

    @Inject lateinit var hazardZoneFetcher: HazardZoneFetcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Cache of recent hazard warnings to debounce alerts (Zone ID -> Timestamp)
    private val alertCooldownMap = ConcurrentHashMap<Int, Long>()

    companion object {
        private const val TAG = "HazardProximityService"
        private const val CHANNEL_ID = "nagarsetu_hazard_proximity_channel"
        private const val CHANNEL_NAME = "Hazard Proximity Warnings"
        private const val PROXIMITY_BUFFER_METERS = 150.0 // Danger buffer beyond hazard radius
        private const val ALERT_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes debounce

        fun start(context: Context) {
            val intent = Intent(context, HazardProximityService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HazardProximityService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tts = TextToSpeech(this, this)
        createNotificationChannel()

        // Start background geofence polling loop
        serviceScope.launch {
            while (isActive) {
                evaluateProximityToHazards()
                delay(15_000L) // Poll every 15 seconds
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("NagarSetu Hazard Geofencing Active")
            .setContentText("Proximity radar scanning for civic hazards & safe corridors.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(9010, notification)
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(0.95f)
            isTtsReady = true
            Log.i(TAG, "TextToSpeech initialized successfully.")
        } else {
            Log.w(TAG, "TextToSpeech initialization failed: $status")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun evaluateProximityToHazards() {
        try {
            val cts = CancellationTokenSource()
            val location = fusedLocationClient?.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            )?.await() ?: return

            val activeZones = hazardZoneFetcher.fetchActiveZones()
            if (activeZones.isEmpty()) return

            val now = System.currentTimeMillis()

            for (zone in activeZones) {
                val distance = calculateHaversineDistanceMeters(
                    location.latitude,
                    location.longitude,
                    zone.lat,
                    zone.lng
                )

                val dangerThreshold = zone.radiusM + PROXIMITY_BUFFER_METERS
                if (distance <= dangerThreshold) {
                    val lastAlertTime = alertCooldownMap[zone.id] ?: 0L
                    if (now - lastAlertTime > ALERT_COOLDOWN_MS) {
                        alertCooldownMap[zone.id] = now
                        triggerHazardAlert(zone, distance.toInt())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating hazard proximity: ${e.message}")
        }
    }

    private fun triggerHazardAlert(zone: HazardZone, distanceMeters: Int) {
        val warningMessage = "Warning: Approaching hazard area within $distanceMeters meters. Safe corridor guidance recommended."
        Log.w(TAG, "🚨 HAZARD PROXIMITY ALERT: Zone #${zone.id} (Distance: ${distanceMeters}m)")

        // 1. Spoken Audio Alert via TTS
        if (isTtsReady && tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(warningMessage, TextToSpeech.QUEUE_FLUSH, null, "hazard_${zone.id}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(warningMessage, TextToSpeech.QUEUE_FLUSH, null)
            }
        }

        // 2. Custom Siren Haptic Feedback
        triggerSirenVibration()

        // 3. Heads-up Notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ DANGER ZONE PROXIMITY ALERT")
            .setContentText(warningMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "You are within $distanceMeters meters of an active hazard zone.\n" +
                "Risk Multiplier: ${zone.risk}x\n" +
                "Recommended Action: Divert to designated green corridor."
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        nm.notify(10000 + zone.id, notification)
    }

    private fun triggerSirenVibration() {
        try {
            val timings = longArrayOf(0, 300, 150, 300, 150, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Siren vibration failed: ${e.message}")
        }
    }

    /**
     * Calculates great-circle distance between two GPS coordinates using Haversine formula.
     */
    private fun calculateHaversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for civic danger zones and safety corridors"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
