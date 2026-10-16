package com.nagarsetu.healthwatch.data

import com.nagarsetu.healthwatch.domain.model.AirQualityData
import com.nagarsetu.healthwatch.domain.model.Clinic
import com.nagarsetu.healthwatch.domain.model.EpidemicZone
import com.nagarsetu.healthwatch.domain.model.TelemedicineDoctor

interface HealthDataRepository {
    suspend fun getEpidemicZones(): List<EpidemicZone>
    suspend fun getDoctors(): List<TelemedicineDoctor>
    suspend fun getClinics(): List<Clinic>
    suspend fun getAirQuality(lat: Double, lng: Double): AirQualityData
}
