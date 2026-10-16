package com.nagarsetu.auth.data

import com.nagarsetu.auth.domain.model.AuthResult
import com.nagarsetu.auth.domain.model.UserProfile
import com.nagarsetu.core.data.local.PreferencesManager
import com.nagarsetu.core.data.local.ProfileLocalRepository
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for authentication.
 *
 * Flow:
 * 1. [ensureGuestSession] → Auto-called on app start. Creates a persisted guest
 *    profile if no session exists, so the app always has a user identity.
 * 2. [sendOtp]   → Sends SMS OTP (call from Profile screen to "upgrade" guest)
 * 3. [verifyOtp] → Validates OTP, upgrades profile to verified, persists session
 * 4. [getSession] → Returns cached [UserProfile]
 * 5. [logout]    → Clears session (resets to guest on next ensureGuestSession call)
 *
 * Improvements over v1:
 *  - Thread-safe guest-session creation via double-checked locking.
 *  - OTP rate-limiting: max [MAX_OTP_ATTEMPTS_PER_WINDOW] sends per phone per
 *    [OTP_WINDOW_MS] to guard against Twilio cost abuse.
 *  - OTP code length validation before hitting the network.
 *  - Cleaner `verifyOtp` null-safe chain.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val twilioOtpService: TwilioOtpService,
    private val supabaseUserRepository: SupabaseUserRepository,
    private val preferencesManager: PreferencesManager,
    private val profileLocalRepository: ProfileLocalRepository
) {
    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val MAX_OTP_ATTEMPTS_PER_WINDOW = 3
        private const val OTP_WINDOW_MS = 10 * 60 * 1000L  // 10 minutes
        private const val OTP_CODE_LENGTH = 6
        private const val DEFAULT_CITY = "Bhopal"
    }

    // ── OTP rate-limit state ──────────────────────────────────────────────────

    /** Tracks (phone → list of send timestamps) within the rolling window. */
    private val otpSendLog = ConcurrentHashMap<String, MutableList<Long>>()

    // ── Guest session ─────────────────────────────────────────────────────────

    /**
     * Returns the existing session if one is persisted (guest or verified).
     * If no session exists yet (first launch), creates and persists a guest
     * profile. Thread-safe: only one guest profile is ever created per device
     * even under concurrent calls.
     */
    @Synchronized
    fun ensureGuestSession(): UserProfile {
        val existing = preferencesManager.getUserSession()
        if (existing != null) return existing

        val guestProfile = UserProfile(
            uid = "guest_${UUID.randomUUID()}",
            phone = "",
            name = "Guest",
            city = DEFAULT_CITY,
            isGuest = true
        )
        preferencesManager.saveUserSession(guestProfile)
        return guestProfile
    }

    // ── OTP: Send ─────────────────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP SMS to [phone] (E.164 or common Indian formats accepted).
     * Returns [AuthResult.Failure] immediately if the phone is rate-limited.
     */
    suspend fun sendOtp(phone: String): AuthResult<Boolean> {
        val normalized = normalizePhone(phone)
            ?: return AuthResult.Failure("Invalid phone number. Use format: +91XXXXXXXXXX")

        if (isRateLimited(normalized)) {
            return AuthResult.Failure(
                "Too many OTP requests. Please wait a few minutes before trying again."
            )
        }

        return twilioOtpService.sendOtp(normalized).also { result ->
            if (result is AuthResult.Success) recordOtpSend(normalized)
        }
    }

    // ── OTP: Verify ───────────────────────────────────────────────────────────

    /**
     * Verifies [code] for [phone]. On success:
     *  - Validates code format before making a network call.
     *  - Fetches or creates the user's Supabase profile.
     *  - Replaces the guest session with a verified one.
     */
    suspend fun verifyOtp(phone: String, code: String): AuthResult<UserProfile> {
        val normalized = normalizePhone(phone)
            ?: return AuthResult.Failure("Invalid phone number")

        val trimmedCode = code.trim()
        if (trimmedCode.length != OTP_CODE_LENGTH || !trimmedCode.all { it.isDigit() }) {
            return AuthResult.Failure("OTP must be exactly $OTP_CODE_LENGTH digits")
        }

        return when (val otpResult = twilioOtpService.verifyOtp(normalized, trimmedCode)) {
            is AuthResult.Failure -> otpResult
            is AuthResult.Success -> buildVerifiedProfile(normalized)
        }
    }

    // ── Session management ────────────────────────────────────────────────────

    fun getSession(): UserProfile? = preferencesManager.getUserSession()

    fun isLoggedIn(): Boolean = getSession()?.isGuest == false

    fun logout() = preferencesManager.clearUserSession()

    // ── Update profile ────────────────────────────────────────────────────────

    suspend fun updateProfile(
        uid: String,
        name: String? = null,
        email: String? = null,
        ward: String? = null
    ): AuthResult<Unit> {
        val result = supabaseUserRepository.updateProfile(uid, name, email, ward)
        if (result is AuthResult.Success) {
            // Re-fetch and sync to secure local preferences
            val updatedDto = supabaseUserRepository.fetchProfile(uid)
            if (updatedDto is AuthResult.Success && updatedDto.data != null) {
                preferencesManager.saveUserSession(updatedDto.data!!)
            }
        }
        return result
    }

    suspend fun saveTrustedContactsRemote(uid: String, contacts: List<com.nagarsetu.auth.domain.model.TrustedContact>) {
        supabaseUserRepository.saveTrustedContacts(uid, contacts)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun buildVerifiedProfile(normalizedPhone: String): AuthResult<UserProfile> {
        val uid = generateUid(normalizedPhone)

        val profile = when (val fetchResult = supabaseUserRepository.fetchProfile(uid)) {
            is AuthResult.Success -> fetchResult.data ?: UserProfile(uid = uid, phone = normalizedPhone)
            is AuthResult.Failure -> UserProfile(uid = uid, phone = normalizedPhone)
        }

        // Best-effort upsert: proceed even if the remote call fails
        val upsertResult = supabaseUserRepository.upsertProfile(profile)
        val finalProfile = if (upsertResult is AuthResult.Success) upsertResult.data ?: profile else profile

        preferencesManager.saveUserSession(finalProfile)
        
        // Fetch and sync trusted contacts from Supabase to local on login
        syncTrustedContactsFromRemote(uid)
        
        return AuthResult.Success(finalProfile)
    }

    private suspend fun syncTrustedContactsFromRemote(uid: String) {
        val result = supabaseUserRepository.fetchTrustedContacts(uid)
        if (result is AuthResult.Success) {
            profileLocalRepository.saveContacts(result.data)
        }
    }

    /**
     * Normalizes Indian phone numbers to E.164 format.
     * Accepts: "9876543210", "09876543210", "+919876543210"
     */
    private fun normalizePhone(phone: String): String? {
        val digits = phone.replace(Regex("[^\\d+]"), "")
        return when {
            digits.startsWith("+91") && digits.length == 13 -> digits
            digits.startsWith("91")  && digits.length == 12 -> "+$digits"
            digits.startsWith("0")   && digits.length == 11 -> "+91${digits.drop(1)}"
            digits.length == 10                             -> "+91$digits"
            else -> null
        }
    }

    /** Deterministic UID based on phone (stable across logins). */
    private fun generateUid(normalizedPhone: String): String =
        UUID.nameUUIDFromBytes(normalizedPhone.toByteArray()).toString()

    // ── Rate limiting helpers ─────────────────────────────────────────────────

    private fun isRateLimited(normalizedPhone: String): Boolean {
        val now = System.currentTimeMillis()
        val sends = otpSendLog.getOrDefault(normalizedPhone, mutableListOf())
        val recentSends = sends.filter { now - it < OTP_WINDOW_MS }
        otpSendLog[normalizedPhone] = recentSends.toMutableList()
        return recentSends.size >= MAX_OTP_ATTEMPTS_PER_WINDOW
    }

    private fun recordOtpSend(normalizedPhone: String) {
        val now = System.currentTimeMillis()
        otpSendLog.getOrPut(normalizedPhone) { mutableListOf() }.add(now)
    }
}
