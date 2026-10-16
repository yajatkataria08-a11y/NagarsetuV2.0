package com.nagarsetu.parkease.data

import com.nagarsetu.parkease.domain.model.ParkingBooking
import com.nagarsetu.parkease.domain.model.ParkingLot

interface ParkingRepository {
    suspend fun getLots(lat: Double, lng: Double): List<ParkingLot>
    suspend fun holdSlot(lotId: String): String

    suspend fun bookSlot(
        uid: String,
        lotId: String,
        lotName: String,
        slotNumber: Int,
        durationHours: Int
    ): Result<ParkingBooking>

    suspend fun getUserBookings(uid: String): List<ParkingBooking>

    suspend fun cancelBooking(bookingId: String): Result<Unit>
}
