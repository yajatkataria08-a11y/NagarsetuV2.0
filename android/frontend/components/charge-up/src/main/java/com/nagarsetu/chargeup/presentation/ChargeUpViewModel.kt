package com.nagarsetu.chargeup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.chargeup.data.ChargingRepository
import com.nagarsetu.chargeup.domain.model.ChargingStation
import com.nagarsetu.core.utils.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChargeUpViewModel @Inject constructor(
    private val repository: ChargingRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _stations = MutableStateFlow<List<ChargingStation>>(emptyList())
    val stations = _stations.asStateFlow()

    init {
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            // Use real GPS; falls back to Bhopal centre if permission denied / no fix yet
            val loc = locationProvider.getLastLocation()
            _stations.value = repository.getStationsNearby(loc.latitude, loc.longitude, 15.0)
        }
    }

    /** Re-fetch stations centred on the user's current GPS position. */
    fun refreshNearby() {
        loadStations()
    }
}
