package com.nagarsetu.raksha.data.incident

import android.content.Context
import android.util.Log
import com.nagarsetu.core.data.network.NagarSetuApiService
import com.nagarsetu.raksha.domain.model.HazardZone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches live hazard zones from the NagarSetu backend REST API.
 *
 * Endpoint:
 *   GET /api/hazards
 *
 * Hazard zones are used by RiskCalculator to override local risk
 * when the citizen approaches an active zone (flood, fire, road hazard, etc.).
 */
@Singleton
class HazardZoneFetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val apiService: NagarSetuApiService
) {
    companion object {
        private const val TAG = "HazardZoneFetcher"
    }

    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("nagarsetu_api_endpoint", null)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            "http://10.0.2.2:4000"
        }.removeSuffix("/")
    }

    /**
     * Returns currently active hazard zones.
     * Uses Retrofit [NagarSetuApiService] first, falling back to direct OkHttp if needed.
     * Falls back to an empty list on any error so routing degrades gracefully.
     */
    suspend fun fetchActiveZones(): List<HazardZone> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getActiveHazards()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.map { dto ->
                    HazardZone(
                        id = (dto.id as? Number)?.toInt() ?: dto.id.toString().hashCode(),
                        lat = dto.lat,
                        lng = dto.lng,
                        radiusM = dto.radiusM,
                        risk = dto.risk
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Retrofit getActiveHazards failed, attempting fallback OkHttp: ${e.message}")
        }

        // Fallback: direct OkHttp call
        val request = Request.Builder()
            .url("${baseUrl()}/api/hazards")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchActiveZones HTTP ${response.code}")
                    return@withContext emptyList()
                }
                parseZones(response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            Log.e(TAG, "fetchActiveZones network error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseZones(raw: String): List<HazardZone> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                HazardZone(
                    id      = obj.optInt("id", i),
                    lat     = obj.getDouble("lat"),
                    lng     = obj.getDouble("lng"),
                    radiusM = obj.optInt("radius_m", 200),
                    risk    = obj.optDouble("risk", 1.0)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseZones failed: ${e.message}", e)
            emptyList()
        }
    }
}
