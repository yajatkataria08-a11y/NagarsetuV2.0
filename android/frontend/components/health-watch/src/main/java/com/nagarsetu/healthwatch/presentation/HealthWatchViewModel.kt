package com.nagarsetu.healthwatch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.healthwatch.data.HealthDataRepository
import com.nagarsetu.backend.core.CivicConstants
import com.nagarsetu.healthwatch.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HealthWatchViewModel @Inject constructor(
    private val repository: HealthDataRepository
) : ViewModel() {

    private val _epidemicZones = MutableStateFlow<List<EpidemicZone>>(emptyList())
    val epidemicZones = _epidemicZones.asStateFlow()

    private val _mapMarkers = MutableStateFlow<List<MapMarker>>(emptyList())
    val mapMarkers = _mapMarkers.asStateFlow()

    private val _doctors = MutableStateFlow<List<TelemedicineDoctor>>(emptyList())
    val doctors = _doctors.asStateFlow()

    private val _activeConsultation = MutableStateFlow<ConsultationSession?>(null)
    val activeConsultation = _activeConsultation.asStateFlow()

    private val _clinics = MutableStateFlow<List<Clinic>>(emptyList())
    val clinics = _clinics.asStateFlow()

    private val _airQuality = MutableStateFlow<AirQualityData?>(null)
    val airQuality = _airQuality.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _epidemicZones.value = repository.getEpidemicZones()
                _mapMarkers.value = _epidemicZones.value.map {
                    MapMarker(it.latitude, it.longitude, it.diseaseName, "Cases: ${it.activeCases}")
                }
                _doctors.value = repository.getDoctors()
                _clinics.value = repository.getClinics()
                
                // Bhopal coords
                _airQuality.value = repository.getAirQuality(CivicConstants.BHOPAL_LAT, CivicConstants.BHOPAL_LNG)
            } catch (e: Exception) {
                // handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startConsultation(doctor: TelemedicineDoctor) {
        val id = UUID.randomUUID().toString().take(8)
        _activeConsultation.value = ConsultationSession(
            id = id,
            doctorId = doctor.id,
            startTime = System.currentTimeMillis(),
            roomName = "NagarSetu-Health-$id",
            status = SessionStatus.ACTIVE
        )
    }

    fun endConsultation() {
        _activeConsultation.value = null
    }
}
