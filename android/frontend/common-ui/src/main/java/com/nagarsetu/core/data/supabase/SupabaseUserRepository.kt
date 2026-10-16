package com.nagarsetu.core.data.supabase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nagarsetu.auth.domain.model.AuthResult
import com.nagarsetu.auth.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

// ── DataStore extension ───────────────────────────────────────────────────────
private val Context.userProfileDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "nagarsetu_user_profile")

// ─────────────────────────────────────────────────────────────────────────────
// Data Transfer Object — matches Supabase `profiles` table exactly
// (camelCase ↔ snake_case handled by @SerialName)
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class ProfileDto(
    val uid: String,
    val phone: String,
    val name: String = "",
    val email: String = "",
    val city: String = "Bhopal",
    val ward: String = "",
    @SerialName("ward_number") val wardNumber: Int? = null,
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_guest") val isGuest: Boolean = false,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
    val role: String = "citizen",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toDomain() = UserProfile(
        uid        = uid,
        phone      = phone,
        name       = name,
        email      = email,
        city       = city,
        ward       = ward,
        avatarUrl  = avatarUrl,
        isVerified = isVerified,
        isGuest    = isGuest,
        firebaseUid = firebaseUid,
        createdAt  = System.currentTimeMillis()
    )
}

@Serializable
data class TrustedContactRemoteDto(
    val name: String,
    val phone: String,
    val relation: String = ""
) {
    fun toDomain() = com.nagarsetu.auth.domain.model.TrustedContact(
        name = name,
        phone = phone,
        relation = relation
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class SupabaseUserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // ── In-memory profile cache ───────────────────────────────────────────────
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profileFlow: StateFlow<UserProfile?> = _profile.asStateFlow()

    // ── DataStore keys ────────────────────────────────────────────────────────
    private val KEY_PROFILE_JSON   = stringPreferencesKey("cached_profile")
    private val KEY_CACHE_TIMESTAMP = longPreferencesKey("cache_ts")
    private val CACHE_TTL_MS       = 30 * 60 * 1000L   // 30 minutes

    // ── Supabase table handles ────────────────────────────────────────────────
    private val profileTable get() = supabase.postgrest["profiles"]

    init {
        // Pre-warm from DataStore on startup
        scope.launch { _profile.value = readFromDataStore() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Upserts profile after successful OTP verification.
     * Retries up to 3 times with exponential back-off.
     */
    suspend fun upsertProfile(profile: UserProfile): AuthResult<UserProfile> =
        withContext(Dispatchers.IO) {
            retryWithBackoff(maxAttempts = 3) {
                val dto = profile.toDto()
                profileTable.upsert(dto)
                val upserted = profile.copy(isVerified = true)
                cacheProfile(upserted)
                AuthResult.Success(upserted)
            }
        }

    /**
     * Fetches profile by uid. Uses cache if fresh (<30 min).
     * Falls back to cache on network error.
     */
    suspend fun fetchProfile(uid: String): AuthResult<UserProfile?> =
        withContext(Dispatchers.IO) {
            val cached = _profile.value
            if (cached != null && isCacheFresh()) {
                return@withContext AuthResult.Success(cached)
            }
            retryWithBackoff(maxAttempts = 3) {
                val dto = profileTable
                    .select(Columns.ALL) { filter { eq("uid", uid) } }
                    .decodeSingleOrNull<ProfileDto>()
                val domain = dto?.toDomain()
                if (domain != null) cacheProfile(domain)
                AuthResult.Success(domain)
            }.recoverFromCache()
        }

    /**
     * Updates mutable profile fields. Optimistically updates cache.
     */
    suspend fun updateProfile(
        uid: String,
        name: String?      = null,
        email: String?     = null,
        ward: String?      = null,
        avatarUrl: String? = null,
        fcmToken: String?  = null,
        firebaseUid: String? = null
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        // Optimistic local update
        _profile.value?.let { current ->
            val updated = current.copy(
                name        = name        ?: current.name,
                email       = email       ?: current.email,
                ward        = ward        ?: current.ward,
                avatarUrl   = avatarUrl   ?: current.avatarUrl,
                firebaseUid = firebaseUid ?: current.firebaseUid
            )
            cacheProfile(updated)
        }

        retryWithBackoff(maxAttempts = 2) {
            val patch = buildJsonObject {
                name?.let        { put("name", it) }
                email?.let       { put("email", it) }
                ward?.let        { put("ward", it) }
                avatarUrl?.let   { put("avatar_url", it) }
                fcmToken?.let    { put("fcm_token", it) }
                firebaseUid?.let { put("firebase_uid", it) }
            }
            profileTable.update(patch) { filter { eq("uid", uid) } }
            AuthResult.Success(Unit)
        }
    }

    /**
     * Uploads avatar photo to Supabase Storage and returns the public URL.
     * Path: avatars/{uid}/avatar.jpg
     */
    suspend fun uploadAvatar(uid: String, imageBytes: ByteArray): AuthResult<String> =
        withContext(Dispatchers.IO) {
            retryWithBackoff(maxAttempts = 2) {
                val bucket = supabase.storage["avatars"]
                val path = "$uid/avatar.jpg"
                bucket.upload(path, imageBytes, upsert = true)
                val publicUrl = bucket.publicUrl(path)
                updateProfile(uid, avatarUrl = publicUrl)
                AuthResult.Success(publicUrl)
            }
        }

    /**
     * Subscribes to real-time changes on this user's profile row.
     * Emits updated [UserProfile] whenever the DB row changes.
     * Call from a ViewModel's viewModelScope.
     */
    fun observeProfile(uid: String): Flow<UserProfile> {
        val channel = supabase.realtime.channel("profile:$uid")
        return channel
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table  = "profiles"
                filter = "uid=eq.$uid"
            }
            .map { action ->
                val dto = json.decodeFromString<ProfileDto>(action.record.toString())
                dto.toDomain().also { cacheProfile(it) }
            }
    }

    /**
     * Starts the real-time profile subscription.
     * Typically called once from AuthRepository after login.
     */
    fun startProfileSync(uid: String) {
        scope.launch {
            runCatching {
                val channel = supabase.realtime.channel("profile_sync:$uid")
                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table  = "profiles"
                    filter = "uid=eq.$uid"
                }.collect { action ->
                    val dto = json.decodeFromString<ProfileDto>(action.record.toString())
                    cacheProfile(dto.toDomain())
                }
            }
        }
    }

    /** Saves an SOS emergency event to Supabase. */
    suspend fun uploadEmergencyEvent(
        uid: String,
        triggerType: String,
        lat: Double,
        lng: Double,
        triageLevel: String,
        contactedNumbers: List<String> = emptyList()
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        retryWithBackoff(maxAttempts = 3) {
            supabase.postgrest["emergency_events"].insert(
                buildJsonObject {
                    put("uid", uid)
                    put("trigger_type", triggerType)
                    put("lat", lat)
                    put("lng", lng)
                    put("triage_level", triageLevel)
                }
            )
            AuthResult.Success(Unit)
        }
    }

    /** Logs a user activity event. Fire-and-forget (best-effort). */
    fun logActivity(
        uid: String,
        eventType: String,
        entityId: String? = null,
        entityType: String? = null
    ) {
        scope.launch {
            runCatching {
                supabase.postgrest["activity_logs"].insert(
                    buildJsonObject {
                        put("uid", uid)
                        put("event_type", eventType)
                        entityId?.let   { put("entity_id", it) }
                        entityType?.let { put("entity_type", it) }
                    }
                )
            }
        }
    }

    /** Saves trusted contacts to Supabase. */
    suspend fun saveTrustedContacts(uid: String, contacts: List<com.nagarsetu.auth.domain.model.TrustedContact>): AuthResult<Unit> = withContext(Dispatchers.IO) {
        retryWithBackoff(maxAttempts = 2) {
            // Delete existing ones for this user first (simple sync)
            supabase.postgrest["trusted_contacts"].delete { filter { eq("uid", uid) } }
            
            // Insert new ones
            if (contacts.isNotEmpty()) {
                val data = contacts.map { contact ->
                    buildJsonObject {
                        put("uid", uid)
                        put("name", contact.name)
                        put("phone", contact.phone)
                        put("relation", contact.relation)
                    }
                }
                supabase.postgrest["trusted_contacts"].insert(data)
            }
            AuthResult.Success(Unit)
        }
    }

    /** Fetches trusted contacts from Supabase. */
    suspend fun fetchTrustedContacts(uid: String): AuthResult<List<com.nagarsetu.auth.domain.model.TrustedContact>> = withContext(Dispatchers.IO) {
        retryWithBackoff(maxAttempts = 2) {
            val list = supabase.postgrest["trusted_contacts"]
                .select(Columns.ALL) { filter { eq("uid", uid) } }
                .decodeList<TrustedContactRemoteDto>()
            AuthResult.Success(list.map { it.toDomain() })
        }
    }

    /** Clears cached profile (call on logout). */
    fun clearCache() {
        _profile.value = null
        scope.launch {
            context.userProfileDataStore.edit { it.clear() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun cacheProfile(profile: UserProfile) {
        _profile.value = profile
        context.userProfileDataStore.edit { prefs ->
            prefs[KEY_PROFILE_JSON]    = json.encodeToString(profile.toDto())
            prefs[KEY_CACHE_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    private suspend fun readFromDataStore(): UserProfile? {
        val prefs = context.userProfileDataStore.data.first()
        val jsonStr = prefs[KEY_PROFILE_JSON] ?: return null
        return runCatching {
            json.decodeFromString<ProfileDto>(jsonStr).toDomain()
        }.getOrNull()
    }

    private suspend fun isCacheFresh(): Boolean {
        val prefs = context.userProfileDataStore.data.first()
        val ts = prefs[KEY_CACHE_TIMESTAMP] ?: return false
        return System.currentTimeMillis() - ts < CACHE_TTL_MS
    }

    /**
     * Retries [block] up to [maxAttempts] times with exponential back-off.
     * 1st retry: 500ms, 2nd: 1000ms, 3rd: 2000ms.
     */
    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        block: suspend () -> AuthResult<T>
    ): AuthResult<T> {
        var lastResult: AuthResult<T> = AuthResult.Failure("Unknown error")
        repeat(maxAttempts) { attempt ->
            try {
                val result = block()
                if (result is AuthResult.Success) return result
                lastResult = result
            } catch (e: Exception) {
                lastResult = AuthResult.Failure(
                    e.message ?: "Network error",
                    code = if (e.message?.contains("timeout", true) == true) 408 else 500
                )
                if (attempt < maxAttempts - 1) {
                    delay((500L * 2.0.pow(attempt)).toLong())
                }
            }
        }
        return lastResult
    }

    /** If a network call fails, return cached profile instead of an error. */
    private fun AuthResult<UserProfile?>.recoverFromCache(): AuthResult<UserProfile?> {
        return if (this is AuthResult.Failure) {
            val cached = _profile.value
            if (cached != null) AuthResult.Success(cached) else this
        } else this
    }

    // ── Domain → DTO mapper ───────────────────────────────────────────────────
    private fun UserProfile.toDto() = ProfileDto(
        uid        = uid,
        phone      = phone,
        name       = name,
        email      = email,
        city       = city,
        ward       = ward,
        avatarUrl  = avatarUrl,
        isVerified = isVerified,
        isGuest    = isGuest,
        firebaseUid = firebaseUid
    )
}
