package com.nagarsetu.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.auth.data.AuthRepository
import com.nagarsetu.auth.domain.model.MedicalInfo
import com.nagarsetu.auth.domain.model.TrustedContact
import com.nagarsetu.core.data.local.ProfileLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fix #14: Extracted from [LoginViewModel], which previously managed OTP state,
 * trusted contacts, medical info, alert settings, language preferences, and profile
 * updates all in one 200-line class.
 *
 * [LoginViewModel] now handles ONLY authentication state (OTP, guest session, logout).
 * All user preferences and safety data live here and can be injected independently
 * by Settings, Profile, and Emergency screens — without pulling in auth state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localProfileRepository: ProfileLocalRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val trustedContacts = localProfileRepository.trustedContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medicalInfo = localProfileRepository.medicalInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicalInfo())

    val alertSettings = localProfileRepository.alertSettings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            mapOf("emergency" to true, "civic" to true, "news" to true)
        )

    val selectedLanguage = localProfileRepository.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en")

    fun saveTrustedContacts(contacts: List<TrustedContact>) {
        viewModelScope.launch { 
            localProfileRepository.saveContacts(contacts)
            // Sync to Supabase if logged in
            val user = authRepository.getSession()
            if (user != null && !user.isGuest) {
                authRepository.saveTrustedContactsRemote(user.uid, contacts)
            }
        }
    }

    fun saveMedicalInfo(info: MedicalInfo) {
        viewModelScope.launch { localProfileRepository.saveMedicalInfo(info) }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            val user = authRepository.getSession()
            if (user != null && !user.isGuest) {
                authRepository.updateProfile(user.uid, name = name, email = email)
            }
        }
    }

    fun updateAlertSettings(key: String, enabled: Boolean) {
        val current = alertSettings.value.toMutableMap()
        current[key] = enabled
        viewModelScope.launch { localProfileRepository.saveAlertSettings(current) }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch { localProfileRepository.saveLanguage(langCode) }
    }
}
