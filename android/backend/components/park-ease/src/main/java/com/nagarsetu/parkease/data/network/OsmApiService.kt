package com.nagarsetu.parkease.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OSM Overpass API – free, no key required.
 *
 * Used for:
 *  • Parking areas / lots near a coordinate    → [queryParking]
 *  • Road-speed / flow proxy via OSRM routing  → see [OsrmApiService]
 *
 * Base URL: https://overpass-api.de/
 */
interface OsmOverpassApiService {

    /**
     * Execute any Overpass QL query and get back GeoJSON-compatible JSON.
     * Caller builds the [query] string.
     */
    @GET("api/interpreter")
    suspend fun query(
        @Query("data") query: String
    ): OverpassResponse
}

// ── Response models ───────────────────────────────────────────────────────────
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

data class OverpassElement(
    val id: Long    = 0L,
    val type: String = "node",
    val lat: Double  = 0.0,
    val lon: Double  = 0.0,
    val tags: Map<String, String> = emptyMap()
)

// ── Query builder helpers ─────────────────────────────────────────────────────
object OverpassQueries {

    /**
     * Parking nodes/ways within [radiusM] metres of the given point.
     * Returns up to [limit] results.
     */
    fun parking(lat: Double, lng: Double, radiusM: Int = 2000, limit: Int = 20): String =
        """[out:json][timeout:15];
(
  node[amenity=parking](around:$radiusM,$lat,$lng);
  way[amenity=parking](around:$radiusM,$lat,$lng);
);
out $limit center;""".trimIndent()

    /**
     * Hospitals and clinics within [radiusM] metres.
     */
    fun hospitals(lat: Double, lng: Double, radiusM: Int = 5000, limit: Int = 20): String =
        """[out:json][timeout:15];
(
  node[amenity=hospital](around:$radiusM,$lat,$lng);
  node[amenity=clinic](around:$radiusM,$lat,$lng);
  node[amenity=doctors](around:$radiusM,$lat,$lng);
  node[amenity=health_post](around:$radiusM,$lat,$lng);
  way[amenity=hospital](around:$radiusM,$lat,$lng);
);
out $limit center;""".trimIndent()
}
