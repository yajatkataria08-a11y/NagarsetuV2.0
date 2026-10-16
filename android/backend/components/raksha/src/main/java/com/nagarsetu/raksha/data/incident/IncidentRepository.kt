package com.nagarsetu.raksha.data.incident

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for crowd-sourced safety incident reports.
 *
 * Ported from Raksha (com.safepath.indore.data.IncidentRepository) and
 * rewritten as a Hilt-injectable suspend-based coroutine repository to
 * fit the NagarSetu architecture (no raw callbacks, no static objects).
 *
 * Backend endpoints:
 *   POST /api/incidents              – submit a new report
 *   GET  /api/incidents?status=verified – fetch active verified reports
 *   POST /api/sos                    – trigger SOS alert
 */
@Singleton
class IncidentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    // ── Live incident stream ─────────────────────────────────────────────────
    /** Observed by RakshaViewModel to update the real-time heatmap. */
    private val _incidentFlow = MutableStateFlow<List<IncidentReport>>(emptyList())
    val incidentFlow = _incidentFlow.asStateFlow()

    companion object {
        private const val TAG = "IncidentRepository"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    // ── config ───────────────────────────────────────────────────────────────

    /**
     * Base URL for the SafePath / Raksha backend.
     * Reads an override from SharedPreferences (useful for local dev).
     * Falls back to the BuildConfig value written at compile time.
     *
     * Set via:  Settings → Developer → API Endpoint  (in the NagarSetu UI)
     */
    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("raksha_api_endpoint", null)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            // Default: laptop IP for real device (admin server port 3000)
            "http://10.197.165.59:3000"
        }.removeSuffix("/")
    }

    private fun url(path: String) = "${baseUrl()}$path"

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Submits a new safety incident report to the backend.
     * @return true on HTTP 2xx, false on network or server error.
     */
    suspend fun submitReport(report: IncidentReport): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("type", report.type.name)
            .put("description", report.description)
            .put("lat", report.latitude)          // Matches admin
            .put("lng", report.longitude)         // Matches admin
            .put("severity", report.severity)
            .put("reportedBy", report.reportedBy)
            .put("reporterName", report.reportedBy)
            .put("ward", "Unknown")
            .put("photos", JSONArray())
            .put("assignedTo", JSONObject.NULL)
            .put("notes", "")
            .toString()
            .toRequestBody(JSON_MEDIA)

        val request = Request.Builder()
            .url(url("/api/incidents"))
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "submitReport HTTP ${response.code}: ${response.message}")
                }
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e(TAG, "submitReport network error: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches verified active incidents from the backend.
     * Returns an empty list on any error so the UI degrades gracefully.
     */
    suspend fun getActiveIncidents(): List<IncidentReport> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url("/api/incidents?status=verified"))
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "getActiveIncidents HTTP ${response.code}")
                    return@withContext emptyList()
                }
                parseReports(response.body?.string().orEmpty()).also { reports ->
                    _incidentFlow.value = reports   // publish to live flow
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getActiveIncidents network error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Sends an SOS alert with the user's coordinates and emergency details.
     * @return the SOS ID on success, null otherwise.
     */
    suspend fun submitSos(
        latitude: Double,
        longitude: Double,
        contact: String,
        userId: String = "anonymous",
        userName: String = "NagarSetu User",
        phone: String = "",
        ward: String = "Unknown",
        batteryLevel: Int = -1,
        heartRate: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("userId", userId)
            .put("userName", userName)
            .put("phone", phone)
            .put("lat", latitude)             // Admin uses lat/lng
            .put("lng", longitude)
            .put("ward", ward)
            .put("triageLevel", "HIGH")
            .put("trustedContacts", JSONArray())
            .put("batteryLevel", batteryLevel)
            .put("heartRate", heartRate)
            .put("networkType", "4G")
            .put("address", contact)          // Reuse contact field as address
            .put("fcmToken", "")
            .toString()
            .toRequestBody(JSON_MEDIA)

        Log.d(TAG, "Submitting SOS to ${url("/api/sos")}")

        val request = Request.Builder()
            .url(url("/api/sos"))
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "submitSos HTTP ${response.code}: ${response.message}")
                    null
                } else {
                    val respBody = response.body?.string() ?: return@use null
                    JSONObject(respBody).optString("id").takeIf { it.isNotBlank() }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "submitSos network error: ${e.message}", e)
            null
        }
    }

    /**
     * Updates the location of an active SOS event.
     */
    suspend fun updateSosLocation(
        sosId: String,
        latitude: Double,
        longitude: Double,
        batteryLevel: Int,
        heartRate: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("lat", latitude)
            .put("lng", longitude)
            .put("batteryLevel", batteryLevel)
            .put("heartRate", heartRate)
            .toString()
            .toRequestBody(JSON_MEDIA)

        val request = Request.Builder()
            .url(url("/api/sos/$sosId/location"))
            .patch(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "updateSosLocation HTTP ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e(TAG, "updateSosLocation network error: ${e.message}", e)
            false
        }
    }

    // ── parsing ──────────────────────────────────────────────────────────────

    private fun parseReports(raw: String): List<IncidentReport> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                runCatching {
                    val item = array.getJSONObject(i)
                    val type = runCatching {
                        IncidentType.valueOf(item.optString("type", "OTHER"))
                    }.getOrDefault(IncidentType.OTHER)
                    IncidentReport(
                        id          = item.optString("id"),
                        type        = type,
                        latitude    = item.optDouble("lat", item.optDouble("latitude", 0.0)),
                        longitude   = item.optDouble("lng", item.optDouble("longitude", 0.0)),
                        description = item.optString("description"),
                        severity    = item.optInt("severity", 3),
                        status      = item.optString("status", "verified"),
                        timestamp   = item.optLong("timestamp", System.currentTimeMillis()),
                        reportedBy  = item.optString("reportedBy", "anonymous")
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseReports failed: ${e.message}", e)
            emptyList()
        }
    }
}
