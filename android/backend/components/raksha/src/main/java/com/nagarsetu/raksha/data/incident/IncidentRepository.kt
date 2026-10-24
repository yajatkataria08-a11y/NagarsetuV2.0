package com.nagarsetu.raksha.data.incident

import android.content.Context
import android.util.Log
import com.nagarsetu.core.data.local.OfflineSyncManager
import com.nagarsetu.core.data.network.IncidentSubmissionDto
import com.nagarsetu.core.data.network.NagarSetuApiService
import com.nagarsetu.core.data.network.SosLocationUpdateDto
import com.nagarsetu.core.data.network.SosRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for crowd-sourced safety incident reports and emergency SOS dispatch.
 * Communicates with the NagarSetu API Gateway, equipped with an offline-first Room queue.
 *
 * Backend endpoints:
 *   POST /api/incidents                 – submit a new report
 *   GET  /api/incidents?status=verified – fetch active verified reports
 *   POST /api/sos                       – trigger SOS alert
 *   PATCH /api/sos/:id/location         – live tracking telemetry
 */
@Singleton
class IncidentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val apiService: NagarSetuApiService,
    private val offlineSyncManager: OfflineSyncManager
) {
    // ── Live incident stream ─────────────────────────────────────────────────
    /** Observed by UI ViewModels to update the real-time heatmap. */
    private val _incidentFlow = MutableStateFlow<List<IncidentReport>>(emptyList())
    val incidentFlow = _incidentFlow.asStateFlow()

    companion object {
        private const val TAG = "IncidentRepository"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    // ── config ───────────────────────────────────────────────────────────────

    /**
     * Base URL for the NagarSetu backend gateway.
     * Reads an override from SharedPreferences (useful for physical device test vs emulator).
     * Defaults to Android emulator bridge: http://10.0.2.2:4000
     */
    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("nagarsetu_api_endpoint", null)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            "http://10.0.2.2:4000"
        }.removeSuffix("/")
    }

    private fun url(path: String) = "${baseUrl()}$path"

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Submits a new safety incident report to the backend.
     * If the network is unavailable, enqueues the report to the local Room database for automated sync.
     * @return true on HTTP 2xx or successful offline queueing.
     */
    suspend fun submitReport(report: IncidentReport): Boolean = withContext(Dispatchers.IO) {
        try {
            val dto = IncidentSubmissionDto(
                type = report.type.name,
                description = report.description,
                lat = report.latitude,
                lng = report.longitude,
                severity = report.severity,
                reportedBy = report.reportedBy,
                reporterName = report.reportedBy,
                ward = "Bhopal"
            )
            val response = apiService.submitIncident(dto)
            if (response.isSuccessful) return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "submitReport via apiService failed, attempting raw fallback: ${e.message}")
        }

        val body = JSONObject()
            .put("type", report.type.name)
            .put("description", report.description)
            .put("lat", report.latitude)
            .put("lng", report.longitude)
            .put("severity", report.severity)
            .put("reportedBy", report.reportedBy)
            .put("reporterName", report.reportedBy)
            .put("ward", "Bhopal")
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
                    offlineSyncManager.enqueueReport(
                        type = report.type.name,
                        description = report.description,
                        lat = report.latitude,
                        lng = report.longitude,
                        severity = report.severity,
                        reportedBy = report.reportedBy,
                        ward = "Bhopal"
                    )
                    return@withContext true
                }
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network unavailable, queueing report locally: ${e.message}")
            offlineSyncManager.enqueueReport(
                type = report.type.name,
                description = report.description,
                lat = report.latitude,
                lng = report.longitude,
                severity = report.severity,
                reportedBy = report.reportedBy,
                ward = "Bhopal"
            )
            true
        }
    }

    /**
     * Fetches verified active incidents from the backend.
     * Returns an empty list on any error so the UI degrades gracefully.
     */
    suspend fun getActiveIncidents(): List<IncidentReport> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIncidents("verified")
            if (response.isSuccessful && response.body() != null) {
                val reports = response.body()!!.map { dto ->
                    val type = runCatching {
                        IncidentType.valueOf(dto.type)
                    }.getOrDefault(IncidentType.OTHER)
                    IncidentReport(
                        id = dto.id,
                        type = type,
                        latitude = dto.lat,
                        longitude = dto.lng,
                        description = dto.description ?: "",
                        severity = dto.severity,
                        status = dto.status,
                        timestamp = dto.timestamp,
                        reportedBy = dto.reportedBy ?: "citizen"
                    )
                }
                _incidentFlow.value = reports
                return@withContext reports
            }
        } catch (e: Exception) {
            Log.w(TAG, "getActiveIncidents via apiService failed: ${e.message}")
        }

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
                    _incidentFlow.value = reports
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getActiveIncidents network error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Sends an SOS alert with the user's coordinates and emergency details.
     * If the network is unavailable, saves the SOS event in the local Room queue for immediate sync
     * upon reconnect and returns the local queued ID.
     */
    suspend fun submitSos(
        latitude: Double,
        longitude: Double,
        contact: String,
        userId: String = "anonymous",
        userName: String = "NagarSetu User",
        phone: String = "",
        ward: String = "Bhopal",
        batteryLevel: Int = -1,
        heartRate: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dto = SosRequestDto(
                userId = userId,
                userName = userName,
                phone = phone,
                lat = latitude,
                lng = longitude,
                ward = ward,
                triageLevel = "HIGH",
                batteryLevel = batteryLevel,
                heartRate = heartRate,
                networkType = "4G",
                address = contact
            )
            val response = apiService.triggerSos(dto)
            if (response.isSuccessful && response.body()?.id != null) {
                return@withContext response.body()!!.id
            }
        } catch (e: Exception) {
            Log.w(TAG, "submitSos via apiService failed, attempting raw fallback: ${e.message}")
        }

        val body = JSONObject()
            .put("userId", userId)
            .put("userName", userName)
            .put("phone", phone)
            .put("lat", latitude)
            .put("lng", longitude)
            .put("ward", ward)
            .put("triageLevel", "HIGH")
            .put("trustedContacts", JSONArray())
            .put("batteryLevel", batteryLevel)
            .put("heartRate", heartRate)
            .put("networkType", "4G")
            .put("address", contact)
            .put("fcmToken", "")
            .toString()
            .toRequestBody(JSON_MEDIA)

        val request = Request.Builder()
            .url(url("/api/sos"))
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "submitSos HTTP ${response.code}: ${response.message}")
                    offlineSyncManager.enqueueSos(
                        userId = userId,
                        userName = userName,
                        phone = phone,
                        lat = latitude,
                        lng = longitude,
                        ward = ward,
                        triageLevel = "HIGH",
                        contact = contact,
                        batteryLevel = batteryLevel,
                        heartRate = heartRate
                    )
                } else {
                    val respBody = response.body?.string() ?: return@use null
                    JSONObject(respBody).optString("id").takeIf { it.isNotBlank() }
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "submitSos offline, queueing in Room DB: ${e.message}")
            offlineSyncManager.enqueueSos(
                userId = userId,
                userName = userName,
                phone = phone,
                lat = latitude,
                lng = longitude,
                ward = ward,
                triageLevel = "HIGH",
                contact = contact,
                batteryLevel = batteryLevel,
                heartRate = heartRate
            )
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
        try {
            val dto = SosLocationUpdateDto(
                lat = latitude,
                lng = longitude,
                batteryLevel = batteryLevel,
                heartRate = heartRate
            )
            val response = apiService.updateSosLocation(sosId, dto)
            if (response.isSuccessful) return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "updateSosLocation via apiService failed: ${e.message}")
        }

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
