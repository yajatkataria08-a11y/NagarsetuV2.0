package com.nagarsetu.greenroute.domain.model

data class RouteOption(
    val id: String,
    val mode: TransitMode,
    val durationMin: Int,
    val carbonSavedKg: Float,
    val crowdedness: Crowdedness,
    val cost: Double,
    val currency: String = "INR",
    val destLat: Double,
    val destLng: Double
)

enum class TransitMode {
    WALK,
    CYCLE,
    AUTO,
    BUS
}

enum class Crowdedness {
    LOW,
    MODERATE,
    HIGH,
    FULL
}

data class TransitStation(
    val id: String,
    val name: String,
    val mode: TransitMode,
    val latitude: Double,
    val longitude: Double,
    val nextArrivalMin: Int,
    val routes: List<String> = emptyList()
)
