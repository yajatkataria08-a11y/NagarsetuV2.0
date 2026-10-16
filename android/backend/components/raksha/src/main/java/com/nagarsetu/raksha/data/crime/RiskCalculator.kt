package com.nagarsetu.raksha.data.crime

import com.nagarsetu.raksha.data.incident.IncidentReport
import com.nagarsetu.raksha.data.routing.GeoUtils
import com.nagarsetu.raksha.data.routing.LatLonPoint
import com.nagarsetu.raksha.domain.model.HazardZone
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Computes risk scores by summing time-weighted crime points within a radius
 * and blending in live crowd-sourced incident reports.
 *
 * Improvements over v1:
 *  - [crowdReports] is now protected by a [ReentrantReadWriteLock]; concurrent
 *    coroutines calling [riskAt] while [setCrowdReports] updates the list no
 *    longer race.
 *  - Bounding-box longitude correction factor (cos(lat)) is pre-computed once
 *    per [riskAt] call instead of once per crime point in the inner loop.
 *  - [highRiskThreshold] and [DEFAULT_RADIUS_METERS] are named constants.
 *  - [allWeightedPoints] is now consistent with thread-safety guarantees.
 *  - Crowd-report severity is validated to [1, 5] on ingestion, not on every
 *    risk evaluation.
 */
class RiskCalculator(private val crimes: List<CrimePoint>) {

    companion object {
        private const val HIGH_RISK_THRESHOLD   = 12.0
        private const val DEFAULT_RADIUS_METERS = 250.0
        /** Weight applied to each crowd-report severity point in the risk sum. */
        private const val CROWD_REPORT_WEIGHT   = 4.0
    }

    private val lock = ReentrantReadWriteLock()

    /** Verified and severity-clamped crowd reports. Protected by [lock]. */
    private var crowdReports: List<CrimePoint> = emptyList()

    // ── Crowd-report ingestion ────────────────────────────────────────────────

    /**
     * Replaces the crowd-report overlay used in risk calculations.
     * Only "verified" reports are accepted; severity is clamped to [1, 5].
     * Thread-safe: safe to call from any coroutine dispatcher.
     */
    fun setCrowdReports(reports: List<IncidentReport>) {
        val converted = reports
            .filter { it.status == "verified" }
            .map { report ->
                val weight = report.severity.coerceIn(1, 5) * CROWD_REPORT_WEIGHT
                CrimePoint(
                    latitude                  = report.latitude,
                    longitude                 = report.longitude,
                    timeWeightedRiskOverride = weight
                )
            }
        lock.write { crowdReports = converted }
    }

    // ── Risk evaluation ───────────────────────────────────────────────────────

    fun riskAt(
        lat: Double,
        lng: Double,
        radius: Double = DEFAULT_RADIUS_METERS,
        hazards: List<HazardZone> = emptyList()
    ): Double {
        var totalRisk = baseRiskAt(LatLonPoint(lat, lng), radius)
        for (hazard in hazards) {
            val dist = GeoUtils.distanceMeters(lat, lng, hazard.lat, hazard.lng)
            if (dist <= hazard.radiusM) totalRisk = maxOf(totalRisk, hazard.risk)
        }
        return totalRisk
    }

    fun riskAt(
        location: LatLonPoint,
        radius: Double = DEFAULT_RADIUS_METERS,
        hazards: List<HazardZone> = emptyList()
    ): Double = riskAt(location.lat, location.lng, radius, hazards)

    fun isHighRisk(location: LatLonPoint, hazards: List<HazardZone> = emptyList()): Boolean =
        riskAt(location, hazards = hazards) >= HIGH_RISK_THRESHOLD

    /**
     * Aggregate risk along a polyline, sampled at [sampleEveryMeters] intervals.
     */
    fun routeRisk(
        points: List<LatLonPoint>,
        hazards: List<HazardZone> = emptyList(),
        sampleEveryMeters: Double = 100.0
    ): Double {
        val samples = GeoUtils.samplePolyline(points, sampleEveryMeters)
        return samples.sumOf { riskAt(it, DEFAULT_RADIUS_METERS, hazards) }
    }

    // ── Heatmap helpers ───────────────────────────────────────────────────────

    /** Returns the highest-risk crime points for heatmap rendering. */
    fun riskyPoints(limit: Int = 800): List<Pair<LatLonPoint, Double>> =
        crimes.asSequence()
            .filter { it.timeWeightedRisk > 0 }
            .map { it.latLon to it.timeWeightedRisk }
            .sortedByDescending { it.second }
            .take(limit)
            .toList()

    fun allWeightedPoints(): List<Pair<LatLonPoint, Double>> {
        val crimePoints = crimes
            .filter { it.timeWeightedRisk > 0 }
            .map { it.latLon to it.timeWeightedRisk }

        val reportPoints = lock.read {
            crowdReports.map { it.latLon to it.timeWeightedRisk }
        }

        return crimePoints + reportPoints
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Sums time-weighted risk for all crime/crowd points within [radius] metres
     * of [location].
     *
     * Bounding-box pre-filter rejects points outside an approximate rectangle
     * before the more expensive Haversine check. The longitude correction factor
     * (cosine of latitude) is pre-computed once per call.
     */
    private fun baseRiskAt(location: LatLonPoint, radius: Double): Double {
        val degLat = radius / 111_000.0
        val cosLat = Math.cos(Math.toRadians(location.lat))   // pre-computed once
        val degLng = if (cosLat > 1e-9) radius / (111_000.0 * cosLat) else radius / 111_000.0

        val minLat = location.lat - degLat;  val maxLat = location.lat + degLat
        val minLng = location.lng - degLng;  val maxLng = location.lng + degLng

        var sum = 0.0

        for (c in crimes) {
            if (c.timeWeightedRisk == 0.0) continue
            if (c.latitude < minLat || c.latitude > maxLat ||
                c.longitude < minLng || c.longitude > maxLng) continue
            if (GeoUtils.distanceMeters(location, c.latLon) <= radius) sum += c.timeWeightedRisk
        }

        lock.read {
            for (r in crowdReports) {
                if (r.latitude < minLat || r.latitude > maxLat ||
                    r.longitude < minLng || r.longitude > maxLng) continue
                if (GeoUtils.distanceMeters(location, r.latLon) <= radius) sum += r.timeWeightedRisk
            }
        }

        return sum
    }
}
