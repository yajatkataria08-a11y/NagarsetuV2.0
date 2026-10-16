package com.nagarsetu.firebase.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Authentication as the **secondary auth provider** in the
 * NagarSetu hybrid model.
 *
 * PRIMARY:  Supabase Auth (phone OTP via Twilio) → creates `profiles` row.
 * SECONDARY: Firebase Auth (Google Sign-In) → optional, linked by the same uid.
 *
 * Why both?
 * - Supabase phone OTP is the primary citizen login (works without Google)
 * - Firebase Auth unlocks Google Sign-In as a faster alternative for users
 *   who prefer it, and also lets us use Firebase Admin SDK server-side
 *   to verify identity claims before sending targeted push notifications.
 *
 * Linking strategy:
 *   1. User logs in via Supabase OTP → gets uid (e.g. "uuid-xxx")
 *   2. If user opts into Google Sign-In, we sign them into Firebase with
 *      the Google credential AND store the same uid as a custom claim via
 *      our Supabase Edge Function so both systems agree on identity.
 *
 * The [firebaseUid] is intentionally kept separate from the Supabase uid
 * to avoid coupling — the mapping lives in `profiles.firebase_uid`.
 */
@Singleton
class FirebaseAuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics
) {
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _firebaseUser.value = auth.currentUser
            auth.currentUser?.uid?.let { uid ->
                crashlytics.setUserId(uid)
                Log.d(TAG, "Firebase user changed: uid=$uid")
            }
        }
    }

    /**
     * Signs the current Firebase user out.
     * Does NOT sign out of Supabase — call Supabase logout separately.
     */
    fun signOutFirebase() {
        firebaseAuth.signOut()
        Log.d(TAG, "Firebase sign-out complete")
    }

    /** Returns the current Firebase UID, or null if not signed in. */
    val firebaseUid: String? get() = firebaseAuth.currentUser?.uid

    /** True if the user is currently signed in to Firebase. */
    val isSignedIn: Boolean get() = firebaseAuth.currentUser != null

    /**
     * Returns a fresh Firebase ID token (valid 1 hour) for authenticating
     * requests to our Supabase Edge Functions that verify identity via Firebase Admin SDK.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = firebaseAuth.currentUser ?: return null
        return runCatching {
            user.getIdToken(forceRefresh).await().token
        }.onFailure { e ->
            Log.e(TAG, "getIdToken failed: ${e.message}", e)
        }.getOrNull()
    }
}
