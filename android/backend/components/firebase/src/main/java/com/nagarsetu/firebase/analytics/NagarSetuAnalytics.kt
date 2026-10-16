package com.nagarsetu.firebase.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed analytics facade for NagarSetu.
 *
 * Wraps Firebase Analytics + Crashlytics so:
 *  1. Feature modules don't need a direct dependency on firebase-analytics.
 *  2. Events are typed (no raw string typos across 13 modules).
 *  3. Non-PII attributes are recorded on crash reports automatically.
 *
 * Hybrid note: Supabase does not need to store analytics; Firebase handles
 * all observability (funnels, crash rates, engagement). Only civic data
 * (incidents, reports) goes to Supabase.
 */
@Singleton
class NagarSetuAnalytics @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) {
    companion object {
        private const val TAG = "NagarSetuAnalytics"
    }

    // ── Session ────────────────────────────────────────────────────────────

    /** Call when a Supabase-authenticated user session starts. */
    fun onUserLogin(method: LoginMethod, wardNumber: Int?) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method.name)
        })
        crashlytics.setCustomKey("ward", wardNumber ?: -1)
        crashlytics.setCustomKey("login_method", method.name)
        Log.d(TAG, "onUserLogin: method=${method.name} ward=$wardNumber")
    }

    fun onGuestSession() {
        crashlytics.setCustomKey("login_method", "GUEST")
        analytics.logEvent("guest_session", null)
    }

    // ── Features ───────────────────────────────────────────────────────────

    fun onFeatureOpened(feature: Feature) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, feature.screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, feature.screenName)
        })
    }

    // ── Raksha / Safety ────────────────────────────────────────────────────

    fun onSosTriggered(lat: Double, lng: Double) {
        analytics.logEvent("sos_triggered", Bundle().apply {
            // Round to 3 decimals — enough for city-level analytics, not precise enough to be PII
            putDouble("lat_approx",  Math.round(lat  * 1000) / 1000.0)
            putDouble("lng_approx",  Math.round(lng  * 1000) / 1000.0)
        })
        crashlytics.log("SOS triggered at $lat,$lng")
    }

    fun onLiveTrackStarted(sessionId: String) {
        analytics.logEvent("live_track_started", null)
        crashlytics.setCustomKey("active_track_session", sessionId)
    }

    fun onLiveTrackEnded() {
        analytics.logEvent("live_track_ended", null)
        crashlytics.setCustomKey("active_track_session", "none")
    }

    // ── Incident reporting ─────────────────────────────────────────────────

    fun onIncidentReported(type: String, wardNumber: Int?) {
        analytics.logEvent("incident_reported", Bundle().apply {
            putString("incident_type", type)
            putInt("ward", wardNumber ?: -1)
        })
    }

    // ── Dashboard / alerts ─────────────────────────────────────────────────

    fun onAlertReceived(alertType: String, source: String) {
        analytics.logEvent("alert_received", Bundle().apply {
            putString("alert_type", alertType)
            putString("alert_source", source)
        })
    }

    // ── Errors ─────────────────────────────────────────────────────────────

    /** Record non-fatal errors so they appear in Crashlytics Issues. */
    fun recordNonFatal(throwable: Throwable, context: String = "") {
        if (context.isNotEmpty()) crashlytics.log("Context: $context")
        crashlytics.recordException(throwable)
    }

    /** Attach current Supabase uid to all crash reports (non-PII safe). */
    fun setUserId(supabaseUid: String) {
        analytics.setUserId(supabaseUid)
        crashlytics.setUserId(supabaseUid)
    }
}

// ── Typed enums ────────────────────────────────────────────────────────────

enum class LoginMethod { PHONE_OTP, GOOGLE, GUEST }

enum class Feature(val screenName: String) {
    DASHBOARD("Dashboard"),
    RAKSHA("Raksha"),
    LIVE_TRACK("LiveTrack"),
    REPORT_IT("ReportIt"),
    ROAD_WATCH("RoadWatch"),
    HEALTH_WATCH("HealthWatch"),
    GREEN_ROUTE("GreenRoute"),
    PARK_EASE("ParkEase"),
    CHARGE_UP("ChargeUp"),
    EMERGENCY_AI("EmergencyAI"),
    PREDICTIVE("PredictiveAnalysis"),
    DRIVE_LEGAL("DriveLegal"),
    ASSISTANT("Assistant")
}
