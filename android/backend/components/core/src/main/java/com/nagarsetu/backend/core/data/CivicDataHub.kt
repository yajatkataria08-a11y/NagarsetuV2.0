package com.nagarsetu.backend.core.data

import com.nagarsetu.backend.core.CivicConstants

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.core.utils.LocationUtils
import com.nagarsetu.core.utils.WardPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central read-only access to Bhopal seed data for all feature backends.
 */
@Singleton
class CivicDataHub @Inject constructor(
    private val assets: AssetDataRepository
) {
    fun appData(): JsonObject = assets.loadAppData()

    fun wards(): List<WardPoint> = assets.wards()

    fun nearestWard(lat: Double, lng: Double): WardPoint? =
        wards().minByOrNull { LocationUtils.haversineMeters(lat, lng, it.latitude, it.longitude) }

    fun parkingLots(): JsonArray =
        appData().getAsJsonArray("parkEase") ?: JsonArray()

    fun chargingStations(): JsonArray =
        appData().getAsJsonArray("chargeUp") ?: JsonArray()

    fun emergencyNumbers(): JsonObject? =
        appData().getAsJsonObject("emergencyAI")?.getAsJsonObject("emergencyNumbers")

    fun busStops(): JsonArray =
        appData().getAsJsonObject("greenRoute")?.getAsJsonArray("busStops") ?: JsonArray()

    fun hospitals(): JsonArray =
        appData().getAsJsonObject("healthWatch")?.getAsJsonArray("hospitals") ?: JsonArray()

    fun citySummary(): String {
        val wards = wards().size
        val parking = parkingLots().size()
        val ev = chargingStations().size()
        return "Bhopal civic hub: $wards wards, $parking parking lots, $ev EV stations (center ${CivicConstants.BHOPAL_LAT}, ${CivicConstants.BHOPAL_LNG})."
    }
}
