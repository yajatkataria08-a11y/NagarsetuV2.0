package com.nagarsetu.core.domain

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)
