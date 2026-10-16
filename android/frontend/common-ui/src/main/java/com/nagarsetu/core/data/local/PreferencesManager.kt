package com.nagarsetu.core.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.nagarsetu.auth.domain.model.UserProfile
import com.nagarsetu.core.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("nagarsetu_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    // ────────────────────────────────────────────────────────────
    // Theme (DataStore) - Non-blocking
    // ────────────────────────────────────────────────────────────

    private val themeKey = stringPreferencesKey("app_theme")

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.fromId(prefs[themeKey])
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[themeKey] = theme.id }
    }

    // ────────────────────────────────────────────────────────────
    // User Session (EncryptedSharedPreferences — AES-256)
    // Wrapped in try-catch to prevent startup crashes.
    // ────────────────────────────────────────────────────────────

    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "nagarsetu_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Failed to init EncryptedSharedPreferences", e)
            // Fallback to regular prefs if encryption fails (better than crashing)
            context.getSharedPreferences("nagarsetu_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private val SESSION_KEY = "user_session_v1"

    /** Persists the [UserProfile] to secure storage. */
    fun saveUserSession(profile: UserProfile) {
        encryptedPrefs?.edit()?.putString(SESSION_KEY, gson.toJson(profile))?.apply()
    }

    /** Returns the cached [UserProfile] or null if not logged in. */
    fun getUserSession(): UserProfile? {
        val json = encryptedPrefs?.getString(SESSION_KEY, null) ?: return null
        return try { gson.fromJson(json, UserProfile::class.java) } catch (e: Exception) { null }
    }

    /** Clears all session data (logout). */
    fun clearUserSession() {
        encryptedPrefs?.edit()?.remove(SESSION_KEY)?.apply()
    }
}
