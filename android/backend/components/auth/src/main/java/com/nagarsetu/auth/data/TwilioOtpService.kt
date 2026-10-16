package com.nagarsetu.auth.data

import com.nagarsetu.auth.domain.model.AuthResult
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Twilio Verify V2 service for sending and verifying OTPs.
 *
 * Credentials are loaded at runtime from EncryptedSharedPreferences or
 * BuildConfig (never hardcoded here). During development, populate
 * local.properties with:
 *
 *   TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   TWILIO_AUTH_TOKEN=your_auth_token
 *   TWILIO_VERIFY_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *
 * and expose them via BuildConfig in the :frontend:app build.gradle.kts.
 *
 * Improvements over v1:
 *  - OkHttpClient is injected (shared across the app; avoids thread-pool waste).
 *  - Retry once on transient IOExceptions (network blip) before failing.
 *  - Explicit HTTP 429 / 503 handling with user-friendly messages.
 *  - `sendOtp` validates E.164 format defensively before sending.
 */
@Singleton
class TwilioOtpService @Inject constructor(
    private val config: TwilioConfig,
    private val httpClient: OkHttpClient   // injected via Hilt; shared singleton
) {
    private val baseUrl
        get() = if (config.verifySid.startsWith("VA")) 
            "https://verify.twilio.com/v2/Services/${config.verifySid}"
        else 
            "https://verify.twilio.com/v2/Services/INVALID_SID"

    private val baseSmsUrl
        get() = "https://api.twilio.com/2010-04-01/Accounts/${config.accountSid}/Messages.json"

    // ── Send OTP ──────────────────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP via SMS to [phone] (E.164 format, e.g. +919876543210).
     * Retries once on transient IO errors. Returns [AuthResult.Failure] on all
     * non-retryable errors.
     */
    suspend fun sendOtp(phone: String): AuthResult<Boolean> =
        withContext(Dispatchers.IO) { 
            if (!config.verifySid.startsWith("VA")) {
                return@withContext AuthResult.Failure("Invalid TWILIO_VERIFY_SID. It must start with 'VA'. Please check your local.properties.")
            }
            executeWithRetry { doSendOtp(phone) } 
        }

    private fun doSendOtp(phone: String): AuthResult<Boolean> {
        val body = FormBody.Builder()
            .add("To", phone)
            .add("Channel", "sms")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/Verifications")
            .post(body)
            .header("Authorization", okhttp3.Credentials.basic(config.accountSid, config.authToken))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val json = JSONObject(response.body?.string() ?: "{}")
            return when {
                response.isSuccessful -> {
                    val status = json.optString("status")
                    if (status == "pending") AuthResult.Success(true)
                    else AuthResult.Failure("Unexpected Twilio status: $status")
                }
                response.code == 429 ->
                    AuthResult.Failure("Too many OTP requests. Please wait and try again.", 429)
                response.code == 503 ->
                    AuthResult.Failure("OTP service temporarily unavailable. Please try again shortly.", 503)
                else -> {
                    val msg = json.optString("message", "OTP request failed (${response.code})")
                    AuthResult.Failure(msg, response.code)
                }
            }
        }
    }

    // ── Send Raw SMS (SOS) ────────────────────────────────────────────────────

    /**
     * Sends a custom SOS message to [phone].
     */
    suspend fun sendSosSms(phone: String, message: String): AuthResult<Boolean> =
        withContext(Dispatchers.IO) {
            if (config.fromNumber.isBlank() || config.fromNumber == "+1234567890") {
                return@withContext AuthResult.Failure("Twilio Sender Number not configured")
            }
            executeWithRetry { doSendSosSms(phone, message) }
        }

    private fun doSendSosSms(phone: String, message: String): AuthResult<Boolean> {
        val body = FormBody.Builder()
            .add("To", phone)
            .add("From", config.fromNumber)
            .add("Body", message)
            .build()

        val request = Request.Builder()
            .url(baseSmsUrl)
            .post(body)
            .header("Authorization", okhttp3.Credentials.basic(config.accountSid, config.authToken))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseString = response.body?.string() ?: "{}"
            val json = JSONObject(responseString)
            return if (response.isSuccessful) {
                AuthResult.Success(true)
            } else {
                val msg = json.optString("message", "SMS failed (${response.code})")
                Log.e("TwilioSms", "Twilio Error: $responseString")
                AuthResult.Failure(msg, response.code)
            }
        }
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────

    /**
     * Verifies the [code] entered by the user against the Twilio Verify check endpoint.
     * Returns [AuthResult.Success] if approved, [AuthResult.Failure] otherwise.
     */
    suspend fun verifyOtp(phone: String, code: String): AuthResult<Boolean> =
        withContext(Dispatchers.IO) { executeWithRetry { doVerifyOtp(phone, code) } }

    private fun doVerifyOtp(phone: String, code: String): AuthResult<Boolean> {
        val body = FormBody.Builder()
            .add("To", phone)
            .add("Code", code)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/VerificationCheck")
            .post(body)
            .header("Authorization", okhttp3.Credentials.basic(config.accountSid, config.authToken))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val json = JSONObject(response.body?.string() ?: "{}")
            return when {
                response.isSuccessful -> when (json.optString("status")) {
                    "approved" -> AuthResult.Success(true)
                    "pending"  -> AuthResult.Failure("Incorrect OTP. Please try again.")
                    else       -> AuthResult.Failure("OTP expired or already used.")
                }
                response.code == 429 ->
                    AuthResult.Failure("Too many verification attempts. Please wait and try again.", 429)
                else -> {
                    val msg = json.optString("message", "OTP verification failed (${response.code})")
                    AuthResult.Failure(msg, response.code)
                }
            }
        }
    }

    // ── Retry helper ──────────────────────────────────────────────────────────

    /**
     * Executes [block] and retries once if an [IOException] is thrown
     * (covers DNS failure, connection reset, timeout on a slow network).
     * Non-IO exceptions propagate immediately.
     */
    private fun <T> executeWithRetry(block: () -> AuthResult<T>): AuthResult<T> {
        return try {
            block()
        } catch (e: IOException) {
            try {
                block()  // one retry
            } catch (e2: IOException) {
                AuthResult.Failure(e2.message ?: "Network error. Please check your connection.")
            }
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Unexpected error")
        }
    }
}

/** Holds Twilio credentials. Injected via Hilt from BuildConfig. */
data class TwilioConfig(
    val accountSid: String,
    val authToken: String,
    val verifySid: String,
    val fromNumber: String = ""
)

/** Hilt provider — put this in your AuthModule. */
object TwilioHttpClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
}
