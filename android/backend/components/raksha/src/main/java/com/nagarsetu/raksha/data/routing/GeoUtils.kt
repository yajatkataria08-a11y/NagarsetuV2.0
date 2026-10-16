package com.nagarsetu.raksha.data.routing

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geospatial utility functions.
 *
 * Ported from Raksha (com.safepath.indore.utils.GeoUtils) and adapted to
 * use raw lat/lng doubles instead of Google Maps LatLng, so there is no
 * Google Play Services dependency in the backend module.
 */
object GeoUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Haversine distance in metres between two coordinates. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val h = sin(dLat / 2).let { it * it } +
                cos(rLat1) * cos(rLat2) * sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_M * c
    }

    fun distanceMeters(a: LatLonPoint, b: LatLonPoint): Double =
        distanceMeters(a.lat, a.lng, b.lat, b.lng)

    /** Total length of a polyline in metres. */
    fun polylineLengthMeters(points: List<LatLonPoint>): Double {
        var sum = 0.0
        for (i in 1 until points.size) sum += distanceMeters(points[i - 1], points[i])
        return sum
    }

    /** Linear interpolation between two coordinates. t ∈ [0, 1]. */
    fun interpolate(a: LatLonPoint, b: LatLonPoint, t: Double): LatLonPoint =
        LatLonPoint(
            lat = a.lat + (b.lat - a.lat) * t,
            lng = a.lng + (b.lng - a.lng) * t
        )

    /**
     * Returns a point offset perpendicular to the AB line by [meters] metres.
     * Positive = left of A→B, negative = right.
     */
    fun perpendicularOffset(a: LatLonPoint, b: LatLonPoint, t: Double, meters: Double): LatLonPoint {
        val mid = interpolate(a, b, t)
        val dLat = b.lat - a.lat
        val dLng = b.lng - a.lng
        val len = sqrt(dLat * dLat + dLng * dLng)
        if (len == 0.0) return mid
        val pLat = -dLng / len
        val pLng = dLat / len
        val degLat = meters / 111_000.0
        val degLng = meters / (111_000.0 * cos(Math.toRadians(mid.lat)))
        return LatLonPoint(mid.lat + pLat * degLat, mid.lng + pLng * degLng)
    }

    /** Sample a polyline at fixed metre intervals. */
    fun samplePolyline(points: List<LatLonPoint>, intervalMeters: Double): List<LatLonPoint> {
        if (points.size < 2) return points
        val out = mutableListOf(points.first())
        var carry = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val seg = distanceMeters(a, b)
            var d = intervalMeters - carry
            while (d <= seg) {
                out += interpolate(a, b, d / seg)
                d += intervalMeters
            }
            carry = seg - (d - intervalMeters)
        }
        out += points.last()
        return out
    }
}

/** Lightweight coordinate pair used throughout the Raksha backend. */
data class LatLonPoint(val lat: Double, val lng: Double)
