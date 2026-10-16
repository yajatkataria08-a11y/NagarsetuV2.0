package com.nagarsetu.roadwatch.presentation.report

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.roadwatch.data.RoadWatchRepository
import com.nagarsetu.roadwatch.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ReportStep {
    data object CaptureImage : ReportStep()
    data class VerifyAi(val imageUri: String) : ReportStep()
    data class LocationDetails(val detection: PotholeDetection) : ReportStep()
    data class Success(val ticketId: String) : ReportStep()
}

@HiltViewModel
class RoadReportViewModel @Inject constructor(
    private val repository: RoadWatchRepository,
    private val userRepository: SupabaseUserRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _currentStep = MutableStateFlow<ReportStep>(ReportStep.CaptureImage)
    val currentStep = _currentStep.asStateFlow()

    // Seeded from GPS on init; overwritten by setLocation() when user pins the map
    private var _reportLat: Double = locationProvider.defaultLat
    private var _reportLng: Double = locationProvider.defaultLng

    val heatmapMarkers = emptyList<com.nagarsetu.core.ui.map.MapMarker>()

    init {
        // Get a real GPS fix immediately so the first report isn't on city centre
        viewModelScope.launch {
            val loc = locationProvider.getLastLocation()
            _reportLat = loc.latitude
            _reportLng = loc.longitude
        }
    }

    fun onImageCaptured(imageUri: String) {
        _currentStep.value = ReportStep.VerifyAi(imageUri)
    }

    fun runAiDetection() {
        viewModelScope.launch {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.GRAY)
            }
            val detection = repository.detectPothole(bitmap)
            _currentStep.value = ReportStep.LocationDetails(detection)
        }
    }

    /** Called from the LocationDetails screen when the user confirms their GPS pin. */
    fun setLocation(lat: Double, lng: Double) {
        _reportLat = lat
        _reportLng = lng
    }

    fun submitReport(detection: PotholeDetection) {
        val uid = userRepository.profileFlow.value?.uid ?: "guest_user"
        val id  = UUID.randomUUID().toString()

        viewModelScope.launch {
            val report = RoadReport(
                id        = id,
                type      = ReportType.POTHOLE,
                latitude  = _reportLat,
                longitude = _reportLng,
                severity  = detection.criticality,
                depthMm   = detection.depthEstimateMm,
                timestamp = System.currentTimeMillis(),
                status    = ReportStatus.SUBMITTED,
                slaDeadline = System.currentTimeMillis() + (detection.criticality.slaDays * 86400000L)
            )
            val result = repository.submitReport(uid, report, null)
            if (result.isSuccess) {
                _currentStep.value = ReportStep.Success(result.getOrDefault(id))
            }
        }
    }

    fun reset() {
        _currentStep.value = ReportStep.CaptureImage
        viewModelScope.launch {
            val loc = locationProvider.getLastLocation()
            _reportLat = loc.latitude
            _reportLng = loc.longitude
        }
    }
}
