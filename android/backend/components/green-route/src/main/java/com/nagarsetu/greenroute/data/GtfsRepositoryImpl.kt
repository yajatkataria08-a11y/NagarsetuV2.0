package com.nagarsetu.greenroute.data

import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.greenroute.data.network.OsrmApiService
import com.nagarsetu.greenroute.data.network.firstRoute
import com.nagarsetu.greenroute.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GtfsRepositoryImpl @Inject constructor(
    private val assets: AssetDataRepository,
    private val osrm: OsrmApiService          // ← was ORS/GreenRouteApiService
) : GtfsRepository {

    override suspend fun getNearbyArrivals(lat: Double, lng: Double): List<TransitStation> {
        val root = assets.loadAppData().getAsJsonObject("greenRoute") ?: return emptyList()
        val stations = mutableListOf<TransitStation>()

        // 1. Bus Stops
        root.getAsJsonArray("busStops")?.forEach { el ->
            val o = el.asJsonObject
            stations.add(TransitStation(
                id = o["id"].asString,
                name = o["name"].asString,
                mode = TransitMode.BUS,
                latitude = o["latitude"].asDouble,
                longitude = o["longitude"].asDouble,
                nextArrivalMin = (2..15).random(),
                routes = o.getAsJsonArray("routes")?.map { it.asString } ?: emptyList()
            ))
        }

        // 2. Cycle Stands
        root.getAsJsonArray("cycleStands")?.forEach { el ->
            val o = el.asJsonObject
            stations.add(TransitStation(
                id = o["id"].asString,
                name = o["location"].asString,
                mode = TransitMode.CYCLE,
                latitude = o["latitude"].asDouble,
                longitude = o["longitude"].asDouble,
                nextArrivalMin = 0, // Cycle is always there if available
                routes = listOf("${o["available_bikes"].asInt} bikes free")
            ))
        }

        // 3. Auto Stands
        root.getAsJsonArray("autoRickshawStands")?.forEach { el ->
            val o = el.asJsonObject
            stations.add(TransitStation(
                id = o["id"].asString,
                name = o["area"].asString,
                mode = TransitMode.AUTO,
                latitude = o["latitude"].asDouble,
                longitude = o["longitude"].asDouble,
                nextArrivalMin = (1..5).random(),
                routes = listOf("${o["available_autos"].asInt} autos waiting")
            ))
        }

        // Sort by distance to user (simple Euclidean for demo)
        return stations.sortedBy { 
            val dx = it.latitude - lat
            val dy = it.longitude - lng
            dx*dx + dy*dy
        }
    }

    private val areaCoords = mapOf(
        "mp nagar" to (23.2380 to 77.4620),
        "board office" to (23.2450 to 77.4280),
        "shahpura" to (23.2100 to 77.4300),
        "arera colony" to (23.2280 to 77.4380),
        "rani kamlapati" to (23.2600 to 77.4120),
        "station" to (23.2600 to 77.4120),
        "new market" to (23.2430 to 77.3990),
        "bairagarh" to (23.2500 to 77.3400),
        "karond" to (23.3000 to 77.4000),
        "tt nagar" to (23.2320 to 77.4580),
        "misrod" to (23.2100 to 77.4600)
    )

    override suspend fun routeOptions(destination: String): List<RouteOption> =
        withContext(Dispatchers.IO) {
            val dest = areaCoords.entries.find { destination.lowercase().contains(it.key) }
            val destLat = dest?.value?.first ?: (CivicConstants.BHOPAL_LAT + 0.012)
            val destLng = dest?.value?.second ?: (CivicConstants.BHOPAL_LNG + 0.015)

            // OSRM coords format: "lng,lat;lng,lat"
            val userLat = CivicConstants.BHOPAL_LAT
            val userLng = CivicConstants.BHOPAL_LNG
            val drivingCoords  = "$userLng,$userLat;$destLng,$destLat"
            val cyclingCoords  = drivingCoords

            val drivingMin = runCatching {
                osrm.getRoute(profile = "driving",  coords = drivingCoords)
                    .firstRoute()?.let { (it.duration / 60).toInt() } ?: 20
            }.getOrElse { 20 }

            val cyclingMin = runCatching {
                osrm.getRoute(profile = "cycling", coords = cyclingCoords)
                    .firstRoute()?.let { (it.duration / 60).toInt() } ?: 25
            }.getOrElse { 25 }

            val walkMin = runCatching {
                osrm.getRoute(profile = "foot", coords = drivingCoords)
                    .firstRoute()?.let { (it.duration / 60).toInt() } ?: 40
            }.getOrElse { 40 }

            listOf(
                RouteOption(
                    id          = UUID.randomUUID().toString(),
                    mode        = TransitMode.BUS,
                    durationMin = drivingMin + 8, // Realistic overhead
                    carbonSavedKg = 1.2f,
                    crowdedness = if (drivingMin > 25) Crowdedness.HIGH else Crowdedness.MODERATE,
                    cost        = 20.0,
                    destLat     = destLat,
                    destLng     = destLng
                ),
                RouteOption(
                    id          = UUID.randomUUID().toString(),
                    mode        = TransitMode.CYCLE,
                    durationMin = cyclingMin,
                    carbonSavedKg = 2.4f,
                    crowdedness = Crowdedness.LOW,
                    cost        = 0.0,
                    destLat     = destLat,
                    destLng     = destLng
                ),
                RouteOption(
                    id          = UUID.randomUUID().toString(),
                    mode        = TransitMode.AUTO,
                    durationMin = drivingMin,
                    carbonSavedKg = 0.5f,
                    crowdedness = Crowdedness.LOW,
                    cost        = 50.0,
                    destLat     = destLat,
                    destLng     = destLng
                ),
                RouteOption(
                    id          = UUID.randomUUID().toString(),
                    mode        = TransitMode.WALK,
                    durationMin = walkMin,
                    carbonSavedKg = 3.2f,
                    crowdedness = Crowdedness.LOW,
                    cost        = 0.0,
                    destLat     = destLat,
                    destLng     = destLng
                )
            )
        }
}
