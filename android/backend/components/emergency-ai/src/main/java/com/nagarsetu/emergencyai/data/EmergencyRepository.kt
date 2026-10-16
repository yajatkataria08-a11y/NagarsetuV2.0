package com.nagarsetu.emergencyai.data

import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.emergencyai.domain.model.EmergencyEvent
import com.nagarsetu.emergencyai.domain.model.EmergencyStatus
import com.nagarsetu.emergencyai.domain.model.TriggerType
import com.nagarsetu.emergencyai.domain.triage.TriageEngine
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class EmergencyDialNumber(val label: String, val number: String)

@Singleton
class EmergencyRepository @Inject constructor(
    private val assets: AssetDataRepository,
    val triageEngine: TriageEngine,
    private val supabase: SupabaseClient,
    private val userRepository: SupabaseUserRepository
) {
    companion object {
        const val GOLDEN_HOUR_SECONDS = 3_600
        // Fix R3: use CivicConstants instead of duplicating coordinates here
        private val FALLBACK_LAT get() = CivicConstants.BHOPAL_LAT
        private val FALLBACK_LNG get() = CivicConstants.BHOPAL_LNG
    }

    val bimstecDialer = listOf(
        EmergencyDialNumber("Emergency",    "112"),
        EmergencyDialNumber("Ambulance",    "108"),
        EmergencyDialNumber("Police",       "100"),
        EmergencyDialNumber("Fire",         "101"),
        EmergencyDialNumber("Ambulance Alt","102")
    )

    private val _active = MutableStateFlow<EmergencyEvent?>(null)
    val activeEmergency: StateFlow<EmergencyEvent?> = _active.asStateFlow()

    // ── Supabase Integration ──────────────────────────────────────────────────

    /**
     * Persists an SOS event to Supabase and logs activity.
     */
    suspend fun saveEmergencyEvent(
        uid: String,
        event: EmergencyEvent,
        contactedNumbers: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Simplified insert without decodeSingle since it can be tricky with Result objects
            supabase.postgrest["emergency_events"].insert(buildJsonObject {
                put("uid",               uid)
                put("trigger_type",      event.triggerType.name)
                put("triage_level",      event.triageInfo?.priority?.name ?: "LOW")
                put("lat",               event.latitude)
                put("lng",               event.longitude)
                put("ward",              event.ward ?: "")
                put("is_resolved",       false)
            })

            userRepository.logActivity(uid, "SOS_TRIGGER", event.id, "emergency_event")
            event.id
        }
    }

    /** Marks an emergency event as resolved. */
    suspend fun resolveEmergency(eventId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["emergency_events"].update(buildJsonObject {
                    put("is_resolved", true)
                }) { filter { eq("id", eventId) } }
                Unit
            }
        }

    // ── Trigger ───────────────────────────────────────────────────────────────

    fun triggerSos(
        type: TriggerType,
        latitude: Double? = null,
        longitude: Double? = null
    ): EmergencyEvent {
        val lat = latitude  ?: FALLBACK_LAT
        val lng = longitude ?: FALLBACK_LNG

        val event = EmergencyEvent(
            id          = UUID.randomUUID().toString(),
            timestamp   = System.currentTimeMillis(),
            triggerType = type,
            latitude    = lat,
            longitude   = lng
        )
        _active.value = event
        return event
    }

    // ── State updates ─────────────────────────────────────────────────────────

    fun updateStatus(status: EmergencyStatus) {
        _active.value = _active.value?.copy(status = status)
    }

    fun applyTriage(text: String) {
        if (_active.value == null) return
        val triage = triageEngine.processInput(text)
        _active.value = _active.value?.copy(triageInfo = triage)
    }

    fun cancel() {
        _active.value = null
    }

    // ── Data queries ──────────────────────────────────────────────────────────

    fun nearestTraumaCentre(latitude: Double? = null, longitude: Double? = null): String {
        val arr = assets.loadAppData()
            .getAsJsonObject("emergencyAI")
            ?.getAsJsonArray("traumaCentres")
            ?: return "Narmada Trauma Centre"

        if (arr.size() == 0) return "Narmada Trauma Centre"

        if (latitude != null && longitude != null) {
            var bestName = "Narmada Trauma Centre"
            var bestDist = Double.MAX_VALUE
            arr.forEach { el ->
                val o = el.asJsonObject
                val tLat = o["latitude"]?.asDouble  ?: return@forEach
                val tLng = o["longitude"]?.asDouble ?: return@forEach
                val dist = haversineMeters(latitude, longitude, tLat, tLng)
                if (dist < bestDist) {
                    bestDist = dist
                    bestName = o["name"]?.asString ?: bestName
                }
            }
            return bestName
        }

        return arr[0].asJsonObject?.get("name")?.asString ?: "Narmada Trauma Centre"
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
