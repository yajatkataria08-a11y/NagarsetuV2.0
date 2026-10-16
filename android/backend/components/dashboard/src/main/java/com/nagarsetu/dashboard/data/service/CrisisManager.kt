package com.nagarsetu.dashboard.data.service

import com.nagarsetu.dashboard.domain.model.CrisisLevel
import com.nagarsetu.dashboard.domain.model.DashboardAlert
import com.nagarsetu.dashboard.domain.model.RadarAlert
import com.nagarsetu.dashboard.domain.model.AlertType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Central crisis orchestrator.
 * Tracks crisis level, proximity radar alerts, and deep-link crisis params.
 */
@Singleton
class CrisisManager @Inject constructor(
    private val alertService: LiveAlertService
) {
    private val _crisisLevel = MutableStateFlow(CrisisLevel.NORMAL)
    val crisisLevel: StateFlow<CrisisLevel> = _crisisLevel.asStateFlow()

    private val _radarAlerts = MutableStateFlow<List<RadarAlert>>(seedRadarAlerts())
    val radarAlerts: StateFlow<List<RadarAlert>> = _radarAlerts.asStateFlow()

    fun updateFromAlerts(alerts: List<DashboardAlert>) {
        _crisisLevel.value = alertService.computeCrisisLevel(alerts)
    }

    fun applyCrisisDeepLink(crisis: String?) {
        _crisisLevel.value = when (crisis?.lowercase()) {
            "high", "critical" -> CrisisLevel.CRITICAL
            "emergency"        -> CrisisLevel.EMERGENCY
            "elevated"         -> CrisisLevel.ELEVATED
            else               -> CrisisLevel.NORMAL
        }
    }

    /** Compute radar bearing from user's position to an alert point. */
    fun computeBearing(userLat: Double, userLng: Double, targetLat: Double, targetLng: Double): Float {
        val dLng = Math.toRadians(targetLng - userLng)
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(targetLat)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    /** Haversine distance in metres. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        return (R * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    private fun seedRadarAlerts() = listOf(
        RadarAlert("r1", "Pothole cluster",   320, 45f,  AlertType.HAZARD),
        RadarAlert("r2", "Water main break",  850, 120f, AlertType.CIVIC),
        RadarAlert("r3", "Traffic accident",  1_200, 210f, AlertType.TRAFFIC),
        RadarAlert("r4", "Health advisory",   650, 315f, AlertType.HEALTH)
    )
}
