package com.nagarsetu.raksha.data.routing

import com.nagarsetu.raksha.data.crime.RiskCalculator
import com.nagarsetu.raksha.domain.model.HazardZone
import com.nagarsetu.raksha.domain.model.RouteType
import com.nagarsetu.raksha.domain.model.SafeRoute

/**
 * Generates exactly three candidate routes (Fastest / Balanced / Safest)
 * between an origin and a destination using crime-risk-aware cost functions.
 *
 * Cost functions (from spec):
 *   Fastest:  cost = distance
 *   Balanced: cost = distance + 3 × risk + 2 × road_penalty
 *   Safest:   cost = distance + 6 × risk + 5 × road_penalty
 *
 * No real road graph is used — routes are polylines of ~12 waypoints built
 * from the straight-line baseline; BALANCED and SAFEST variants perturb each
 * intermediate waypoint perpendicularly to minimise their cost function.
 *
 * Ported from Raksha (com.safepath.indore.routing.RouteGenerator).
 */
class RouteGenerator(private val riskCalc: RiskCalculator) {

    private val numWaypoints = 11   // total points including endpoints

    /**
     * Generates the three candidate routes.
     *
     * @param origin          start coordinate
     * @param destination     end coordinate
     * @param stickToMainRoads if true, doubles road penalty on tertiary/residential
     *                         roads (except in a ~500 m buffer around endpoints)
     * @param hazards         live hazard zones from the backend (may be empty)
     */
    fun generate(
        origin: LatLonPoint,
        destination: LatLonPoint,
        stickToMainRoads: Boolean,
        hazards: List<HazardZone> = emptyList()
    ): List<SafeRoute> {
        val directDist = GeoUtils.distanceMeters(origin, destination)
        val maxOff = (directDist * 0.2).coerceAtMost(1000.0).coerceAtLeast(100.0)

        val dynamicBalanced = listOf(-maxOff * 0.6, -maxOff * 0.3, 0.0, maxOff * 0.3, maxOff * 0.6)
        val dynamicSafest   = listOf(-maxOff, -maxOff * 0.6, -maxOff * 0.3, 0.0,
                                      maxOff * 0.3, maxOff * 0.6, maxOff)

        // FASTEST: snap intermediate waypoints to the nearest main road corridor
        val fastestPts = ArrayList<LatLonPoint>(numWaypoints)
        fastestPts += origin
        for (i in 1 until numWaypoints - 1) {
            val t = i.toDouble() / (numWaypoints - 1)
            val baselinePoint = GeoUtils.interpolate(origin, destination, t)
            fastestPts += RoadNetwork.nearestMainRoadPoint(baselinePoint)
        }
        fastestPts += destination
        val fastest = scoreRoute(RouteType.FASTEST, fastestPts, stickToMainRoads, origin, destination, hazards)

        val balanced = optimise(RouteType.BALANCED, origin, destination, dynamicBalanced, stickToMainRoads, hazards)
        val safest   = optimise(RouteType.SAFEST,   origin, destination, dynamicSafest,   stickToMainRoads, hazards)

        return listOf(fastest, balanced, safest)
    }

    // ── core optimiser ───────────────────────────────────────────────────────

