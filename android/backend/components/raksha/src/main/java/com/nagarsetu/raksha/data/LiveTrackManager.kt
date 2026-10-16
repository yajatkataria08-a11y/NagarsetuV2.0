package com.nagarsetu.raksha.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nagarsetu.firebase.realtime.GpsPoint
import com.nagarsetu.firebase.realtime.GpsTrackingRepository
import com.nagarsetu.firebase.realtime.SessionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Raksha "LiveTrack" session lifecycle.
 *
 * Hybrid model role:
 *  - Uses **Google's FusedLocationProviderClient** for GPS fixes (foreground).
 *  - Writes each fix to **Firebase Realtime Database** via [GpsTrackingRepository]
 *    so trusted contacts observe the live path in < 500 ms.
 *  - When the session ends, the caller is responsible for persisting a summary
 *    row to Supabase `live_sessions` for audit via [persistSessionToSupabase].
 *
 * Usage (from RakshaViewModel):
 * ```kotlin
 * val sessionId = liveTrackManager.startTracking(uid = profile.uid)
 * // share sessionId link to trusted contacts
 * liveTrackManager.stopTracking()
 * ```
 *
 * Observers (trusted contacts' devices) use:
 * ```kotlin
 * gpsTrackingRepository.observeLatestLocation(sessionId).collect { point -> ... }
 * ```
 */
@Singleton
class LiveTrackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gpsTrackingRepository: GpsTrackingRepository
) {
    companion object {
        private const val TAG              = "LiveTrackManager"
        private const val GPS_INTERVAL_MS  = 5_000L     // update every 5 s
        private const val GPS_FASTEST_MS   = 2_000L     // at most every 2 s
        private const val SESSION_DURATION = 3_600_000L // default 1 hour
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var activeSessionId: String? = null
    private var locationCallback: LocationCallback? = null

    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    // ── Start / Stop ───────────────────────────────────────────────────────

    /**
     * Starts a Firebase RTDB session and begins forwarding GPS fixes.
     * Requires [Manifest.permission.ACCESS_FINE_LOCATION].
     *
     * @param ownerUid  Supabase uid of the user sharing their location.
     * @return The Firebase session ID (share this URL to trusted contacts).
     */
    suspend fun startTracking(ownerUid: String): String? {
        if (_trackingState.value is TrackingState.Active) {
            Log.w(TAG, "Already tracking — ignoring startTracking()")
            return activeSessionId
        }

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            _trackingState.value = TrackingState.Error("Location permission required")
            return null
        }

        val sessionId = runCatching {
            gpsTrackingRepository.startSession(ownerUid, SESSION_DURATION)
        }.getOrElse { e ->
            Log.e(TAG, "Failed to create Firebase session: ${e.message}", e)
            _trackingState.value = TrackingState.Error("Could not start session: ${e.message}")
            return null
        }

        activeSessionId = sessionId
        _trackingState.value = TrackingState.Active(sessionId)
        Log.i(TAG, "LiveTrack started: sessionId=$sessionId")

        startLocationUpdates(sessionId)
        return sessionId
    }

    /**
     * Stops GPS updates and marks the Firebase session as inactive.
     */
    fun stopTracking() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null

        scope.launch {
            activeSessionId?.let { id ->
                gpsTrackingRepository.endSession(id)
                Log.i(TAG, "LiveTrack ended: sessionId=$id")
            }
        }
        activeSessionId = null
        _trackingState.value = TrackingState.Idle
    }

    /**
     * Returns the real-time GPS flow for any active session.
     * Safe to call from RakshaViewModel on the observer (trusted contact) side.
     */
    fun observeSession(sessionId: String): Flow<GpsPoint?> =
        gpsTrackingRepository.observeLatestLocation(sessionId)

    suspend fun getSessionInfo(sessionId: String): SessionInfo? =
        gpsTrackingRepository.getSessionInfo(sessionId)

    // ── Internal helpers ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")   // Permission checked in startTracking()
    private fun startLocationUpdates(sessionId: String) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_INTERVAL_MS)
            .setMinUpdateIntervalMillis(GPS_FASTEST_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    onNewLocation(sessionId, loc)
                }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun onNewLocation(sessionId: String, location: Location) {
        scope.launch {
            val ok = gpsTrackingRepository.pushLocation(
                sessionId    = sessionId,
                lat          = location.latitude,
                lng          = location.longitude,
                accuracyMeters = location.accuracy
            )
            if (!ok) {
                Log.w(TAG, "GPS push failed for session=$sessionId — RTDB offline?")
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

// ── State sealed class ─────────────────────────────────────────────────────

sealed class TrackingState {
    object Idle : TrackingState()
    data class Active(val sessionId: String) : TrackingState()
    data class Error(val message: String) : TrackingState()
}
