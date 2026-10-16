package com.nagarsetu.healthwatch.data

import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.backend.core.config.ExternalApiConfig
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.healthwatch.data.network.HealthApiService
import com.nagarsetu.healthwatch.data.network.HealthOverpassQuery
import com.nagarsetu.healthwatch.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val assets: AssetDataRepository,
    private val apiService: HealthApiService,
    private val config: ExternalApiConfig
) : HealthDataRepository {

    // ── Epidemic zones from data.gov.in → fallback to seed ───────────────────
    override suspend fun getEpidemicZones(): List<EpidemicZone> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getEpidemicData(apiKey = config.dataGovInKey)
            response.records.mapIndexed { i, record ->
                EpidemicZone(
                    id          = "dg_$i",
                    diseaseName = record.disease ?: "Unknown",
                    latitude    = record.latitude?.toDoubleOrNull()  ?: (23.25 + i * 0.01),
                    longitude   = record.longitude?.toDoubleOrNull() ?: (77.41 + i * 0.01),
                    intensity   = 0.7f,
                    trend       = HealthTrend.RISING,
                    activeCases = record.cases?.toIntOrNull() ?: 0
                )
            }
        }.getOrElse {
            // Asset seed fallback
            val epidemic = assets.loadAppData()
                .getAsJsonObject("healthWatch")
                ?.getAsJsonObject("epidemicData") ?: return@getOrElse emptyList()
            val zones = mutableListOf<EpidemicZone>()
            listOf("dengue", "malaria", "covid").forEachIndexed { i, key ->
                val o        = epidemic.getAsJsonObject(key) ?: return@forEachIndexed
                val hotspots = o.getAsJsonArray("hotspots")?.map { it.asString } ?: emptyList()
                hotspots.forEachIndexed { j, _ ->
                    zones += EpidemicZone(
                        id          = "${key}_$j",
                        diseaseName = key.replaceFirstChar { it.uppercase() },
                        latitude    = 23.25 + i * 0.01 + j * 0.005,
                        longitude   = 77.41 + j * 0.01,
                        intensity   = when (o["riskLevel"].asString) {
                            "HIGH"   -> 0.9f; "MEDIUM" -> 0.6f; else -> 0.3f
                        },
                        trend       = HealthTrend.RISING,
                        activeCases = o["casesLastWeek"].asInt
                    )
                }
            }
            zones
        }
    }

    // ── Hospitals & clinics: OSM Overpass → fallback to asset seed ───────────
    override suspend fun getClinics(): List<Clinic> = withContext(Dispatchers.IO) {
        val lat = CivicConstants.BHOPAL_LAT
        val lng = CivicConstants.BHOPAL_LNG

        runCatching {
            val query    = HealthOverpassQuery.hospitalsNear(lat, lng, radiusM = 5000, limit = 25)
            val response = apiService.queryHospitals(overpassQuery = query)

            response.elements.mapNotNull { el ->
                val elLat = if (el.lat != 0.0) el.lat else return@mapNotNull null
                val elLon = if (el.lon != 0.0) el.lon else return@mapNotNull null
                val name  = el.tags["name"]
                    ?: el.tags["name:hi"]
                    ?: el.tags["operator"]
                    ?: "अस्पताल (OSM ${el.id})"
                val amenity = el.tags["amenity"] ?: el.tags["healthcare"] ?: "hospital"
                val type    = when (amenity) {
                    "hospital"    -> "अस्पताल"
                    "clinic"      -> "क्लीनिक"
                    "doctors"     -> "डॉक्टर"
                    "health_post" -> "स्वास्थ्य केंद्र"
                    else          -> "चिकित्सा केंद्र"
                }
                val dist = haversineKm(lat, lng, elLat, elLon)
                Clinic(
                    id           = "osm_${el.id}",
                    name         = name,
                    type         = type,
                    latitude     = elLat,
                    longitude    = elLon,
                    distanceKm   = dist,
                    phone        = el.tags["phone"] ?: el.tags["contact:phone"] ?: "",
                    isOpen       = el.tags["opening_hours"] != "off",
                    timings      = el.tags["opening_hours"] ?: "9:00 AM – 6:00 PM",
                    specialties  = el.tags["healthcare:speciality"]
                        ?.split(";")?.map { it.trim() } ?: emptyList()
                )
            }.sortedBy { it.distanceKm }
        }.getOrElse {
            // Asset seed fallback
            val arr = assets.loadAppData()
                .getAsJsonObject("healthWatch")
                ?.getAsJsonArray("hospitals") ?: return@getOrElse emptyList()
            arr.mapIndexed { i, h ->
                val o    = h.asJsonObject
                val hLat = o["latitude"].asDouble
                val hLon = o["longitude"].asDouble
                Clinic(
                    id         = "seed_$i",
                    name       = o["name"].asString,
                    type       = "अस्पताल",
                    latitude   = hLat,
                    longitude  = hLon,
                    distanceKm = haversineKm(lat, lng, hLat, hLon),
                    phone      = o["phone"]?.asString ?: "",
                    isOpen     = true
                )
            }
        }
    }

    // ── Telemedicine doctors (seed, no public API) ────────────────────────────
    override suspend fun getDoctors(): List<TelemedicineDoctor> {
        return assets.loadAppData()
            .getAsJsonObject("healthWatch")
            ?.getAsJsonArray("telemedicineDoctors")
            ?.mapIndexed { i, d ->
                val o = d.asJsonObject
                TelemedicineDoctor(
                    id        = "doc_$i",
                    name      = o["name"].asString,
                    specialty = o["specialty"].asString,
                    isOnline  = o["isOnline"].asBoolean,
                    rating    = o["rating"].asFloat
                )
            } ?: emptyList()
    }

    override suspend fun getAirQuality(lat: Double, lng: Double): AirQualityData = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getAirQuality(config.waqiToken)
            if (response.status != "ok" || response.data == null) throw Exception("WAQI Fail")
            
            val d = response.data
            val aqi = d.aqi
            val pollutants = mutableListOf<Pollutant>()
            
            d.iaqi?.let { iaqi ->
                iaqi.pm25?.let { pollutants.add(mapPollutant("PM2.5", it.v, "μg/m³")) }
                iaqi.pm10?.let { pollutants.add(mapPollutant("PM10", it.v, "μg/m³")) }
                iaqi.no2?.let { pollutants.add(mapPollutant("NO2", it.v, "ppb")) }
                iaqi.o3?.let { pollutants.add(mapPollutant("O3", it.v, "ppb")) }
                iaqi.so2?.let { pollutants.add(mapPollutant("SO2", it.v, "ppb")) }
                iaqi.co?.let { pollutants.add(mapPollutant("CO", it.v, "ppm")) }
            }

            val (status, desc, rec) = aqiInfo(aqi)
            AirQualityData(aqi, status, desc, rec, pollutants)
        }.getOrElse {
            // Simulated Bhopal data
            val aqi = Random.nextInt(40, 160)
            val (status, desc, rec) = aqiInfo(aqi)
            val pollutants = listOf(
                mapPollutant("PM2.5", Random.nextInt(10, 150).toDouble(), "μg/m³"),
                mapPollutant("PM10", Random.nextInt(20, 200).toDouble(), "μg/m³"),
                mapPollutant("NO2", Random.nextInt(5, 40).toDouble(), "ppb"),
                mapPollutant("O3", Random.nextInt(10, 60).toDouble(), "ppb"),
                mapPollutant("SO2", Random.nextInt(1, 10).toDouble(), "ppb"),
                mapPollutant("CO", Random.nextDouble(0.2, 2.0), "ppm")
            )
            AirQualityData(aqi, status, desc, rec, pollutants)
        }
    }

    private fun mapPollutant(name: String, value: Double, unit: String): Pollutant {
        val (level, color, impact, progress) = when {
            value <= 50 -> Quadruple("Good", "#4CAF50", "Minimal impact", value / 100f)
            value <= 100 -> Quadruple("Moderate", "#FFEB3B", "May affect sensitive individuals", value / 200f)
            value <= 200 -> Quadruple("Poor", "#FF9800", "Breathing discomfort to most", value / 300f)
            else -> Quadruple("Severe", "#F44336", "Respiratory illness on long exposure", 0.9f)
        }
        return Pollutant(name, value, unit, level, progress.toFloat().coerceIn(0.1f, 0.95f), color, impact)
    }

    private fun aqiInfo(aqi: Int): Triple<String, String, String> = when {
        aqi <= 50 -> Triple("Good", "Air quality is satisfactory", "Safe for outdoor activities.")
        aqi <= 100 -> Triple("Moderate", "Acceptable air quality", "Sensitive groups should limit exposure.")
        aqi <= 200 -> Triple("Poor", "Health caution advised", "Wear mask and avoid long outdoor stays.")
        else -> Triple("Severe", "Health alert", "Avoid all outdoor activities.")
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ── Haversine helper ──────────────────────────────────────────────────────
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = Math.sin(dLat / 2).let { it * it } +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
