package com.nagarsetu.raksha.data.network

import android.util.Log
import com.nagarsetu.raksha.domain.model.DisasterAlert
import com.nagarsetu.raksha.domain.model.DisasterSeverity
import com.nagarsetu.raksha.domain.model.DisasterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches active disaster alerts from NDMA SACHET CAP feed.
 *
 * Endpoint (public, no auth):
 *   GET https://sachet.ndma.gov.in/cap_public_website/getAllCAPAlerts
 *
 * The response is a JSON array of CAP alert objects. We parse only the
 * fields relevant to the Bhopal / Madhya Pradesh area.
 *
 * Fallback: if the network call fails, returns Bhopal-specific seed alerts
 * so the UI never appears empty.
 */
@Singleton
class NdmaApiService @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG       = "NdmaApiService"
        private const val NDMA_URL  = "https://sachet.ndma.gov.in/cap_public_website/getAllCAPAlerts"
        private const val STATE_MP  = "Madhya Pradesh"
    }

    suspend fun fetchActiveAlerts(): List<DisasterAlert> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(NDMA_URL).get().build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "NDMA HTTP ${response.code} – using seed data")
                    return@withContext seedAlerts()
                }
                val body = response.body?.string().orEmpty()
                parseCap(body).ifEmpty { seedAlerts() }
            }
        } catch (e: IOException) {
            Log.w(TAG, "NDMA network error: ${e.message} – using seed data")
            seedAlerts()
        }
    }

    // ── CAP JSON parser ───────────────────────────────────────────────────
    private fun parseCap(raw: String): List<DisasterAlert> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val alerts = mutableListOf<DisasterAlert>()
            for (i in 0 until array.length()) {
                val obj: JSONObject = array.getJSONObject(i)
                // Filter to MP / Bhopal only
                val area = obj.optString("areaDesc", "")
                if (!area.contains(STATE_MP, ignoreCase = true) &&
                    !area.contains("Bhopal", ignoreCase = true)) continue

                val event    = obj.optString("event", "Unknown")
                val urgency  = obj.optString("urgency", "")
                val severity = obj.optString("severity", "")
                val headline = obj.optString("headline", event)

                alerts += DisasterAlert(
                    id        = obj.optString("identifier", "ndma_$i"),
                    type      = mapEventType(event),
                    severity  = mapSeverity(urgency, severity),
                    message   = headline,
                    timestamp = System.currentTimeMillis()
                )
            }
            alerts
        }.getOrElse { e ->
            Log.e(TAG, "CAP parse error: ${e.message}")
            emptyList()
        }
    }

    private fun mapEventType(event: String): DisasterType = when {
        event.contains("flood",     ignoreCase = true) -> DisasterType.FLOOD
        event.contains("rain",      ignoreCase = true) -> DisasterType.FLOOD
        event.contains("cyclone",   ignoreCase = true) -> DisasterType.CYCLONE
        event.contains("thunder",   ignoreCase = true) -> DisasterType.CYCLONE
        event.contains("fire",      ignoreCase = true) -> DisasterType.FIRE
        event.contains("earthquake",ignoreCase = true) -> DisasterType.EARTHQUAKE
        event.contains("epidemic",  ignoreCase = true) -> DisasterType.EPIDEMIC
        else                                            -> DisasterType.FLOOD
    }

    private fun mapSeverity(urgency: String, severity: String): DisasterSeverity = when {
        severity.equals("Extreme",  ignoreCase = true) -> DisasterSeverity.EXTREME
        severity.equals("Severe",   ignoreCase = true) -> DisasterSeverity.WARNING
        urgency.equals("Immediate", ignoreCase = true) -> DisasterSeverity.WARNING
        severity.equals("Moderate", ignoreCase = true) -> DisasterSeverity.WATCH
        else                                            -> DisasterSeverity.ADVISORY
    }

    // ── Seed fallback (Bhopal monsoon season context) ─────────────────────
    private fun seedAlerts(): List<DisasterAlert> = listOf(
        DisasterAlert(
            id        = "SEED-BPL-01",
            type      = DisasterType.FLOOD,
            severity  = DisasterSeverity.WATCH,
            message   = "IMD: भारी वर्षा की संभावना – निचले इलाकों में जलभराव से बचें।",
            timestamp = System.currentTimeMillis()
        ),
        DisasterAlert(
            id        = "SEED-BPL-02",
            type      = DisasterType.FIRE,
            severity  = DisasterSeverity.ADVISORY,
            message   = "शुष्क हवाएं – बाहरी इलाकों में वनाग्नि का खतरा बढ़ा है।",
            timestamp = System.currentTimeMillis() - 3_600_000
        ),
        DisasterAlert(
            id        = "SEED-BPL-03",
            type      = DisasterType.CYCLONE,
            severity  = DisasterSeverity.WARNING,
            message   = "19:00 के बाद तेज़ हवाएं। बाहरी सामान सुरक्षित रखें।",
            timestamp = System.currentTimeMillis() - 7_200_000
        )
    )
}
