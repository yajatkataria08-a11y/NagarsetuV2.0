package com.nagarsetu.reportit.domain.model

enum class CivicIssueType {
    POTHOLE,
    DRAIN,
    GARBAGE,
    STREETLIGHT,
    ENCROACHMENT,
    OTHER
}

data class CivicReport(
    val ticketId: String,
    val type: CivicIssueType,
    val description: String,
    val ward: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
