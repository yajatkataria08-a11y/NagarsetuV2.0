package com.nagarsetu.chargeup.data

import com.nagarsetu.chargeup.domain.model.ChargingStation

interface ChargingRepository {
    suspend fun getStationsNearby(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation>
}
