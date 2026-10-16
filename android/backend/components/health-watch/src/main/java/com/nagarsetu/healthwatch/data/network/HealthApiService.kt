package com.nagarsetu.healthwatch.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Health-related network endpoints:
 *
 *  1. data.gov.in  – epidemic / disease surveillance data (free, key required)
 *  2. OSM Overpass – live hospital & clinic discovery near Bhopal (free, no key)
 */
interface HealthApiService {

    // ── 1. Epidemic data from data.gov.in ─────────────────────────────────
    @GET("https://api.data.gov.in/resource/90df18d2-4318-4a6c-940f-7b7134444535")
    suspend fun getEpidemicData(
        @Query("api-key") apiKey: String,
        @Query("format")  format: String = "json",
        @Query("limit")   limit: Int     = 10
    ): DataGovResponse

    // ── 2. OSM Overpass – hospitals & clinics ─────────────────────────────
    // Base URL is overpass-api.de; the query is passed as a GET param.
    @GET("https://overpass-api.de/api/interpreter")
    suspend fun queryHospitals(
        @Query("data") overpassQuery: String
    ): OverpassHealthResponse

    // ── 3. WAQI Air Quality API ───────────────────────────────────────────
    @GET("https://api.waqi.info/feed/bhopal/")
    suspend fun getAirQuality(
        @Query("token") token: String
    ): WaqiResponse
}

// ── WAQI models ──────────────────────────────────────────────────────────────
data class WaqiResponse(
    val status: String,
    val data: WaqiData? = null
)

data class WaqiData(
    val aqi: Int,
    val city: WaqiCity? = null,
    val iaqi: WaqiIaqi? = null,
    val time: WaqiTime? = null
)

data class WaqiCity(val name: String)
data class WaqiTime(val s: String)

data class WaqiIaqi(
    val pm25: WaqiValue? = null,
    val pm10: WaqiValue? = null,
    val no2:  WaqiValue? = null,
    val o3:   WaqiValue? = null,
    val so2:  WaqiValue? = null,
    val co:   WaqiValue? = null
)

data class WaqiValue(val v: Double)

// ── data.gov.in models ────────────────────────────────────────────────────────
data class DataGovResponse(
    val status: String = "",
    val records: List<HealthRecord> = emptyList()
)

data class HealthRecord(
    val disease:   String? = null,
    val state:     String? = null,
    val district:  String? = null,
    val cases:     String? = null,
    val latitude:  String? = null,
    val longitude: String? = null
)

// ── OSM Overpass models (health) ──────────────────────────────────────────────
data class OverpassHealthResponse(
    val elements: List<OverpassHealthElement> = emptyList()
)

data class OverpassHealthElement(
    val id:   Long                    = 0L,
    val type: String                  = "node",
    val lat:  Double                  = 0.0,
    val lon:  Double                  = 0.0,
    val tags: Map<String, String>     = emptyMap()
)

// ── Overpass QL query for Bhopal hospitals/clinics ────────────────────────────
object HealthOverpassQuery {
    fun hospitalsNear(lat: Double, lng: Double, radiusM: Int = 5000, limit: Int = 20): String =
        """[out:json][timeout:20];
(
  node[amenity=hospital](around:$radiusM,$lat,$lng);
  node[amenity=clinic](around:$radiusM,$lat,$lng);
  node[amenity=doctors](around:$radiusM,$lat,$lng);
  node[amenity=health_post](around:$radiusM,$lat,$lng);
  node[healthcare=hospital](around:$radiusM,$lat,$lng);
  way[amenity=hospital](around:$radiusM,$lat,$lng);
);
out $limit center;""".trimIndent()
}
