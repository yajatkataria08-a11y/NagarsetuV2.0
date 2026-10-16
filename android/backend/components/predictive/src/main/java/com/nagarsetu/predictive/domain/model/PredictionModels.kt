package com.nagarsetu.predictive.domain.model

// ── Risk Level ──────────────────────────────────────────────────────────────
enum class RiskLevel(val label: String, val emoji: String) {
    LOW("Low", "🟢"),
    MODERATE("Moderate", "🟡"),
    HIGH("High", "🟠"),
    SEVERE("Severe", "🔴")
}

// ── Prediction Types ────────────────────────────────────────────────────────
enum class PredictionType(val label: String, val emoji: String) {
    ACCIDENT("Accident", "💥"),
    FLOOD("Flood", "🌊"),
    CRIME("Crime", "🔒"),
    HEALTH("Health Risk", "🏥")
}

// ── 7-Day Forecast Card ─────────────────────────────────────────────────────
data class Forecast(
    val type: PredictionType,
    val day: String,                // "Mon", "Tue", etc.
    val probability: Float,         // 0.0 – 1.0
    val riskLevel: RiskLevel,
    val hotspot: String = "",       // Area name with highest risk
    val mlConfidence: Float = 0.80f // ML model confidence
)

// ── Hazard Prediction Detail ────────────────────────────────────────────────
data class HazardPrediction(
    val id: String,
    val area: String,
    val areaHi: String = "",
    val latitude: Double,
    val longitude: Double,
    val type: PredictionType,
    val riskScore: Int,             // 0–100
    val riskLevel: RiskLevel,
    val dayLabel: String,
    val dayLabelHi: String = "",
    val factors: List<String>,      // Contributing risk factors
    val factorsHi: List<String> = emptyList(),
    val recommendation: String,
    val recommendationHi: String = ""
)

// ── Risk Grid Cell (for heatmap overlay) ───────────────────────────────────
data class RiskGridCell(
    val id: String,
    val centerLat: Double,
    val centerLng: Double,
    val riskScore: Int,             // 0–100
    val dominantType: PredictionType
)

// ── RAG Result ──────────────────────────────────────────────────────────────
data class RAGResult(
    val query: String,
    val answer: String,
    val sources: List<String>,
    val confidence: Float,
    val relatedIncidents: List<String> = emptyList(),
    val legalSections: List<String> = emptyList()
)

// ── BIMSTEC City ────────────────────────────────────────────────────────────
data class BimstecCityData(
    val city: String,
    val country: String,
    val flag: String,
    val accidentBlackspots: Int,
    val floodRiskAreas: Int,
    val crimeHotspots: Int,
    val healthRiskIndex: Int,       // 0–100
    val dataQuality: String,        // "Live" | "Historical" | "Projected"
    val lastUpdated: String
)

// ── Proactive Alert ─────────────────────────────────────────────────────────
data class ProactiveAlert(
    val id: String,
    val title: String,
    val titleHi: String = "",
    val description: String,
    val descriptionHi: String = "",
    val type: PredictionType,
    val area: String,
    val areaHi: String = "",
    val riskLevel: RiskLevel,
    val recommendation: String,
    val recommendationHi: String = "",
    val timestamp: Long
)
