package com.nagarsetu.auth.data

import android.util.Log
import com.nagarsetu.firebase.fcm.SupabaseTokenUploader
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase-backed implementation of [SupabaseTokenUploader].
 *
 * This is the **bridge** between Firebase FCM and Supabase profiles.
 *
 * When the device receives a new FCM token (app install / token rotation),
 * [NagarSetuFirebaseMessagingService.onNewToken] calls this to update
 * `profiles.fcm_token` in Supabase so the server can target the device.
 *
 * The hybrid model boundary:
 *  Firebase owns the token lifecycle → emits new tokens.
 *  Supabase owns user identity       → stores tokens against uid.
 *  Server uses Supabase to look up token → Firebase Admin SDK sends push.
 */
@Singleton
class SupabaseTokenUploaderImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SupabaseTokenUploader {

    companion object {
        private const val TAG = "SupabaseTokenUploader"
    }

    /**
     * Upserts `fcm_token` for the given `uid` into the `profiles` table.
     *
     * Uses a partial update (PATCH) via Supabase PostgREST:
     * ```
     * PATCH /rest/v1/profiles?uid=eq.<uid>
     * { "fcm_token": "<token>", "updated_at": "now()" }
     * ```
     */
    override suspend fun upsertFcmToken(uid: String, token: String): Boolean {
        return runCatching {
            supabaseClient.postgrest["profiles"]
                .update(FcmTokenUpdate(fcm_token = token)) {
                    filter {
                        eq("uid", uid)
                    }
                }
            Log.d(TAG, "FCM token updated in Supabase for uid=$uid")
            true
        }.onFailure { e ->
            Log.e(TAG, "upsertFcmToken failed for uid=$uid: ${e.message}", e)
        }.getOrDefault(false)
    }

    @Serializable
    private data class FcmTokenUpdate(
        val fcm_token: String
    )
}
