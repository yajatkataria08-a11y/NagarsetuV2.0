package com.nagarsetu.core.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Unified Retrofit interface for the NagarSetu API Gateway.
 * Base URL targets Express gateway: http://10.0.2.2:4000/
 */
interface NagarSetuApiService {

    // ── Emergency SOS ──
    @POST("api/sos")
    suspend fun triggerSos(@Body request: SosRequestDto): Response<SosResponseDto>

    @PATCH("api/sos/{id}/location")
    suspend fun updateSosLocation(
        @Path("id") id: String,
        @Body request: SosLocationUpdateDto
    ): Response<Map<String, Any>>

    // ── Hazards & Safety Corridors ──
    @GET("api/hazards")
    suspend fun getActiveHazards(): Response<List<HazardZoneDto>>

    @POST("api/hazards")
    suspend fun createHazard(@Body request: CreateHazardRequestDto): Response<HazardZoneDto>

    // ── Civic Incidents ──
    @GET("api/incidents")
    suspend fun getIncidents(@Query("status") status: String? = null): Response<List<IncidentDto>>

    @POST("api/incidents")
    suspend fun submitIncident(@Body request: IncidentSubmissionDto): Response<IncidentDto>

    // ── Routing & Corridors ──
    @POST("api/routing/calculate-routes")
    suspend fun calculateRoutes(@Body request: RouteRequestDto): Response<RouteResponseDto>

    // ── Auth & OTP ──
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequestDto): Response<OtpStatusDto>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequestDto): Response<VerifyOtpResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    // ── Telemetry & Ingestion ──
    @POST("api/ingest/sensor")
    suspend fun submitSensorTelemetry(@Body request: SensorTelemetryDto): Response<SensorTelemetryResponseDto>
}

// ── DTOs ──

data class SosRequestDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("ward") val ward: String = "Bhopal",
    @SerializedName("triageLevel") val triageLevel: String = "HIGH",
    @SerializedName("batteryLevel") val batteryLevel: Int = -1,
    @SerializedName("heartRate") val heartRate: Int = 0,
    @SerializedName("networkType") val networkType: String = "4G",
    @SerializedName("address") val address: String = "",
    @SerializedName("fcmToken") val fcmToken: String = "",
    @SerializedName("trustedContacts") val trustedContacts: List<String> = emptyList()
)

data class SosResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

data class SosLocationUpdateDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("batteryLevel") val batteryLevel: Int,
    @SerializedName("heartRate") val heartRate: Int = 0
)

data class HazardZoneDto(
    @SerializedName("id") val id: Any, // can be Int or String
    @SerializedName("name") val name: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("radius_m") val radiusM: Int = 200,
    @SerializedName("risk") val risk: Double = 1.0,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("type") val type: String? = null
)

data class CreateHazardRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("radius_m") val radiusM: Int,
    @SerializedName("risk") val risk: Double,
    @SerializedName("severity") val severity: String = "HIGH",
    @SerializedName("description") val description: String = ""
)

data class IncidentDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("severity") val severity: Int = 3,
    @SerializedName("status") val status: String = "submitted",
    @SerializedName("description") val description: String? = null,
    @SerializedName("reportedBy") val reportedBy: String? = null,
    @SerializedName("ward") val ward: String? = null,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class IncidentSubmissionDto(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("severity") val severity: Int = 3,
    @SerializedName("reportedBy") val reportedBy: String,
    @SerializedName("reporterName") val reporterName: String = "NagarSetu User",
    @SerializedName("ward") val ward: String = "Bhopal",
    @SerializedName("photos") val photos: List<String> = emptyList()
)

data class RoutePointDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class RouteRequestDto(
    @SerializedName("origin") val origin: RoutePointDto,
    @SerializedName("destination") val destination: RoutePointDto,
    @SerializedName("mode") val mode: String = "walking",
    @SerializedName("preference") val preference: String = "safest"
)

data class RouteResponseDto(
    @SerializedName("routes") val routes: List<RouteOptionDto> = emptyList()
)

data class RouteOptionDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("safety_score") val safetyScore: Double,
    @SerializedName("coordinates") val coordinates: List<List<Double>> = emptyList()
)

data class SendOtpRequestDto(
    @SerializedName("phone") val phone: String
)

data class OtpStatusDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

data class VerifyOtpRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("otp") val otp: String
)

data class VerifyOtpResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: Map<String, Any>? = null
)

data class LoginRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class LoginResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("name") val name: String? = null
)

data class SensorTelemetryDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("ward") val ward: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class SensorTelemetryResponseDto(
    @SerializedName("received") val received: Boolean,
    @SerializedName("escalated") val escalated: Boolean = false,
    @SerializedName("hazard_created") val hazardCreated: Map<String, Any>? = null
)
