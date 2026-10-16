package com.nagarsetu.auth.domain.model

/**
 * Represents the authenticated user profile stored in Supabase.
 */
data class UserProfile(
    val uid: String,
    val phone: String,
    val name: String = "",
    val email: String = "",
    val city: String = "Bhopal",
    val ward: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val isGuest: Boolean = false,          // ← NEW: marks guest sessions
    val firebaseUid: String? = null        // ← NEW: for hybrid linking
)

/** Possible states of the OTP / login flow. */
sealed class OtpState {
    object Idle : OtpState()
    object SendingOtp : OtpState()
    data class OtpSent(val phone: String, val maskedPhone: String) : OtpState()
    object VerifyingOtp : OtpState()
    data class Verified(val profile: UserProfile) : OtpState()
    data class Error(val message: String) : OtpState()
    object GuestMode : OtpState()         // ← NEW: guest entered without OTP
}

/** Result wrapper for auth operations. */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Failure(val error: String, val code: Int = -1) : AuthResult<Nothing>()
}
