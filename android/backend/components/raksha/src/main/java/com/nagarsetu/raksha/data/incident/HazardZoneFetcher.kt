package com.nagarsetu.raksha.data.incident

import android.content.Context
import android.util.Log
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
 * Fetches live hazard zones from the backend REST API.
 *
 * Ported from Raksha (com.safepath.indore.data.HazardZoneFetcher) and
 * rewritten as a Hilt-injectable coroutine repository.
 *
 * Endpoint:
 *   GET /api/hazard-zones?active=true
 *   Response: [ { "id": Int, "lat": Double, "lng": Double,
 *                 "radius_m": Int, "risk": Double }, ... ]
 *
 * Hazard zones are used by [RiskCalculator] to override local crime-based risk
 * when the user enters an active zone (flood, fire, construction, etc.).
 */
@Singleton
class HazardZoneFetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "HazardZoneFetcher"
    }

    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("raksha_api_endpoint", null)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            "http://10.0.2.2:8080"
        }.removeSuffix("/")
    }

    /**
     * Returns currently active hazard zones.
     * Falls back to an empty list on any error so routing degrades gracefully.
     */
    suspend fun fetchActiveZones(): List<HazardZone> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl()}/api/hazard-zones?active=true")
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
                    id      = obj.getInt("id"),
                    lat     = obj.getDouble("lat"),
                    lng     = obj.getDouble("lng"),
                    radiusM = obj.getInt("radius_m"),
                    risk    = obj.getDouble("risk")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseZones failed: ${e.message}", e)
            emptyList()
        }
    }
}
