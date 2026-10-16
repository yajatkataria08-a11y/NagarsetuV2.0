package com.nagarsetu.dashboard.data.service

import com.nagarsetu.dashboard.domain.model.AlertSeverity
import com.nagarsetu.dashboard.domain.model.AlertType
import com.nagarsetu.dashboard.domain.model.CrisisLevel
import com.nagarsetu.dashboard.domain.model.DashboardAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits periodic live civic alerts simulating a real-time WebSocket feed.
 * In production: replace the `flow` body with a Ktor/OkHttp SSE stream.
 */
@Singleton
class LiveAlertService @Inject constructor() {

    // Simulated alert pool — easily replaced with backend SSE
    private val alertTemplates = listOf(
        Triple("Water Pressure Drop", "Low pressure reported in Arera Colony sector 4", AlertType.CIVIC),
        Triple("Traffic Snarl", "Heavy congestion on Hoshangabad Road near Board Office", AlertType.TRAFFIC),
        Triple("Pothole Alert", "New pothole cluster reported on Kolar Road", AlertType.HAZARD),
        Triple("Health Advisory", "Dengue hotspot flagged in Govindpura ward", AlertType.HEALTH),
        Triple("SLA Breach", "Ward W03 has exceeded 72h SLA for 3 complaints", AlertType.SLA_BREACH),
        Triple("Geofence Event", "Emergency vehicle entered Berasia corridor", AlertType.GEOFENCE_ENTRY),
        Triple("Budget Utilization", "Indrapuri ward at 91% budget utilization", AlertType.CIVIC),
        Triple("Crime Alert", "Elevated night patrol activated near New Market", AlertType.HAZARD)
    )

    private val wards = listOf(
        "Berasia", "Kothakhaira", "Chunar Ganj", "Ayodhya Nagar", "Govindpura",
        "Indrapuri", "Arera Colony", "MP Nagar", "Shahpura", "Piplani",
        "Bairagarh", "TT Nagar", "Kolar Road"
    )

    /** Continuous SharedFlow-compatible alert stream (30s interval). */
    fun alertStream(): Flow<DashboardAlert> = flow {
        var tick = 0
        while (true) {
            delay(30_000L)
            val (title, msg, type) = alertTemplates[tick % alertTemplates.size]
            emit(
                DashboardAlert(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    message = msg,
                    type = type,
                    ward = wards.random(),
                    timestamp = System.currentTimeMillis(),
                    severity = when (type) {
                        AlertType.SLA_BREACH, AlertType.NEW_CRISIS -> AlertSeverity.CRITICAL
                        AlertType.HAZARD, AlertType.HEALTH -> AlertSeverity.WARNING
                        else -> AlertSeverity.INFO
                    }
                )
            )
            tick++
        }
    }

    /** Compute crisis level from recent alert density. */
    fun computeCrisisLevel(recentAlerts: List<DashboardAlert>): CrisisLevel {
        val criticalCount = recentAlerts.count { it.severity == AlertSeverity.CRITICAL }
        val warnCount = recentAlerts.count { it.severity == AlertSeverity.WARNING }
        return when {
            criticalCount >= 3 -> CrisisLevel.EMERGENCY
            criticalCount >= 1 || warnCount >= 4 -> CrisisLevel.CRITICAL
            warnCount >= 2 -> CrisisLevel.ELEVATED
            else -> CrisisLevel.NORMAL
        }
    }
}
