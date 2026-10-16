package com.nagarsetu.emergencyai.domain.model

data class EmergencyEvent(
    val id: String,
    val timestamp: Long,
    val triggerType: TriggerType,
    val latitude: Double,
    val longitude: Double,
    val ward: String? = null,
    val triageInfo: TriageInfo? = null,
    val status: EmergencyStatus = EmergencyStatus.ACTIVE
)

enum class TriggerType {
    SHAKE,
    TAP_SOS,
    VOICE_COMMAND,
    CRASH_DETECTION
}

enum class EmergencyStatus {
    ACTIVE,
    DISPATCHED,
    RESOLVED,
    CANCELLED
}

/**
 * Result of triage NLP analysis.
 *
 * Improvement over v1: added [confidence] field (0.0–1.0) produced by
 * [TriageEngine] so the UI can display certainty to the operator.
 */
data class TriageInfo(
    val priority: Priority,
    val medicalNeed: String,
    val etaSeconds: Int,
    /** Confidence in [0.0, 1.0]. Higher = more keywords matched. */
    val confidence: Float = 0.5f
)

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
