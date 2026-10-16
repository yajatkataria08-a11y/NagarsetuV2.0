package com.nagarsetu.reportit.data

import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.backend.core.data.CivicDataHub
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.reportit.domain.model.CivicIssueType
import com.nagarsetu.reportit.domain.model.CivicReport
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.util.Log
import com.nagarsetu.backend.reportit.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ReportItRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val hub: CivicDataHub,
    private val supabase: SupabaseClient,
    private val userRepository: SupabaseUserRepository
) {
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("raksha_api_endpoint", BuildConfig.ADMIN_BASE_URL)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            BuildConfig.ADMIN_BASE_URL
        }.removeSuffix("/")
    }
    fun generateTicketId(): String = "RI-2026-${Random.nextInt(1000, 9999)}"

    fun nearestWard(lat: Double = CivicConstants.BHOPAL_LAT, lng: Double = CivicConstants.BHOPAL_LNG): String =
        hub.nearestWard(lat, lng)?.authorityName ?: "Bhopal Municipal Corporation"

    /**
     * Submits a civic report to Supabase and logs user activity.
     */
    suspend fun submitReport(
        uid: String,
        type: CivicIssueType,
        description: String,
        lat: Double,
        lng: Double,
        photoUrl: String = ""
    ): Result<CivicReport> = withContext(Dispatchers.IO) {
        runCatching {
            val ticketId = generateTicketId()
            val ward     = nearestWard(lat, lng)

            supabase.postgrest["civic_reports"].insert(buildJsonObject {
                put("uid",         uid)
                put("issue_type",  type.name)
                put("description", description)
                put("lat",         lat)
                put("lng",         lng)
                put("ward",        ward)
                put("photo_url",   photoUrl)
                put("status",      "submitted")
            }) {
                select()
                single()
            }

            val report = CivicReport(
                ticketId    = ticketId,
                type        = type,
                description = description,
                ward        = ward,
                latitude    = lat,
                longitude   = lng,
                timestamp   = System.currentTimeMillis()
            )

            // ─── ADMIN DASHBOARD SYNC ───
            try {
                val adminBody = JSONObject()
                    .put("type", type.name)
                    .put("description", description)
                    .put("lat", lat)
                    .put("lng", lng)
                    .put("severity", 3)
                    .put("reportedBy", uid)
                    .put("reporterName", "NagarSetu User")
                    .put("ward", ward)
                    .put("photos", JSONArray())
                    .toString()
                    .toRequestBody(JSON_MEDIA)

                val request = Request.Builder()
                    .url("${baseUrl()}/api/incidents")
                    .post(adminBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ReportItRepo", "Admin Sync Failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportItRepo", "Admin Sync Error: ${e.message}")
            }

            userRepository.logActivity(uid, "REPORT_SUBMIT", ticketId, "civic_report")

            report
        }
    }
}
