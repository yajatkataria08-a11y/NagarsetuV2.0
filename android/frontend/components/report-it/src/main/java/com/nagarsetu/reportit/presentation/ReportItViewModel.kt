package com.nagarsetu.reportit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.auth.domain.usecase.GetSessionUseCase
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.core.ui.theme.BHOPAL_LAT
import com.nagarsetu.core.ui.theme.BHOPAL_LNG
import com.nagarsetu.reportit.data.ReportItRepository
import com.nagarsetu.reportit.domain.model.CivicIssueType
import com.nagarsetu.reportit.domain.model.CivicReport
import com.nagarsetu.core.utils.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReportItStep {
    data object Capture : ReportItStep()
    data object Describe : ReportItStep()
    data object Location : ReportItStep()
    data class Success(val report: CivicReport) : ReportItStep()
}

@HiltViewModel
class ReportItViewModel @Inject constructor(
    private val repository: ReportItRepository,
    private val userRepository: SupabaseUserRepository,
    val locationProvider: LocationProvider
) : ViewModel() {

    private val _step = MutableStateFlow<ReportItStep>(ReportItStep.Capture)
    val step = _step.asStateFlow()

    private val _issueType = MutableStateFlow(CivicIssueType.GARBAGE)
    val issueType = _issueType.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    // track the coordinates the user pins in the Location step.
    private val _selectedLat = MutableStateFlow(BHOPAL_LAT)
    val selectedLat = _selectedLat.asStateFlow()
    
    private val _selectedLng = MutableStateFlow(BHOPAL_LNG)
    val selectedLng = _selectedLng.asStateFlow()

    private val _useLiveLocation = MutableStateFlow(true)
    val useLiveLocation = _useLiveLocation.asStateFlow()

    fun setUseLiveLocation(use: Boolean) {
        _useLiveLocation.value = use
        if (use) {
            viewModelScope.launch {
                val loc = locationProvider.getLastLocation()
                _selectedLat.value = loc.latitude
                _selectedLng.value = loc.longitude
            }
        }
    }

    val wardPreview: String get() = repository.nearestWard(_selectedLat.value, _selectedLng.value)
    
    fun toDescribe() { _step.value = ReportItStep.Describe }
    fun onPhotoCaptured() { _step.value = ReportItStep.Describe }
    fun setType(type: CivicIssueType) { _issueType.value = type }
    fun setDescription(text: String) { _description.value = text }
    
    fun toLocation() { 
        if (_description.value.isNotBlank()) {
            _step.value = ReportItStep.Location 
            setUseLiveLocation(true) // Default to live when entering location step
        }
    }

    /** Called by the Location step when the user moves the map pin. */
    fun setLocation(lat: Double, lng: Double) {
        if (!_useLiveLocation.value) {
            _selectedLat.value = lat
            _selectedLng.value = lng
        }
    }

    fun submit() {
        val uid = userRepository.profileFlow.value?.uid ?: "guest_user"
        _isSubmitting.value = true
        viewModelScope.launch {
            // Ensure we have the absolute latest if live is on
            if (_useLiveLocation.value) {
                val loc = locationProvider.getLastLocation()
                _selectedLat.value = loc.latitude
                _selectedLng.value = loc.longitude
            }

            val result = repository.submitReport(
                uid = uid,
                type = _issueType.value,
                description = _description.value,
                lat = _selectedLat.value,
                lng = _selectedLng.value
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                _step.value = ReportItStep.Success(result.getOrThrow())
            }
        }
    }

    fun reset() { 
        _step.value = ReportItStep.Capture
        _description.value = ""
        _selectedLat.value = BHOPAL_LAT
        _selectedLng.value = BHOPAL_LNG
    }
}