    private fun optimise(
        type: RouteType,
        a: LatLonPoint,
        b: LatLonPoint,
        offsets: List<Double>,
        stickToMainRoads: Boolean,
        hazards: List<HazardZone>
    ): SafeRoute {
        val pts = ArrayList<LatLonPoint>(numWaypoints)
        pts += a
        val totalDirectDist = GeoUtils.distanceMeters(a, b)

        for (i in 1 until numWaypoints - 1) {
            val t = i.toDouble() / (numWaypoints - 1)
            val baseline = GeoUtils.interpolate(a, b, t)

            val candidates = ArrayList<LatLonPoint>()
            for (off in offsets) {
                candidates += if (off == 0.0) baseline
                              else GeoUtils.perpendicularOffset(a, b, t, off)
            }

            if (type == RouteType.SAFEST || type == RouteType.BALANCED) {
                val mainRoad = RoadNetwork.nearestMainRoadPoint(baseline)
                val distToMain = GeoUtils.distanceMeters(baseline, mainRoad)
                val mainDistToDest = GeoUtils.distanceMeters(mainRoad, b)
                val baseDistToDest = GeoUtils.distanceMeters(baseline, b)
                if (distToMain < totalDirectDist * 0.4 &&
                    mainDistToDest <= baseDistToDest + 100) {
                    candidates += mainRoad
                }
            }

            val prev = pts.last()
            val best = candidates.minBy { c ->
                val d = GeoUtils.distanceMeters(prev, c)
                val distFromBaseline = GeoUtils.distanceMeters(baseline, c)
                val risk = riskCalc.riskAt(c, hazards = hazards)
                val penalty = roadPenaltyAt(c, t, stickToMainRoads)

                // Progress check: penalise any candidate that moves away from the destination
                val distToDest = GeoUtils.distanceMeters(c, b)
                val prevDistToDest = GeoUtils.distanceMeters(prev, b)
                val movingAwayPenalty = if (distToDest > prevDistToDest)
                    (distToDest - prevDistToDest) * 100.0 else 0.0

                val distW  = when (type) { RouteType.FASTEST -> 1.0; RouteType.BALANCED -> 2.0; RouteType.SAFEST -> 3.0 }
                val riskW  = when (type) { RouteType.FASTEST -> 2.0; RouteType.BALANCED -> 10.0; RouteType.SAFEST -> 25.0 }
                val penW   = when (type) { RouteType.FASTEST -> 0.0; RouteType.BALANCED -> 6.0; RouteType.SAFEST -> 12.0 }

                (d * distW) + (distFromBaseline * 0.5) +
                (risk * riskW * 40.0) + (penalty * penW * 40.0) + movingAwayPenalty
            }
            pts += best
        }
        pts += b
        return scoreRoute(type, pts, stickToMainRoads, a, b, hazards)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun roadPenaltyAt(point: LatLonPoint, progressT: Double, stickToMainRoads: Boolean): Double {
        val type = RoadNetwork.classify(point)
        var pen = type.penalty
        if (stickToMainRoads &&
            progressT > 0.05 && progressT < 0.95 &&
            (type == RoadType.TERTIARY || type == RoadType.RESIDENTIAL || type == RoadType.SERVICE)) {
            pen *= 2.0
        }
        return pen
    }

    private fun scoreRoute(
        type: RouteType,
        points: List<LatLonPoint>,
        stickToMainRoads: Boolean,
        origin: LatLonPoint,
        destination: LatLonPoint,
        hazards: List<HazardZone>
    ): SafeRoute {
        val dist = GeoUtils.polylineLengthMeters(points)
        val risk = riskCalc.routeRisk(points, hazards = hazards, sampleEveryMeters = 100.0)

        val samples = GeoUtils.samplePolyline(points, 100.0)
        val totalLen = GeoUtils.distanceMeters(origin, destination).coerceAtLeast(1.0)
        var penaltySum = 0.0
        for ((idx, s) in samples.withIndex()) {
            val t = idx.toDouble() / (samples.size - 1).coerceAtLeast(1)
            penaltySum += roadPenaltyAt(s, t, stickToMainRoads)
        }
        val penaltyAvg = penaltySum / samples.size.coerceAtLeast(1)

        val cost = when (type) {
            RouteType.FASTEST  -> dist
            RouteType.BALANCED -> dist + 3.0 * risk + 2.0 * penaltyAvg * totalLen
            RouteType.SAFEST   -> dist + 6.0 * risk + 5.0 * penaltyAvg * totalLen
        }
        return SafeRoute(type, points, dist, risk, penaltyAvg, cost)
    }
}
