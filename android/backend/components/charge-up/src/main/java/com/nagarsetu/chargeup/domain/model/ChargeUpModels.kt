package com.nagarsetu.chargeup.domain.model

data class ChargingStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val connectorTypes: List<ConnectorType>,
    val status: StationStatus,
    val powerKw: Int,
    val costPerKwh: Double,
    val currency: String = "INR"
)

enum class ConnectorType {
    CCS2,
    CHADEMO,
    TYPE2,
    GB_T
}

enum class StationStatus {
    AVAILABLE,
    OCCUPIED,
    UNDER_MAINTENANCE
}
