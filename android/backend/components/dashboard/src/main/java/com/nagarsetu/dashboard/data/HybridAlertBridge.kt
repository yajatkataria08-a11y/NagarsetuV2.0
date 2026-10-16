package com.nagarsetu.dashboard.data

import android.util.Log
import com.nagarsetu.firebase.realtime.AlertSeverity
import com.nagarsetu.firebase.realtime.LiveAlert
import com.nagarsetu.firebase.realtime.LiveAlertFirebaseSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges two real-time alert streams into a single deduplicated feed:
 *
 * 1. **Supabase Realtime** (`live_alerts` table) — officer-posted alerts,
 *    AI-triaged incidents elevated to alerts, persisted for audit.
 *    Represented as [LiveAlertDto] from DashboardRepository.
 *
 * 2. **Firebase Firestore** (`city_alerts` collection) — ephemeral broadcasts
 *    (< 24 h TTL), NDMA disaster alerts, and city-wide FCM-triggered events.
 *    Represented as [LiveAlert] from LiveAlertFirebaseSource.
 *
 * The hybrid model rationale:
 *  - Supabase Realtime excels at row-level change events on structured data.
 *    It integrates naturally with PostGIS and RLS policies.
 *  - Firestore excels at fan-out reads — a single write is pushed to 10k+
 *    clients in < 100 ms, which is essential for city-wide emergency broadcasts.
 *  - Together they cover both the "rich civic data" and "fast broadcast" cases.
 *
 * [observeMergedAlerts] returns a Flow that emits a combined, deduplicated
 * list every time either source changes. The merged list is sorted newest-first.
 *
 * @param wardNumber Optional ward filter. When set, includes ward-specific +
 *                   city-wide alerts from Firestore. Supabase alerts are already
 *                   filtered server-side via RLS.
 */
@Singleton
class HybridAlertBridge @Inject constructor(
    private val firebaseSource: LiveAlertFirebaseSource
) {
    companion object {
        private const val TAG = "HybridAlertBridge"
        private const val MAX_MERGED_ALERTS = 80
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Unified state exposed to ViewModel ────────────────────────────────

    private val _mergedAlerts = MutableStateFlow<List<UnifiedAlert>>(emptyList())
    val mergedAlerts: StateFlow<List<UnifiedAlert>> = _mergedAlerts.asStateFlow()

    /**
     * Subscribes to Firebase Firestore alerts for [wardNumber].
     * Supabase alerts are injected externally via [mergeSupabaseAlerts].
     *
     * Call from DashboardRepository.init{} or on login when ward is known.
     */
    fun startFirestoreSubscription(wardNumber: Int? = null) {
        firebaseSource.observeAlerts(wardNumber)
            .catch { e -> Log.e(TAG, "Firestore alert stream error: ${e.message}", e) }
            .onEach { firestoreAlerts ->
                val current = _mergedAlerts.value
                    .filter { it.source != AlertSource.FIREBASE_FIRESTORE }
                val fromFirestore = firestoreAlerts.map { it.toUnified() }
                _mergedAlerts.value = (current + fromFirestore)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt }
                    .take(MAX_MERGED_ALERTS)
            }
            .launchIn(scope)
    }

    /**
     * Called by DashboardRepository whenever Supabase Realtime delivers new
     * or updated alerts. Merges them into [_mergedAlerts] by deduplicating on id.
     */
    fun mergeSupabaseAlerts(supabaseAlerts: List<SupabaseAlertInput>) {
        val current = _mergedAlerts.value
            .filter { it.source != AlertSource.SUPABASE_REALTIME }
        val fromSupabase = supabaseAlerts.map { it.toUnified() }
        _mergedAlerts.value = (current + fromSupabase)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            .take(MAX_MERGED_ALERTS)
    }
}

// ── Data transfer types ────────────────────────────────────────────────────

/** Unified alert presentation model — source-agnostic. */
data class UnifiedAlert(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val severity: AlertSeverity,
    val ward: Int?,
    val lat: Double?,
    val lng: Double?,
    val source: AlertSource,
    val createdAt: Long
)

enum class AlertSource {
    SUPABASE_REALTIME,      // Persisted, auditable (officers, AI triage)
    FIREBASE_FIRESTORE,     // Ephemeral, fast broadcast (< 24 h TTL)
    LOCAL_MOCK              // Offline fallback from app_data.json
}

/** Lightweight DTO carrying just what HybridAlertBridge needs from Supabase. */
data class SupabaseAlertInput(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val ward: Int?,
    val lat: Double?,
    val lng: Double?,
    val createdAt: Long
)

// ── Extension converters ───────────────────────────────────────────────────

private fun LiveAlert.toUnified() = UnifiedAlert(
    id         = id.ifEmpty { "fb_${createdAt}" },
    type       = type,
    title      = title,
    body       = body,
    severity   = severity,
    ward       = ward,
    lat        = lat,
    lng        = lng,
    source     = AlertSource.FIREBASE_FIRESTORE,
    createdAt  = createdAt
)

private fun SupabaseAlertInput.toUnified() = UnifiedAlert(
    id         = id,
    type       = type,
    title      = title,
    body       = description,
    severity   = AlertSeverity.fromString(severity),
    ward       = ward,
    lat        = lat,
    lng        = lng,
    source     = AlertSource.SUPABASE_REALTIME,
    createdAt  = createdAt
)
