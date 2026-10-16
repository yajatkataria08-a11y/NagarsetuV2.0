package com.nagarsetu.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nagarsetu.auth.domain.model.MedicalInfo
import com.nagarsetu.auth.domain.model.TrustedContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore("profile_local_prefs")

@Singleton
class ProfileLocalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_CONTACTS = stringPreferencesKey("trusted_contacts")
    private val KEY_MEDICAL = stringPreferencesKey("medical_info")
    private val KEY_ALERTS = stringPreferencesKey("alert_settings")
    private val KEY_LANG = stringPreferencesKey("app_language")

    val trustedContacts: Flow<List<TrustedContact>> = context.profileDataStore.data.map { prefs ->
        val data = prefs[KEY_CONTACTS] ?: return@map defaultContacts()
        try {
            json.decodeFromString<List<TrustedContact>>(data)
        } catch (e: Exception) {
            defaultContacts()
        }
    }

    private fun defaultContacts() = listOf(
        TrustedContact("Mom", "+919876543210", "Family"),
        TrustedContact("Dad", "+919876543211", "Family"),
        TrustedContact("Emergency Support", "100", "Police")
    )

    val medicalInfo: Flow<MedicalInfo> = context.profileDataStore.data.map { prefs ->
        val data = prefs[KEY_MEDICAL] ?: return@map MedicalInfo()
        try { json.decodeFromString<MedicalInfo>(data) } catch (e: Exception) { MedicalInfo() }
    }

    val alertSettings: Flow<Map<String, Boolean>> = context.profileDataStore.data.map { prefs ->
        val data = prefs[KEY_ALERTS] ?: return@map mapOf("emergency" to true, "civic" to true, "news" to true)
        try { json.decodeFromString<Map<String, Boolean>>(data) } catch (e: Exception) { emptyMap() }
    }

    val selectedLanguage: Flow<String> = context.profileDataStore.data.map { prefs ->
        prefs[KEY_LANG] ?: "en"
    }

    suspend fun saveContacts(contacts: List<TrustedContact>) {
        context.profileDataStore.edit { it[KEY_CONTACTS] = json.encodeToString(contacts) }
    }

    suspend fun saveMedicalInfo(info: MedicalInfo) {
        context.profileDataStore.edit { it[KEY_MEDICAL] = json.encodeToString(info) }
    }

    suspend fun saveAlertSettings(settings: Map<String, Boolean>) {
        context.profileDataStore.edit { it[KEY_ALERTS] = json.encodeToString(settings) }
    }

    suspend fun saveLanguage(langCode: String) {
        context.profileDataStore.edit { it[KEY_LANG] = langCode }
    }
}
