package com.nagarsetu.greenroute.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.greenroute.data.GtfsRepository
import com.nagarsetu.greenroute.domain.model.RouteOption
import com.nagarsetu.greenroute.domain.model.TransitStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GreenRouteViewModel @Inject constructor(
    private val gtfs: GtfsRepository,
    val locationProvider: LocationProvider
) : ViewModel() {

    private val _routeOptions   = MutableStateFlow<List<RouteOption>>(emptyList())
    val routeOptions = _routeOptions.asStateFlow()

    private val _nearbyStations = MutableStateFlow<List<TransitStation>>(emptyList())
    val nearbyStations = _nearbyStations.asStateFlow()

    private val _isRefreshing   = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        refresh()
        startLivePoll()
    }

    fun findRoutes(destination: String) {
        viewModelScope.launch {
            _routeOptions.value = gtfs.routeOptions(destination)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Use real GPS for station proximity; falls back to Bhopal centre
            val loc = locationProvider.getLastLocation()
            _nearbyStations.value = gtfs.getNearbyArrivals(loc.latitude, loc.longitude)
            _isRefreshing.value  = false
        }
    }

    /**
     * Polls GTFS arrivals every 30 s using the user's live GPS position so
     * nearby-station results stay accurate as the user moves around Bhopal.
     */
    private fun startLivePoll() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val loc = locationProvider.getLastLocation()
                _nearbyStations.value = gtfs.getNearbyArrivals(loc.latitude, loc.longitude)
            }
        }
    }
}
