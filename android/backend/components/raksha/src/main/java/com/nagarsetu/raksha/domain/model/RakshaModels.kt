package com.nagarsetu.raksha.domain.model

data class DisasterAlert(
    val id: String,
    val type: DisasterType,
    val severity: DisasterSeverity,
    val message: String,
    val timestamp: Long
)

enum class DisasterType {
    FLOOD,
    EARTHQUAKE,
    CYCLONE,
    FIRE,
    EPIDEMIC
}

enum class DisasterSeverity {
    ADVISORY,
    WATCH,
    WARNING,
    EXTREME
}

data class SafetyContact(
    val name: String,
    val phoneNumber: String,
    val isEmergencyContact: Boolean = false
)

data class FakeCallProfile(
    val callerName: String,
    val callerNumber: String,
    val delaySeconds: Int
)


// ── Police Station ───────────────────────────────────────────────────────────

data class PoliceStation(
    val name: String,
    val lat: Double,
    val lng: Double
)

object PoliceStationProvider {
    val stations = listOf(
        PoliceStation("MP Nagar Police Station",    23.2380, 77.4620),
        PoliceStation("Arera Colony Police Station", 23.2280, 77.4350),
        PoliceStation("Shahpura Police Station",    23.2000, 77.4220),
        PoliceStation("Govindpura Police Station",  23.2220, 77.4080),
        PoliceStation("Jahangirabad Police Station", 23.2500, 77.4050),
        PoliceStation("Bairagarh Police Station",    23.2520, 77.3700),
        PoliceStation("Habibganj Police Station",    23.2200, 77.4400),
        PoliceStation("Misrod Police Station",       23.1900, 77.4500),
        PoliceStation("Kolar Police Station",        23.1800, 77.4100),
        PoliceStation("TT Nagar Police Station",     23.2350, 77.3980)
    )
}

// ── Emergency Guide ──────────────────────────────────────────────────────────

data class EmergencyGuideItem(
    val emoji: String,
    val question: String,
    val answer: String
)
