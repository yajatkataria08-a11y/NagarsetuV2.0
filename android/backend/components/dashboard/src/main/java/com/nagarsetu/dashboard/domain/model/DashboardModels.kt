package com.nagarsetu.dashboard.domain.model

// ─── Map Modes ────────────────────────────────────────────────────────────────
enum class MapMode(val label: String, val emoji: String) {
    EMERGENCY("Emergency", "🚨"),
    CIVIC("Civic", "🏙️"),
    ROAD_WATCH("RoadWatch", "🛣️"),
    AUTHORITY("Authority", "🏛️"),
    PARKING("Parking", "🅿️"),
    HOSPITALS("Hospitals", "🏥"),
    EV_CHARGING("EV Charging", "⚡"),
    TRAFFIC("Traffic", "🚦")
}

// ─── Crisis Levels ────────────────────────────────────────────────────────────
enum class CrisisLevel(val label: String) {
    NORMAL("Normal"),
    ELEVATED("Elevated"),
    CRITICAL("Critical"),
    EMERGENCY("Emergency")
}

// ─── Alert Types ──────────────────────────────────────────────────────────────
enum class AlertType { CIVIC, TRAFFIC, HEALTH, GEOFENCE_ENTRY, SLA_BREACH, NEW_CRISIS, HAZARD }

// ─── Live Alert ───────────────────────────────────────────────────────────────
data class DashboardAlert(
    val id: String,
    val title: String,
    val message: String,
    val type: AlertType,
    val ward: String,
    val timestamp: Long,
    val severity: AlertSeverity = AlertSeverity.INFO
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }

// ─── Ward Authority KPI ───────────────────────────────────────────────────────
data class WardAuthority(
    val id: String,
    val wardName: String,
    val authorityName: String,
    val helpline: String,
    val zone: String,
    val latitude: Double,
    val longitude: Double,
    val complaintCount: Int,
    val resolvedCount: Int,
    val budgetSanctioned: Long,
    val budgetSpent: Long,
    val slaBreaches: Int
) {
    val resolutionRate: Float get() = if (complaintCount > 0) resolvedCount.toFloat() / complaintCount else 0f
    val budgetUtilization: Float get() = if (budgetSanctioned > 0) budgetSpent.toFloat() / budgetSanctioned else 0f
    val pendingComplaints: Int get() = complaintCount - resolvedCount
}

// ─── Citizen Complaint ────────────────────────────────────────────────────────
data class CitizenComplaint(
    val id: String,
    val category: String,
    val description: String,
    val status: ComplaintStatus,
    val wardName: String,
    val submittedAt: Long,
    val resolvedAt: Long? = null
)

enum class ComplaintStatus { PENDING, IN_PROGRESS, RESOLVED, REJECTED }

// ─── Budget Card ──────────────────────────────────────────────────────────────
data class BudgetTransparencyItem(
    val category: String,
    val allocated: Long,
    val spent: Long,
    val projects: Int
)

// ─── City Stats ───────────────────────────────────────────────────────────────
data class CityStats(val total: Int, val pending: Int, val resolved: Int)

// ─── Radar Proximity Alert ────────────────────────────────────────────────────
data class RadarAlert(
    val id: String,
    val label: String,
    val distanceMeters: Int,
    val bearing: Float,     // 0..360
    val type: AlertType
)

// ─── Weather & AQI ────────────────────────────────────────────────────────────
data class WeatherInfo(
    val temp: Double,
    val condition: String,
    val humidity: Int,
    val city: String
)

data class AqiInfo(
    val aqi: Int,
    val status: String
)

// ─── News ─────────────────────────────────────────────────────────────────────
data class NewsItem(
    val title: String,
    val description: String,
    val url: String,
    val image: String?,
    val source: String,
    val publishedAt: String
)
