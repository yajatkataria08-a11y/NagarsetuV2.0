package com.nagarsetu.raksha.domain.model

/**
 * A dynamic hazard zone fetched from the backend REST API.
 *
 * Ported from Raksha (com.safepath.indore.data.HazardZone).
 * The risk value is a dimensionless score used by [RiskCalculator]
 * to override local crime-based risk when a user enters the zone.
 */
data class HazardZone(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val risk: Double
)
