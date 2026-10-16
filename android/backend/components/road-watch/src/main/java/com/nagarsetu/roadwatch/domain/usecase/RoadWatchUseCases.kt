package com.nagarsetu.roadwatch.domain.usecase

import com.nagarsetu.roadwatch.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Calculates SLA deadline and remaining time for a given report.
 */
@Singleton
class CalculateSlaUseCase @Inject constructor() {

    fun execute(report: RoadReport): SlaInfo {
        val nowMs = System.currentTimeMillis()
        val remainingMs = report.slaDeadline - nowMs
        val isBreached = remainingMs <= 0

        val urgencyLabel = when {
            isBreached -> "⛔ SLA Breached"
            remainingMs < 3_600_000 -> "🔴 < 1 hour"
            remainingMs < 86_400_000 -> "🟠 < 1 day"
            remainingMs < 2 * 86_400_000L -> "🟡 < 2 days"
            else -> "🟢 On track"
        }

        return SlaInfo(
            deadlineMs = report.slaDeadline,
            remainingMs = remainingMs.coerceAtLeast(0),
            isBreached = isBreached,
            urgencyLabel = urgencyLabel,
            slaDays = report.severity.slaDays
        )
    }

    /** Generate SLA deadline from report creation timestamp */
    fun deadlineFor(severity: Severity, fromMs: Long = System.currentTimeMillis()): Long {
        val slaDays = severity.slaDays.coerceAtLeast(1)
        return fromMs + slaDays * 86_400_000L
    }
}

/**
 * Determines if a report should be escalated and to which authority level.
 */
@Singleton
class EscalationUseCase @Inject constructor(
    private val slaUseCase: CalculateSlaUseCase
) {
    fun shouldEscalate(report: RoadReport): Boolean {
        val sla = slaUseCase.execute(report)
        return sla.isBreached && report.status != ReportStatus.RESOLVED
    }

    fun nextAuthority(current: AuthorityLevel): AuthorityLevel? {
        val next = current.escalatesTo ?: return null
        return AuthorityLevel.entries.find { it.name == next }
    }

    fun escalate(report: RoadReport): RoadReport {
        val next = nextAuthority(report.authorityLevel) ?: report.authorityLevel
        return report.copy(
            authorityLevel = next,
            status = ReportStatus.ESCALATED,
            escalationCount = report.escalationCount + 1,
            // Extend SLA by 24h after escalation
            slaDeadline = System.currentTimeMillis() + 86_400_000L
        )
    }
}

/**
 * Clusters nearby pothole reports for heatmap display.
 * Uses a simple grid-based grouping (extensible to k-means).
 */
@Singleton
class ClusterReportsUseCase @Inject constructor() {

    fun execute(reports: List<RoadReport>, gridSizeDeg: Double = 0.005): List<PotholeCluster> {
        val grid = mutableMapOf<Pair<Int, Int>, MutableList<RoadReport>>()

        reports.forEach { report ->
            val gx = (report.latitude / gridSizeDeg).toInt()
            val gy = (report.longitude / gridSizeDeg).toInt()
            grid.getOrPut(gx to gy) { mutableListOf() }.add(report)
        }

        return grid.map { (key, bucket) ->
            val centerLat = bucket.sumOf { it.latitude } / bucket.size
            val centerLng = bucket.sumOf { it.longitude } / bucket.size
            val dominant = bucket.maxByOrNull { it.severity.ordinal }?.severity ?: Severity.LOW
            PotholeCluster(centerLat, centerLng, bucket.size, dominant, bucket)
        }
    }
}

/**
 * Filters nearby reports by distance (Haversine).
 */
@Singleton
class NearbyReportsUseCase @Inject constructor() {

    fun execute(
        all: List<RoadReport>,
        userLat: Double,
        userLng: Double,
        radiusKm: Double = 2.0
    ): List<Pair<RoadReport, Double>> {
        return all
            .map { report ->
                val dist = haversineKm(userLat, userLng, report.latitude, report.longitude)
                report to dist
            }
            .filter { (_, dist) -> dist <= radiusKm }
            .sortedBy { (_, dist) -> dist }
    }

    private fun haversineKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2).let { it * it }
        return r * 2 * Math.atan2(sqrt(a), sqrt(1 - a))
    }
}
