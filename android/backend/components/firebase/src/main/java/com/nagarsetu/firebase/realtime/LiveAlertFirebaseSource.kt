package com.nagarsetu.firebase.realtime

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed live alert feed for the Dashboard.
 *
 * Hybrid role in NagarSetu:
 *  - **Firestore** stores ephemeral city alerts (< 24 h TTL, ~KB per doc)
 *    and pushes real-time diffs to all subscribed clients.
 *  - **Supabase** stores long-lived, auditable `incidents` rows with PostGIS
 *    geometry and officer attribution.
 *
 * Firestore collection layout:
 * ```
 * city_alerts/
 *   {alertId}/
 *     type:       "FLOOD | FIRE | TRAFFIC | HEALTH | CRIME"
 *     title:      "Heavy rainfall in Kolar"
 *     body:       "Avoid low-lying roads near Kolar reservoir."
 *     severity:   "LOW | MEDIUM | HIGH | CRITICAL"
 *     ward:       42           (optional — null means city-wide)
 *     lat:        23.2599      (optional epicentre)
 *     lng:        77.4126
 *     source:     "NDMA | OFFICER | CITIZEN | AI"
 *     created_at: Timestamp
 *     expires_at: Timestamp    (client-side TTL filter)
 *     active:     true
 * ```
 *
 * The Dashboard subscribes to the last 50 active alerts, ordered by created_at desc.
 * Old alerts are cleaned up by a Supabase Edge Function cron that reads Firestore
 * via the Admin SDK and deletes docs where `expires_at < now()`.
 */
@Singleton
class LiveAlertFirebaseSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG                  = "LiveAlertFirebaseSource"
        private const val COLLECTION_ALERTS    = "city_alerts"
        private const val MAX_LIVE_ALERTS      = 50
        private const val TTL_HOURS            = 24L
    }

    // ── Read: real-time alert stream ──────────────────────────────────────

    /**
     * Returns a [Flow] of [LiveAlert] lists that re-emits on every Firestore
     * document change. The list is sorted newest-first.
     *
     * @param wardNumber If non-null, includes ward-specific + city-wide alerts.
     *                   If null, returns only city-wide alerts (ward == null).
     */
    fun observeAlerts(wardNumber: Int? = null): Flow<List<LiveAlert>> = callbackFlow {
        var registration: ListenerRegistration? = null

        val query = firestore.collection(COLLECTION_ALERTS)
            .whereEqualTo("active", true)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(MAX_LIVE_ALERTS.toLong())

        registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore listen error: ${error.message}", error)
                return@addSnapshotListener
            }
            val now = System.currentTimeMillis()
            val alerts = snapshot?.documents?.mapNotNull { doc ->
                runCatching {
                    val expiresAt = doc.getDate("expires_at")?.time ?: Long.MAX_VALUE
                    if (expiresAt < now) return@runCatching null  // client-side TTL filter

                    val ward = doc.getLong("ward")?.toInt()
                    // Filter by ward if specified — include city-wide (null) and matching ward
                    if (wardNumber != null && ward != null && ward != wardNumber) {
                        return@runCatching null
                    }

                    LiveAlert(
                        id        = doc.id,
                        type      = doc.getString("type") ?: "INFO",
                        title     = doc.getString("title").orEmpty(),
                        body      = doc.getString("body").orEmpty(),
                        severity  = AlertSeverity.fromString(doc.getString("severity")),
                        ward      = ward,
                        lat       = doc.getDouble("lat"),
                        lng       = doc.getDouble("lng"),
                        source    = doc.getString("source") ?: "SYSTEM",
                        createdAt = doc.getDate("created_at")?.time ?: now
                    )
                }.getOrNull()
            }?.filterNotNull() ?: emptyList()

            trySend(alerts)
        }

        awaitClose { registration?.remove() }
    }

    // ── Write: publish a new alert ────────────────────────────────────────

    /**
     * Publishes a new city alert to Firestore.
     * Typically called by:
     *  - Supabase Edge Function after an officer approves an incident
     *  - AI triage system detecting a hazard pattern
     *
     * Returns the new alert's Firestore document ID, or null on failure.
     */
    suspend fun publishAlert(alert: LiveAlert): String? {
        return runCatching {
            val now = com.google.firebase.Timestamp.now()
            val expiresAt = com.google.firebase.Timestamp(
                now.seconds + TTL_HOURS * 3600, 0
            )
            val data = hashMapOf(
                "type"       to alert.type,
                "title"      to alert.title,
                "body"       to alert.body,
                "severity"   to alert.severity.name,
                "ward"       to alert.ward,
                "lat"        to alert.lat,
                "lng"        to alert.lng,
                "source"     to alert.source,
                "active"     to true,
                "created_at" to now,
                "expires_at" to expiresAt
            )
            val ref = firestore.collection(COLLECTION_ALERTS).add(data).await()
            Log.i(TAG, "Alert published: ${ref.id} type=${alert.type}")
            ref.id
        }.onFailure { e ->
            Log.e(TAG, "publishAlert failed: ${e.message}", e)
        }.getOrNull()
    }

    /**
     * Marks an alert as inactive (soft-delete).
     * Used when an officer resolves an incident.
     */
    suspend fun deactivateAlert(alertId: String) {
        runCatching {
            firestore.collection(COLLECTION_ALERTS).document(alertId)
                .update("active", false).await()
            Log.d(TAG, "Alert deactivated: $alertId")
        }.onFailure { e ->
            Log.e(TAG, "deactivateAlert failed: ${e.message}", e)
        }
    }
}

// ── Data models ────────────────────────────────────────────────────────────

data class LiveAlert(
    val id: String        = "",
    val type: String      = "INFO",
    val title: String     = "",
    val body: String      = "",
    val severity: AlertSeverity = AlertSeverity.LOW,
    val ward: Int?        = null,
    val lat: Double?      = null,
    val lng: Double?      = null,
    val source: String    = "SYSTEM",
    val createdAt: Long   = System.currentTimeMillis()
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL;

    companion object {
        fun fromString(s: String?) = when (s?.uppercase()) {
            "MEDIUM"   -> MEDIUM
            "HIGH"     -> HIGH
            "CRITICAL" -> CRITICAL
            else       -> LOW
        }
    }
}
