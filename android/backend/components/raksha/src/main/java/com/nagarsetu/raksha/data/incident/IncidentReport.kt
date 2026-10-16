package com.nagarsetu.raksha.data.incident

/**
 * Types of community-reported safety incidents.
 * Ported from Raksha (com.safepath.indore.data.IncidentType).
 */
enum class IncidentType(val label: String) {
    HARASSMENT("Harassment"),
    UNSAFE_SPOT("Unsafe Spot"),
    POOR_LIGHTING("Poor Lighting"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity"),
    OTHER("Other")
}

/**
 * A crowd-sourced safety incident report.
 *
 * Ported from Raksha (com.safepath.indore.data.IncidentReport).
 *
 * Backend contract:
 *  - POST /api/incidents  → submit a new report
 *  - GET  /api/incidents?status=verified → fetch active reports
 *  - Reports should expire after 24–48 h (server-side).
 *  - Rate-limit by device ID to prevent spam.
 */
data class IncidentReport(
    val id: String = "",
    val type: IncidentType,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    /** 1 (low) – 5 (critical). Each severity point contributes 4× to the risk score. */
    val severity: Int = 3,
    /** "pending" | "verified" | "rejected" */
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis(),
    val reportedBy: String = "anonymous"
)
