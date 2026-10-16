package com.nagarsetu.raksha.data.routing

import kotlin.math.sqrt
import com.nagarsetu.backend.core.CivicConstants

/**
 * Hardcoded "main-road corridor" approximation for Bhopal/Indore.
 *
 * Ported from Raksha (com.safepath.indore.routing.RoadNetwork) and adapted
 * with additional Bhopal corridors for NagarSetu's civic context.
 *
 * A point's [RoadType] is derived from how close it is to a known corridor:
 *   ≤  60 m of MOTORWAY  → MOTORWAY
 *   ≤ 120 m of PRIMARY   → PRIMARY
 *   ≤ 200 m of SECONDARY → SECONDARY
 *   ≤ 350 m of any       → TERTIARY
 *   else                 → RESIDENTIAL
 */
object RoadNetwork {

    private data class Corridor(val type: RoadType, val polyline: List<LatLonPoint>)

    private val corridors: List<Corridor> = listOf(
        // ─── Indore corridors (from Raksha) ───────────────────────────────

        // AB Road — Rajwada → Vijay Nagar → Bypass
        Corridor(RoadType.MOTORWAY, listOf(
            LatLonPoint(23.2599, 77.4126),
            LatLonPoint(22.7250, 75.8650),
            LatLonPoint(22.7330, 75.8740),
            LatLonPoint(22.7430, 75.8850),
            LatLonPoint(22.7530, 75.8930),
            LatLonPoint(22.7660, 75.8980)
        )),
        // Ring Road (eastern arc)
        Corridor(RoadType.MOTORWAY, listOf(
            LatLonPoint(22.7660, 75.8980),
            LatLonPoint(22.7700, 75.9100),
            LatLonPoint(22.7600, 75.9220),
            LatLonPoint(22.7400, 75.9270),
            LatLonPoint(22.7150, 75.9200),
            LatLonPoint(22.6970, 75.9050)
        )),
        // MR-10 (north-west)
        Corridor(RoadType.PRIMARY, listOf(
            LatLonPoint(22.7530, 75.8930),
            LatLonPoint(22.7720, 75.8780),
            LatLonPoint(22.7860, 75.8600)
        )),
        // Khandwa Road (south)
        Corridor(RoadType.PRIMARY, listOf(
            LatLonPoint(23.2599, 77.4126),
            LatLonPoint(22.7050, 75.8500),
            LatLonPoint(22.6850, 75.8420),
            LatLonPoint(22.6650, 75.8300)
        )),
        // Mhow / Agra-Bombay road south-west
        Corridor(RoadType.PRIMARY, listOf(
            LatLonPoint(23.2599, 77.4126),
            LatLonPoint(22.7100, 75.8400),
            LatLonPoint(22.6950, 75.8200)
        )),
        // East-west cross corridor (Bhawarkuan → Palasia)
        Corridor(RoadType.SECONDARY, listOf(
            LatLonPoint(22.7050, 75.8700),
            LatLonPoint(22.7200, 75.8780),
            LatLonPoint(22.7330, 75.8740),
            LatLonPoint(22.7430, 75.8850)
        )),
        // Sapna Sangeeta / Geeta Bhavan inner ring
        Corridor(RoadType.SECONDARY, listOf(
            LatLonPoint(22.7280, 75.8700),
            LatLonPoint(22.7350, 75.8800),
            LatLonPoint(22.7400, 75.8900),
            LatLonPoint(22.7460, 75.8970)
        )),

        // ─── Bhopal corridors (added for NagarSetu) ──────────────────────

        // Hoshangabad Road (NH-12) — main south spine
        Corridor(RoadType.MOTORWAY, listOf(
            LatLonPoint(CivicConstants.BHOPAL_LAT, CivicConstants.BHOPAL_LNG),
            LatLonPoint(23.2450, 77.4200),
            LatLonPoint(23.2250, 77.4350),
            LatLonPoint(23.2050, 77.4500)
        )),
        // VIP Road / Arera Colony axis
        Corridor(RoadType.PRIMARY, listOf(
            LatLonPoint(23.2100, 77.4350),
            LatLonPoint(23.2200, 77.4400),
            LatLonPoint(23.2350, 77.4440),
            LatLonPoint(23.2500, 77.4450)
        )),
        // Kolar Road (east)
        Corridor(RoadType.PRIMARY, listOf(
            LatLonPoint(CivicConstants.BHOPAL_LAT - 0.02, CivicConstants.BHOPAL_LNG),
            LatLonPoint(23.2350, 77.4400),
            LatLonPoint(23.2300, 77.4600),
            LatLonPoint(23.2200, 77.4800)
        )),
        // Berasia Road (north)
        Corridor(RoadType.SECONDARY, listOf(
            LatLonPoint(CivicConstants.BHOPAL_LAT, CivicConstants.BHOPAL_LNG),
            LatLonPoint(23.2750, 77.4100),
            LatLonPoint(23.2900, 77.4080)
        ))
    )

