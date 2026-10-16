package com.nagarsetu.firebase.realtime

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Realtime Database repository for live GPS tracking.
 *
 * Used by the Raksha "LiveTrack" feature where a user in distress shares
 * their real-time location with trusted contacts.
 *
 * Firebase RTDB path layout:
 * ```
 * live_tracks/
 *   {sessionId}/
 *     owner_uid:   "supabase-uid"
 *     started_at:  1716638400000
 *     expires_at:  1716642000000
 *     locations/
 *       {pushKey}/
 *         lat:       23.2599
 *         lng:       77.4126
 *         accuracy:  8.5
 *         timestamp: 1716638405000
 * ```
 *
 * Why Firebase RTDB instead of Supabase Realtime?
 * - Sub-second latency for GPS pushes (RTDB is optimised for small, fast writes)
 * - Built-in TTL via `.priority` / Security Rules server-side delete
 * - Offline persistence + sync on reconnect out of the box
 * - Supabase Realtime is better suited for row-change events on structured tables
 *
 * Supabase still stores the final resolved track (when session ends) in the
 * `live_sessions` table so officers can audit after the fact.
 */
@Singleton
class GpsTrackingRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val TAG = "GpsTrackingRepository"
        private const val LIVE_TRACKS_PATH = "live_tracks"
        private const val MAX_LOCATIONS_PER_SESSION = 1000
    }

    // ── Write path (tracking device) ──────────────────────────────────────

    /**
     * Creates a new live-track session and returns the [sessionId].
     * The caller should then push GPS points via [pushLocation].
     *
     * @param ownerUid  Supabase uid of the user sharing location
     * @param durationMs How long the session should stay active (default 1 h)
     */
    suspend fun startSession(
        ownerUid: String,
        durationMs: Long = 3_600_000L
    ): String {
        val sessionRef = database.getReference(LIVE_TRACKS_PATH).push()
        val sessionId  = sessionRef.key ?: throw IllegalStateException("RTDB push() returned null key")
        val now = System.currentTimeMillis()

        sessionRef.setValue(
            mapOf(
                "owner_uid"  to ownerUid,
                "started_at" to now,
                "expires_at" to now + durationMs,
                "active"     to true
            )
        ).await()

        Log.i(TAG, "Live-track session started: $sessionId for uid=$ownerUid")
        return sessionId
    }

    /**
     * Appends a GPS point to an active session.
     * Returns false if the write fails (network error, session expired).
     */
    suspend fun pushLocation(
        sessionId: String,
        lat: Double,
        lng: Double,
        accuracyMeters: Float
    ): Boolean {
        return runCatching {
            val locRef = database
                .getReference("$LIVE_TRACKS_PATH/$sessionId/locations")
                .push()
            locRef.setValue(
                mapOf(
                    "lat"       to lat,
                    "lng"       to lng,
                    "accuracy"  to accuracyMeters,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            true
        }.onFailure { e ->
            Log.e(TAG, "pushLocation failed for session=$sessionId: ${e.message}", e)
        }.getOrDefault(false)
    }

    /**
     * Marks a session as inactive (user ends sharing).
     * Does NOT delete the data — auditors can still read it.
     */
    suspend fun endSession(sessionId: String) {
        runCatching {
            database.getReference("$LIVE_TRACKS_PATH/$sessionId/active")
                .setValue(false)
                .await()
            Log.i(TAG, "Live-track session ended: $sessionId")
        }.onFailure { e ->
            Log.e(TAG, "endSession failed: ${e.message}", e)
        }
    }

    // ── Read path (observers / trusted contacts) ───────────────────────────

    /**
     * Emits the latest [GpsPoint] whenever the owner pushes a new location.
     * Cancels when the collector's scope is cancelled.
     */
    fun observeLatestLocation(sessionId: String): Flow<GpsPoint?> = callbackFlow {
        val locRef = database.getReference("$LIVE_TRACKS_PATH/$sessionId/locations")

        // Listen to the very last child added (most recent GPS point)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val last = snapshot.children.lastOrNull()
                val point = last?.let {
                    runCatching {
                        GpsPoint(
                            lat       = it.child("lat").getValue(Double::class.java) ?: 0.0,
                            lng       = it.child("lng").getValue(Double::class.java) ?: 0.0,
                            accuracy  = it.child("accuracy").getValue(Float::class.java) ?: 0f,
                            timestamp = it.child("timestamp").getValue(Long::class.java)
                                            ?: System.currentTimeMillis()
                        )
                    }.getOrNull()
                }
                trySend(point)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeLatestLocation cancelled: ${error.message}")
                close(error.toException())
            }
        }

        locRef.addValueEventListener(listener)
        awaitClose { locRef.removeEventListener(listener) }
    }

    /**
     * Returns metadata for a session (active flag, owner, expiry).
     * Null if the session does not exist.
     */
    suspend fun getSessionInfo(sessionId: String): SessionInfo? {
        return runCatching {
            val snap = database.getReference("$LIVE_TRACKS_PATH/$sessionId").get().await()
            SessionInfo(
                sessionId  = sessionId,
                ownerUid   = snap.child("owner_uid").getValue(String::class.java).orEmpty(),
                startedAt  = snap.child("started_at").getValue(Long::class.java) ?: 0L,
                expiresAt  = snap.child("expires_at").getValue(Long::class.java) ?: 0L,
                isActive   = snap.child("active").getValue(Boolean::class.java) ?: false
            )
        }.onFailure { e ->
            Log.e(TAG, "getSessionInfo failed: ${e.message}", e)
        }.getOrNull()
    }
}

// ── Data models ────────────────────────────────────────────────────────────

data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)

data class SessionInfo(
    val sessionId: String,
    val ownerUid: String,
    val startedAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)
