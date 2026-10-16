package com.nagarsetu.drivelegal.domain.model

// ─── Violation types ─────────────────────────────────────────────────────────
enum class ViolationType(
    val label: String,
    val emoji: String,
    val mvActSection: String
) {
    SPEEDING("Speeding", "💨", "183"),
    PARKING("Illegal Parking", "🅿️", "122"),
    SIGNAL_JUMP("Signal Jump", "🚦", "119"),
    NO_HELMET("No Helmet", "⛑️", "129"),
    DRUNK_DRIVING("Drunk Driving", "🍺", "185"),
    NO_INSURANCE("No Insurance", "📋", "196"),
    NO_LICENSE("No License", "🪪", "3/181"),
    NO_SEATBELT("No Seat Belt", "🔒", "138"),
    MOBILE_DRIVING("Phone While Driving", "📱", "184"),
    OVERLOADING("Overloading", "⚖️", "194"),
    TRIPLE_RIDING("Triple Riding", "🏍️", "128"),
    EXPIRED_RC("Expired RC", "📄", "192")
}

// ─── Vehicle categories with fine multipliers ─────────────────────────────────
enum class VehicleCategory(
    val label: String,
    val emoji: String,
    val fineMultiplier: Double
) {
    BIKE("Bike / Scooter", "🏍️", 1.0),
    CAR("Car / Jeep", "🚗", 1.0),
    TRUCK("Truck / HMV", "🚛", 2.5),
    AUTO("Auto Rickshaw", "🛺", 1.2),
    OTHER("Other", "🚐", 1.5)
}

// ─── Fine resolution tiers ────────────────────────────────────────────────────
enum class OffenceRepeat(val label: String, val multiplier: Double) {
    FIRST("1st offence", 1.0),
    SECOND("2nd offence", 2.0),
    THIRD_PLUS("3rd+ offence", 4.0)
}

// ─── Challan model ────────────────────────────────────────────────────────────
data class Challan(
    val id: String,
    val vehicleNumber: String,
    val violationType: ViolationType,
    val baseAmount: Double,
    val currency: String,
    val repeatCount: Int,
    val timestamp: Long,
    val authority: String = "MP Traffic Police",
    val mvActSection: String = violationType.mvActSection,
    val state: String = "MP",
    val countryCode: String = "IN"
)

// ─── Fine calculation result ─────────────────────────────────────────────────
data class FineCalculation(
    val violation: ViolationType,
    val vehicleCategory: VehicleCategory,
    val baseAmount: Double,
    val vehicleMultiplier: Double,
    val offenceMultiplier: Double,
    val totalAmount: Double,
    val currency: String,
    val currencySymbol: String,
    val mvActSection: String,
    val stateNote: String?,
    val countryCode: String
) {
    val multiplier: Double get() = offenceMultiplier
    val formattedTotal: String get() = "$currencySymbol${totalAmount.toLong()}"
}

// ─── OCR scan result ──────────────────────────────────────────────────────────
data class OcrScanResult(
    val vehicleNumber: String?,
    val fineAmount: String?,
    val date: String?,
    val section: String?,
    val authority: String?,
    val isSuspectFraud: Boolean = false,
    val fraudReason: String? = null,
    val rawText: String = ""
)

// ─── State amendment ─────────────────────────────────────────────────────────
data class StateAmendment(
    val stateCode: String,
    val stateName: String,
    val note: String,
    val fineOverride: Double? = null
)

// ─── Chat message ─────────────────────────────────────────────────────────────
data class DriveLegalMessage(
    val id: String,
    val userText: String,
    val botText: String,
    val detectedViolation: ViolationType? = null,
    val fineCalculation: FineCalculation? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// ─── Multi-violation result ───────────────────────────────────────────────────
data class MultiViolationResult(
    val violations: List<FineCalculation>,
    val grandTotal: Double,
    val currency: String,
    val currencySymbol: String
)
