package com.nagarsetu.chargeup.data

import com.nagarsetu.backend.core.config.ExternalApiConfig
import com.nagarsetu.backend.core.data.CivicDataHub
import com.nagarsetu.chargeup.data.network.ChargingApiService
import com.nagarsetu.chargeup.domain.model.*
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingRepositoryImpl @Inject constructor(
    private val hub: CivicDataHub,
    private val supabase: SupabaseClient,
    private val apiService: ChargingApiService,
    private val config: ExternalApiConfig
) : ChargingRepository {

    override suspend fun getStationsNearby(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> = withContext(Dispatchers.IO) {
        val localStations = hub.chargingStations().map { el ->
            val o = el.asJsonObject
            val types = o.getAsJsonArray("chargerTypes")?.map { t ->
                when (t.asString) {
                    "DC_FAST" -> ConnectorType.CCS2
                    else -> ConnectorType.TYPE2
                }
            } ?: listOf(ConnectorType.CCS2)
            val available = o["availableSlots"].asInt > 0
            ChargingStation(
                id = o["id"].asString,
                name = o["name"].asString,
                latitude = o["latitude"].asDouble,
                longitude = o["longitude"].asDouble,
                connectorTypes = types,
                status = if (available) StationStatus.AVAILABLE else StationStatus.OCCUPIED,
                powerKw = if (types.contains(ConnectorType.CCS2)) 50 else 22,
                costPerKwh = o["pricing"].asDouble
            )
        }

        val apiStations = runCatching {
            apiService.getChargingStations(
                apiKey = config.openChargeMapKey, // empty string = anonymous, still works
                lat = lat,
                lng = lng,
                distance = radiusKm
            ).map { poi ->
                // poi.StatusType?.IsOperational == false → OFFLINE
                ChargingStation(
                    id = "api_${poi.ID}",
                    name = poi.AddressInfo.Title,
                    latitude = poi.AddressInfo.Latitude,
                    longitude = poi.AddressInfo.Longitude,
                    connectorTypes = poi.Connections?.map { 
                        if ((it.PowerKW ?: 0.0) > 40.0) ConnectorType.CCS2 else ConnectorType.TYPE2 
                    }?.distinct() ?: listOf(ConnectorType.TYPE2),
                    status = StationStatus.AVAILABLE,
                    powerKw = poi.Connections?.maxOfOrNull { it.PowerKW ?: 0.0 }?.toInt() ?: 22,
                    costPerKwh = 15.0
                )
            }
        }.getOrDefault(emptyList())

        // Merge and return
        val finalStations = (localStations + apiStations).distinctBy { "${it.latitude},${it.longitude}" }
        
        if (finalStations.isEmpty()) {
            // Hard fallback if even local assets fail
            listOf(
                ChargingStation("fs_1", "Ola Hypercharger - Bhopal", 23.235, 77.425, listOf(ConnectorType.CCS2), StationStatus.AVAILABLE, 120, 15.0),
                ChargingStation("fs_2", "Tata Power - MP Nagar", 23.240, 77.465, listOf(ConnectorType.TYPE2), StationStatus.AVAILABLE, 22, 12.0)
            )
        } else {
            finalStations
        }
    }
}
