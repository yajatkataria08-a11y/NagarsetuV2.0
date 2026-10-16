package com.nagarsetu.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nagarsetu.core.ui.theme.BHOPAL_LAT
import com.nagarsetu.core.ui.theme.BHOPAL_LNG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized GPS / fused-location provider for NagarSetu.
 *
 * All features (ChargeUp, ParkEase, GreenRoute, HealthWatch, RoadWatch, Raksha, Dashboard)
 * share this single instance rather than duplicating location boilerplate.
 *
 * Defaults to Bhopal city centre (23.2599°N, 77.4126°E) when GPS is unavailable,
 * permission is denied, or before the first fix arrives.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Bhopal centre as default until real GPS fix arrives
    val defaultLat: Double = BHOPAL_LAT
    val defaultLng: Double = BHOPAL_LNG

    private val _currentLocation = MutableStateFlow<LatLngPoint>(
        LatLngPoint(defaultLat, defaultLng, isGpsFix = false)
    )
    val currentLocation: StateFlow<LatLngPoint> = _currentLocation.asStateFlow()

    /** One-shot: get the last known location or Bhopal centre as fallback. */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): LatLngPoint {
        return runCatching {
            val loc: Location? = fusedClient.lastLocation.await()
            if (loc != null) {
                val point = LatLngPoint(loc.latitude, loc.longitude, isGpsFix = true, speedMps = loc.speed)
                _currentLocation.value = point
                point
            } else {
                LatLngPoint(defaultLat, defaultLng, isGpsFix = false)
            }
        }.getOrDefault(LatLngPoint(defaultLat, defaultLng, isGpsFix = false))
    }

    /**
     * Live location updates as a Flow. Emits whenever the device moves.
     * Accuracy: BALANCED_POWER (≈ 100 m), updates every 10 s / 50 m minimum.
     * Automatically cancels the callback when the Flow collector is gone.
     */
    @SuppressLint("MissingPermission")
    fun locationFlow(
        intervalMs: Long = 10_000L,
        minDistanceM: Float = 50f
    ): Flow<LatLngPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val point = LatLngPoint(loc.latitude, loc.longitude, isGpsFix = true, speedMps = loc.speed)
                    _currentLocation.value = point
                    trySend(point)
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    /** High-accuracy flow for active navigation — 2s / 5m updates. */
    @SuppressLint("MissingPermission")
    fun navigationLocationFlow(): Flow<LatLngPoint> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2_000L
        )
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val point = LatLngPoint(loc.latitude, loc.longitude, isGpsFix = true, speedMps = loc.speed)
                    _currentLocation.value = point
                    trySend(point)
                }
            }
        }
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}

/**
 * Thin wrapper around a lat/lng pair so callers can distinguish a real GPS fix
 * (isGpsFix = true) from the Bhopal-centre default (isGpsFix = false).
 */
data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val isGpsFix: Boolean = false,
    val speedMps: Float = 0f
)