    /** Classify a single coordinate by distance to known corridors. */
    fun classify(point: LatLonPoint): RoadType {
        var bestType: RoadType? = null
        var bestDistance = Double.MAX_VALUE
        for (c in corridors) {
            val d = distanceToPolyline(point, c.polyline)
            val tolerance = when (c.type) {
                RoadType.MOTORWAY  -> 60.0
                RoadType.PRIMARY   -> 120.0
                RoadType.SECONDARY -> 200.0
                else               -> 350.0
            }
            if (d <= tolerance && d < bestDistance) {
                bestDistance = d
                bestType = c.type
            } else if (d <= 350.0 && d < bestDistance && bestType == null) {
                bestDistance = d
                bestType = RoadType.TERTIARY
            }
        }
        return bestType ?: RoadType.RESIDENTIAL
    }

    /**
     * Closest point on any MOTORWAY or PRIMARY corridor to [point].
     * Used by the safest-route generator to bias waypoints toward the main network.
     */
    fun nearestMainRoadPoint(point: LatLonPoint): LatLonPoint {
        var best = point
        var bestD = Double.MAX_VALUE
        for (c in corridors) {
            if (c.type == RoadType.MOTORWAY || c.type == RoadType.PRIMARY) {
                val (closest, d) = nearestOnPolyline(point, c.polyline)
                if (d < bestD) { bestD = d; best = closest }
            }
        }
        return best
    }

    // ── internal helpers ────────────────────────────────────────────────────

    private fun distanceToPolyline(p: LatLonPoint, line: List<LatLonPoint>): Double {
        var min = Double.MAX_VALUE
        for (i in 1 until line.size) {
            val (_, d) = nearestOnSegment(p, line[i - 1], line[i])
            if (d < min) min = d
        }
        return min
    }

    private fun nearestOnPolyline(p: LatLonPoint, line: List<LatLonPoint>): Pair<LatLonPoint, Double> {
        var best = line.first()
        var bestD = Double.MAX_VALUE
        for (i in 1 until line.size) {
            val (q, d) = nearestOnSegment(p, line[i - 1], line[i])
            if (d < bestD) { bestD = d; best = q }
        }
        return best to bestD
    }

    private fun nearestOnSegment(p: LatLonPoint, a: LatLonPoint, b: LatLonPoint): Pair<LatLonPoint, Double> {
        val abLat = b.lat - a.lat
        val abLng = b.lng - a.lng
        val apLat = p.lat - a.lat
        val apLng = p.lng - a.lng
        val ab2 = abLat * abLat + abLng * abLng
        val t = if (ab2 == 0.0) 0.0
                else ((apLat * abLat + apLng * abLng) / ab2).coerceIn(0.0, 1.0)
        val q = LatLonPoint(a.lat + t * abLat, a.lng + t * abLng)
        return q to GeoUtils.distanceMeters(p, q)
    }
}
