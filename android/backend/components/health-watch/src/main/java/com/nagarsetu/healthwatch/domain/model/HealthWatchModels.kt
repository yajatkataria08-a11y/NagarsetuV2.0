package com.nagarsetu.healthwatch.domain.model

data class EpidemicZone(
    val id: String,
    val diseaseName: String,
    val latitude: Double,
    val longitude: Double,
    val intensity: Float, // 0.0 to 1.0 for heatmap
    val trend: HealthTrend,
    val activeCases: Int
)

enum class HealthTrend {
    RISING,
    STABLE,
    FALLING
}

data class TelemedicineDoctor(
    val id: String,
    val name: String,
    val specialty: String,
    val isOnline: Boolean,
    val rating: Float
)

data class ConsultationSession(
    val id: String,
    val doctorId: String,
    val startTime: Long,
    val roomName: String, // Jitsi room name
    val status: SessionStatus
)

enum class SessionStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED
}

data class Clinic(
    val id: String,
    val name: String,
    val type: String, // "Primary Health Centre", "Diagnostic Lab", "Hospital"
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val phone: String,
    val isOpen: Boolean,
    val timings: String = "9:00 AM – 6:00 PM",
    val specialties: List<String> = emptyList()
)

data class AirQualityData(
    val aqi: Int,
    val status: String,
    val description: String,
    val recommendation: String,
    val pollutants: List<Pollutant>,
    val timestamp: Long = System.currentTimeMillis()
)

data class Pollutant(
    val name: String,
    val value: Double,
    val unit: String,
    val level: String, // "Good", "Fair", "Moderate", "Poor", "Very Poor"
    val progress: Float, // 0..1 for UI
    val colorHex: String,
    val healthImpact: String
)

