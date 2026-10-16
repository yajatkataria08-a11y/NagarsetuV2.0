package com.nagarsetu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "road_reports")
data class RoadReportEntity(
    @PrimaryKey val id: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val severity: String,
    val status: String,
    val ticketId: String,
    val description: String = ""
)

@Entity(tableName = "parking_bookings")
data class ParkingBookingEntity(
    @PrimaryKey val id: String,
    val lotId: String,
    val slotNumber: Int,
    val qrData: String,
    val expiresAt: Long,
    val status: String
)

@Entity(tableName = "civic_issues")
data class CivicIssueEntity(
    @PrimaryKey val id: String,
    val type: String,
    val description: String,
    val photoPath: String?,
    val lat: Double,
    val lng: Double,
    val status: String,
    val ticketId: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val timestamp: Long
)

@Entity(tableName = "challans")
data class ChallanEntity(
    @PrimaryKey val id: String,
    val violationType: String,
    val vehicleType: String,
    val baseFine: Int,
    val multiplier: Float,
    val totalFine: Int
)
