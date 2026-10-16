package com.nagarsetu.firebase.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.nagarsetu.firebase.analytics.NagarSetuAnalytics
import com.nagarsetu.firebase.auth.FirebaseAuthManager
import com.nagarsetu.firebase.fcm.FcmTokenManager
import com.nagarsetu.firebase.realtime.GpsTrackingRepository
import com.nagarsetu.firebase.realtime.LiveAlertFirebaseSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides all Firebase singletons.
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │         HYBRID  ARCHITECTURE  — NagarSetu                            │
 * │                                                                       │
 * │  FIREBASE  (real-time, push, observability)                          │
 * │  ─────────────────────────────────────────                           │
 * │  • FCM push       — city alerts, SOS confirmation, ward updates      │
 * │  • Realtime DB    — live GPS broadcast for Raksha LiveTrack          │
 * │  • Firestore      — ephemeral live alert feed (< 24 h TTL)           │
 * │  • Firebase Auth  — optional Google Sign-In (linked to Supabase uid) │
 * │  • Analytics      — feature usage, crash-free rate via Crashlytics   │
 * │                                                                       │
 * │  SUPABASE  (persistent data, authoritative auth)                     │
 * │  ────────────────────────────────────────────────                    │
 * │  • Phone OTP auth — primary login (Twilio via Supabase Auth)         │
 * │  • profiles table — user identity, FCM token storage                 │
 * │  • incidents / wards / reports — PostGIS-enabled civic tables        │
 * │  • Storage        — report images, evidence uploads                  │
 * │  • Edge Functions — AI triage, hazard scoring                        │
 * │                                                                       │
 * │  DATA FLOW:                                                           │
 * │  GPS update → Firebase Realtime DB  ──► Raksha LiveTrack UI          │
 * │  Incident report → Supabase table   ──► RiskCalculator seed          │
 * │  City alert → Firestore (live feed) ──► Dashboard AlertFeed          │
 * │                 + FCM (push)        ──► NagarSetuFirebaseMessaging    │
 * │  User login → Supabase Auth (OTP)   ──► profile.fcm_token updated    │
 * │                                         by FcmTokenManager           │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // ── Core Firebase SDK instances ────────────────────────────────────────

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance().also { db ->
            // Disk persistence keeps GPS tracks readable if connectivity drops
            db.setPersistenceEnabled(true)
            db.setPersistenceCacheSizeBytes(10_000_000L)
        }

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().also { db ->
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    // ── NagarSetu Firebase wrapper singletons ─────────────────────────────

    @Provides @Singleton
    fun provideFcmTokenManager(
        messaging: FirebaseMessaging
    ): FcmTokenManager = FcmTokenManager(messaging)

    @Provides @Singleton
    fun provideFirebaseAuthManager(
        firebaseAuth: FirebaseAuth,
        crashlytics: FirebaseCrashlytics
    ): FirebaseAuthManager = FirebaseAuthManager(firebaseAuth, crashlytics)

    @Provides @Singleton
    fun provideGpsTrackingRepository(
        database: FirebaseDatabase
    ): GpsTrackingRepository = GpsTrackingRepository(database)

    @Provides @Singleton
    fun provideLiveAlertFirebaseSource(
        firestore: FirebaseFirestore
    ): LiveAlertFirebaseSource = LiveAlertFirebaseSource(firestore)

    @Provides @Singleton
    fun provideNagarSetuAnalytics(
        analytics: FirebaseAnalytics,
        crashlytics: FirebaseCrashlytics
    ): NagarSetuAnalytics = NagarSetuAnalytics(analytics, crashlytics)
}
