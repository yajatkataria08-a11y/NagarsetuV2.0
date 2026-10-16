package com.nagarsetu.auth.domain.usecase

import com.nagarsetu.auth.data.AuthRepository
import com.nagarsetu.auth.domain.model.AuthResult
import com.nagarsetu.auth.domain.model.UserProfile
import javax.inject.Inject

/** Use-case: send OTP to a phone number via Twilio. */
class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): AuthResult<Boolean> =
        authRepository.sendOtp(phone)
}

/** Use-case: verify OTP and establish a verified user session. */
class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String, code: String): AuthResult<UserProfile> =
        authRepository.verifyOtp(phone, code)
}

/** Use-case: check if user is already logged in (guest or verified). */
class GetSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): UserProfile? = authRepository.getSession()
}

/** Use-case: sign out and clear local session. */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.logout()
}

/**
 * Use-case: guarantees a persisted session always exists.
 * Called once on app start — returns the existing session (guest or verified)
 * or creates and persists a fresh guest profile if this is a first launch.
 */
class EnsureGuestSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): UserProfile = authRepository.ensureGuestSession()
}

/** Use-case: update user profile details in Supabase. */
class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        uid: String,
        name: String? = null,
        email: String? = null,
        ward: String? = null
    ): AuthResult<Unit> = authRepository.updateProfile(uid, name, email, ward)
}
