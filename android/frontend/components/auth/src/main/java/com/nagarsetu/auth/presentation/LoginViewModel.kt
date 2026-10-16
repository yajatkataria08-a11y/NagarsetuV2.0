package com.nagarsetu.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.auth.domain.model.AuthResult
import com.nagarsetu.auth.domain.model.OtpState
import com.nagarsetu.auth.domain.model.UserProfile
import com.nagarsetu.auth.domain.usecase.EnsureGuestSessionUseCase
import com.nagarsetu.auth.domain.usecase.GetSessionUseCase
import com.nagarsetu.auth.domain.usecase.LogoutUseCase
import com.nagarsetu.auth.domain.usecase.SendOtpUseCase
import com.nagarsetu.auth.domain.usecase.UpdateProfileUseCase
import com.nagarsetu.auth.domain.usecase.VerifyOtpUseCase
import com.nagarsetu.firebase.auth.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fix #14: LoginViewModel now owns ONLY authentication state — OTP flow, guest
 * session management, logout, and profile name/email updates.
 *
 * Trusted contacts, medical info, alert settings, and language preferences have
 * been moved to [SettingsViewModel] so that Settings/Profile/Emergency screens
 * no longer need to depend on auth state.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val ensureGuestSessionUseCase: EnsureGuestSessionUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val userRepository: com.nagarsetu.core.data.supabase.SupabaseUserRepository,
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    /** Cached phone so the verify step can re-use it. */
    private var pendingPhone: String = ""

    val currentUserFlow: StateFlow<UserProfile?> = userRepository.profileFlow

    init {
        val profile = ensureGuestSessionUseCase()
        _otpState.value = OtpState.Verified(profile)
    }

    // ────────────────────────────────────────────────────────────
    // Step 1: Send OTP
    // ────────────────────────────────────────────────────────────

    fun sendOtp(phone: String) {
        if (phone.isBlank()) {
            _otpState.value = OtpState.Error("कृपया अपना फ़ोन नंबर दर्ज करें")
            return
        }
        
        // Ensure E.164 format for Twilio
        val digits = phone.replace(Regex("[^\\d]"), "")
        pendingPhone = when {
            phone.startsWith("+") -> phone
            digits.length == 10 -> "+91$digits"
            else -> phone // Let server-side handle or fail if invalid
        }.trim()

        viewModelScope.launch {
            _otpState.value = OtpState.SendingOtp
            when (val result = sendOtpUseCase(pendingPhone)) {
                is AuthResult.Success -> {
                    _otpState.value = OtpState.OtpSent(
                        phone = pendingPhone,
                        maskedPhone = maskPhone(pendingPhone)
                    )
                }
                is AuthResult.Failure -> {
                    _otpState.value = OtpState.Error(result.error)
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Step 2: Verify OTP
    // ────────────────────────────────────────────────────────────

    fun verifyOtp(code: String) {
        if (code.length != 6) {
            _otpState.value = OtpState.Error("कृपया 6 अंकों का OTP दर्ज करें")
            return
        }
        viewModelScope.launch {
            _otpState.value = OtpState.VerifyingOtp
            when (val result = verifyOtpUseCase(pendingPhone, code)) {
                is AuthResult.Success -> {
                    _otpState.value = OtpState.Verified(result.data)
                }
                is AuthResult.Failure -> {
                    _otpState.value = OtpState.Error(result.error)
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Other auth helpers
    // ────────────────────────────────────────────────────────────

    fun resendOtp() {
        if (pendingPhone.isNotBlank()) sendOtp(pendingPhone)
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _otpState.value = OtpState.GuestMode
            kotlinx.coroutines.delay(800)
            val profile = ensureGuestSessionUseCase()
            _otpState.value = OtpState.Verified(profile)
        }
    }

    fun goBackToPhone() {
        _otpState.value = OtpState.Idle
        pendingPhone = ""
    }

    fun startPhoneLink() {
        pendingPhone = ""
        _otpState.value = OtpState.Idle
    }

    fun cancelPhoneLink() {
        val profile = getSessionUseCase()
        _otpState.value = if (profile != null) OtpState.Verified(profile) else OtpState.Idle
        pendingPhone = ""
    }

    fun logout() {
        logoutUseCase()
        pendingPhone = ""
        val guest = ensureGuestSessionUseCase()
        _otpState.value = OtpState.Verified(guest)
    }

    fun clearError() {
        val profile = getSessionUseCase()
        _otpState.value = if (profile != null) OtpState.Verified(profile) else OtpState.Idle
    }

    fun currentUser(): UserProfile? =
        (_otpState.value as? OtpState.Verified)?.profile ?: getSessionUseCase()

    fun updateProfile(name: String, email: String, onDone: () -> Unit) {
        val uid = currentUser()?.uid ?: return
        viewModelScope.launch {
            updateProfileUseCase(uid, name = name, email = email)
            getSessionUseCase()?.let { 
                _otpState.value = OtpState.Verified(it) 
            }
            onDone()
        }
    }

    private fun maskPhone(phone: String): String {
        val digits = phone.replace(Regex("[^\\d]"), "")
        return if (digits.length >= 10) "XXXXXX${digits.takeLast(4)}" else phone
    }
}
