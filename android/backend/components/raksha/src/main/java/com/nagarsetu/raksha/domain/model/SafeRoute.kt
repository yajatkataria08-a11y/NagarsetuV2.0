package com.nagarsetu.raksha.domain.model

import com.nagarsetu.raksha.data.routing.LatLonPoint

/** Route type classifications used by the safe-routing engine. */
enum class RouteType { FASTEST, BALANCED, SAFEST }

/**
 * A generated candidate route between two points.
 *
 * Ported from Raksha (com.safepath.indore.routing.Route).
 */
data class SafeRoute(
    val type: RouteType,
    val points: List<LatLonPoint>,
    val distanceMeters: Double,
    val risk: Double,
    val roadPenalty: Double,
    val cost: Double
) {
    /** Short human-readable summary for bottom-panel chips. */
    fun shortLabel(): String {
        val km = distanceMeters / 1000.0
        val normalizedRisk = if (distanceMeters > 0)
            risk / (distanceMeters / 1000.0) else 0.0
        return "%.1f km · risk %.0f".format(km, normalizedRisk)
    }

    fun displayName(): String = when (type) {
        RouteType.FASTEST  -> "Fastest"
        RouteType.BALANCED -> "Balanced"
        RouteType.SAFEST   -> "Safest"
    }
}
