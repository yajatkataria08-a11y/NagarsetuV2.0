/**
 * Secrets.kt — Secure API Key Management
 *
 * ALL keys are injected at build time from local.properties via BuildConfig.
 * NEVER commit real keys to source control.
 *
 * Setup:
 *   1. Copy local.properties.template → local.properties  (gitignored)
 *   2. Fill in values. Backend-only keys go in .env (also gitignored).
 *   3. The app/build.gradle.kts reads local.properties and injects as BuildConfig fields.
 *
 * local.properties.template entries:
 *   TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   TWILIO_PHONE_NUMBER=+1XXXXXXXXXX
 *   SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxx
 *   GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxx
 *   GEMINI_API_KEY=AIzaxxxxxxxxxxxxxxxxxxxxxxxx
 *
 * The .env file (for backend Python scripts ONLY):
 *   RISK_MODEL_PATH=backend/python/risk_model.joblib
 *   FLASK_HOST=0.0.0.0
 *   FLASK_PORT=8080
 */
package com.nagarsetu.utils

import com.nagarsetu.BuildConfig

object Secrets {
    // ── Twilio (SOS SMS) ───────────────────────────────────────────────────────
    val twilioAccountSid:  String = BuildConfig.TWILIO_ACCOUNT_SID
    val twilioAuthToken:   String = BuildConfig.TWILIO_AUTH_TOKEN
    val twilioPhoneNumber: String = BuildConfig.TWILIO_PHONE_NUMBER

    // ── SendGrid (report e-mail) ───────────────────────────────────────────────
    val sendGridApiKey:    String = BuildConfig.SENDGRID_API_KEY

    // ── LLM backends (used by AssistantModule / RAG) ──────────────────────────
    val groqApiKey:        String = BuildConfig.GROQ_API_KEY
    val geminiApiKey:      String = BuildConfig.GEMINI_API_KEY

    // ── External APIs ────────────────────────────────────────────────────────
    val waqiToken:         String = BuildConfig.WAQI_TOKEN

    // ── Guard helpers ─────────────────────────────────────────────────────────
    val hasTwilio:   Boolean get() = twilioAccountSid.isNotBlank() && !twilioAccountSid.startsWith("YOUR_")
    val hasSendGrid: Boolean get() = sendGridApiKey.isNotBlank()   && !sendGridApiKey.startsWith("YOUR_")
    val hasGroq:     Boolean get() = groqApiKey.isNotBlank()       && !groqApiKey.startsWith("YOUR_")
    val hasGemini:   Boolean get() = geminiApiKey.isNotBlank()     && !geminiApiKey.startsWith("YOUR_")
}
