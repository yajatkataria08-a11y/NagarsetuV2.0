package com.nagarsetu.raksha.data.crime

import com.nagarsetu.raksha.data.routing.LatLonPoint

/**
 * A single crime data record loaded from the local CSV asset.
 *
 * Risk scoring formula (from spec):
 *   baseRisk = (5 × act302) + (4 × act363) + (3 × act323) + (2 × act379)
 *   if hour >= 20: timeWeightedRisk = baseRisk × 1.5
 *
 * Ported from Raksha (com.safepath.indore.data.CrimePoint).
 */
data class CrimePoint(
    val latitude: Double,
    val longitude: Double,
    val hour: Int = 12,
    val act302: Int = 0,
    val act363: Int = 0,
    val act323: Int = 0,
    val act379: Int = 0,
    val timeWeightedRiskOverride: Double? = null
) {
    val latLon: LatLonPoint by lazy { LatLonPoint(latitude, longitude) }

    val baseRisk: Double =
        5.0 * act302 + 4.0 * act363 + 3.0 * act323 + 2.0 * act379

    /** Night-time (20:00+) incidents get a 1.5× boost. */
    val timeWeightedRisk: Double =
        timeWeightedRiskOverride ?: (if (hour >= 20) baseRisk * 1.5 else baseRisk)
}

