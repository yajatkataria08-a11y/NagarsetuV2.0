package com.nagarsetu.parkease.domain.model

data class ParkingLot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val totalSlots: Int,
    val occupiedSlots: Int,
    val ratePerHour: Double,
    val currency: String = "INR"
) {
    val availableSlots: Int get() = (totalSlots - occupiedSlots).coerceAtLeast(0)
    val occupancyRate: Float get() = occupiedSlots.toFloat() / totalSlots.coerceAtLeast(1)

    val status: String get() = when {
        occupancyRate >= 0.95f -> "Full"
        occupancyRate >= 0.80f -> "Nearly Full"
        occupancyRate >= 0.60f -> "Limited"
        else -> "Available"
    }
}

data class ParkingBooking(
    val id: String,
    val lotId: String,
    val slotNumber: Int,
    val startTime: Long,
    val holdExpiryTime: Long, // 30 mins from booking or duration
    val status: BookingStatus,
    val qrData: String
)

enum class BookingStatus {
    ON_HOLD,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
