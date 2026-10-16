package com.nagarsetu.core.utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {
    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    fun nearestWardName(lat: Double, lng: Double, wards: List<WardPoint>): String {
        if (wards.isEmpty()) return "Bhopal Municipal Corporation"
        return wards.minByOrNull { haversineMeters(lat, lng, it.latitude, it.longitude) }?.name
            ?: "Bhopal Municipal Corporation"
    }
}

data class WardPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val authorityName: String
)
