package com.nagarsetu.firebase.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the FCM registration token lifecycle.
 *
 * Hybrid role:
 *  - Firebase supplies the push token (FCM)
 *  - Supabase stores it in `profiles.fcm_token` so the server can target
 *    individual devices when sending ward alerts or SOS confirmations.
 *
 * Call [syncTokenToSupabase] right after:
 *  1. User completes Supabase OTP login
 *  2. App foregrounds and token may have been refreshed
 *
 * The token is also refreshed automatically by [NagarSetuFirebaseMessagingService]
 * when Firebase rotates it.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val messaging: FirebaseMessaging
) {
    companion object {
        private const val TAG = "FcmTokenManager"

        // Supabase RPC / REST endpoint to upsert the token
        // Replace with your Supabase project URL
        const val SUPABASE_UPDATE_TOKEN_PATH = "/rest/v1/profiles"
    }

    /**
     * Retrieves the current FCM registration token.
     * Returns null on failure so the caller can retry.
     */
    suspend fun getToken(): String? = runCatching {
        messaging.token.await()
    }.onFailure { e ->
        Log.e(TAG, "getToken failed: ${e.message}", e)
    }.getOrNull()

    /**
     * Fetches the current FCM token and upserts it into Supabase
     * `profiles.fcm_token` for the given [uid].
     *
     * The actual HTTP call is delegated to [SupabaseTokenUploader] which
     * is wired separately so this class stays testable without a real DB.
     */
    suspend fun syncTokenToSupabase(
        uid: String,
        uploader: SupabaseTokenUploader
    ) {
        val token = getToken() ?: return
        Log.d(TAG, "Syncing FCM token for uid=$uid")
        val success = uploader.upsertFcmToken(uid = uid, token = token)
        if (success) {
            Log.i(TAG, "FCM token synced to Supabase ✓")
        } else {
            Log.w(TAG, "FCM token sync failed — will retry on next launch")
        }
    }

    /**
     * Subscribes the device to a topic so ward officers can broadcast
     * to all citizens in a ward without needing individual tokens.
     *
     * Example topic: "ward_42_alerts"
     */
    suspend fun subscribeToWardTopic(wardNumber: Int) {
        runCatching {
            messaging.subscribeToTopic("ward_${wardNumber}_alerts").await()
            Log.i(TAG, "Subscribed to ward_${wardNumber}_alerts")
        }.onFailure { e ->
            Log.e(TAG, "Topic subscription failed: ${e.message}", e)
        }
    }

    suspend fun unsubscribeFromWardTopic(wardNumber: Int) {
        runCatching {
            messaging.unsubscribeFromTopic("ward_${wardNumber}_alerts").await()
        }.onFailure { e ->
            Log.e(TAG, "Topic unsubscription failed: ${e.message}", e)
        }
    }

    /** Subscribe to city-wide broadcast channel (all NagarSetu users). */
    suspend fun subscribeToCityChannel() {
        runCatching {
            messaging.subscribeToTopic("nagarsetu_bhopal_all").await()
            Log.i(TAG, "Subscribed to city-wide channel")
        }.onFailure { e ->
            Log.e(TAG, "City channel subscription failed: ${e.message}", e)
        }
    }
}

/**
 * Interface so [FcmTokenManager] doesn't directly depend on the Supabase SDK.
 * Implement this in the auth backend module where the Supabase client lives.
 */
interface SupabaseTokenUploader {
    /** Updates profiles.fcm_token = [token] WHERE uid = [uid]. Returns true on success. */
    suspend fun upsertFcmToken(uid: String, token: String): Boolean
}
