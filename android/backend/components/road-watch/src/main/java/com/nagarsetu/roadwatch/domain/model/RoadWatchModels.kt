package com.nagarsetu.roadwatch.domain.model

// ─── Core report model ────────────────────────────────────────────────────────
data class RoadReport(
    val id: String,
    val type: ReportType,
    val latitude: Double,
    val longitude: Double,
    val severity: Severity,
    val depthMm: Float?,
    val timestamp: Long,
    val status: ReportStatus,
    val slaDeadline: Long,
    val imageUri: String? = null,
    val description: String? = null,
    val wardId: String? = null,
    val contractorId: String? = null,
    val authorityLevel: AuthorityLevel = AuthorityLevel.WARD,
    val escalationCount: Int = 0,
    val verifiedByAi: Boolean = false,
    val upvotes: Int = 0
)

// ─── Report types (extended) ──────────────────────────────────────────────────
enum class ReportType(val label: String, val emoji: String) {
    POTHOLE("Pothole", "🕳️"),
    STREET_LIGHT_DOWN("Street Light Down", "💡"),
    WATER_LOGGING("Water Logging", "💧"),
    OPEN_MANHOLE("Open Manhole", "⚠️"),
    ROAD_DAMAGE("Road Damage", "🛤️"),
    ENCROACHMENT("Encroachment", "🚧"),
    BROKEN_SIGNAL("Broken Signal", "🚦"),
    GARBAGE_DUMP("Illegal Garbage", "🗑️"),
    OTHER("Other", "📋")
}

// ─── Severity ─────────────────────────────────────────────────────────────────
enum class Severity(val label: String, val slaDays: Int, val color: Long) {
    LOW("Low", 7, 0xFF2ECC71),
    MEDIUM("Medium", 3, 0xFFF57F17),
    HIGH("High", 1, 0xFFFF9800),
    CRITICAL("Critical", 0, 0xFFE53935)   // 0 = same day
}

// ─── Status ───────────────────────────────────────────────────────────────────
enum class ReportStatus(val label: String) {
    SUBMITTED("Submitted"),
    VERIFIED("Verified"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    ESCALATED("Escalated"),
    REJECTED("Rejected")
}

// ─── Authority hierarchy ──────────────────────────────────────────────────────
enum class AuthorityLevel(val label: String, val escalatesTo: String?) {
    WARD("Ward Office", "ZONAL"),
    ZONAL("Zonal Office", "COMMISSIONER"),
    COMMISSIONER("Municipal Commissioner", "STATE"),
    STATE("State PWD", null)
}

// ─── Contractor info ──────────────────────────────────────────────────────────
data class Contractor(
    val id: String,
    val name: String,
    val phone: String,
    val specialization: String,   // e.g., "Road Repair", "Electrical"
    val wardCoverage: List<String>,
    val averageResponseHours: Int,
    val rating: Float = 0f
)

// ─── SLA result ───────────────────────────────────────────────────────────────
data class SlaInfo(
    val deadlineMs: Long,
    val remainingMs: Long,
    val isBreached: Boolean,
    val urgencyLabel: String,
    val slaDays: Int
) {
    val remainingHours: Long get() = remainingMs / 3_600_000
    val remainingDays: Long get() = remainingMs / 86_400_000
}

// ─── Pothole detection result ─────────────────────────────────────────────────
data class PotholeDetection(
    val confidence: Float,
    val depthEstimateMm: Float,
    val criticality: Severity,
    val suggestedType: ReportType = ReportType.POTHOLE
)

// ─── Heatmap cluster ──────────────────────────────────────────────────────────
data class PotholeCluster(
    val centerLat: Double,
    val centerLng: Double,
    val count: Int,
    val dominantSeverity: Severity,
    val reports: List<RoadReport>
)

// ─── Authority mapping response ───────────────────────────────────────────────
data class AuthorityMapping(
    val wardName: String,
    val wardOfficerName: String,
    val wardPhone: String,
    val zonalOfficeName: String,
    val zonalPhone: String,
    val commissionerPhone: String = "0755-2441100",
    val contractor: Contractor? = null
)
