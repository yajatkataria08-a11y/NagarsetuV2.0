package com.nagarsetu.core.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.data.RouteRepository
import com.nagarsetu.core.utils.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val routeInfo: RouteInfo? = null,
        val userLocation: GeoPoint? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentSpeedKph = MutableStateFlow(0f)
    val currentSpeedKph: StateFlow<Float> = _currentSpeedKph.asStateFlow()

    fun startSpeedTracking() {
        viewModelScope.launch {
            locationProvider.locationFlow().collect { loc ->
                // Android Location.speed is in m/s — convert to km/h
                _currentSpeedKph.update { loc.speedMps * 3.6f }
            }
        }
    }

    fun startNavigationTracking(onLocation: (GeoPoint, Float) -> Unit) {
        viewModelScope.launch {
            locationProvider.navigationLocationFlow().collect { loc ->
                val speedKph = loc.speedMps * 3.6f
                _currentSpeedKph.update { speedKph }
                onLocation(GeoPoint(loc.latitude, loc.longitude), speedKph)
            }
        }
    }

    fun loadRoute(destLat: Double, destLng: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get current user location
            val userLocPoint = locationProvider.getLastLocation()
            val userLoc = GeoPoint(userLocPoint.latitude, userLocPoint.longitude)

            _uiState.update { it.copy(userLocation = userLoc) }

            // Fetch OSRM route
            routeRepository.getRoute(
                originLat = userLoc.latitude,
                originLng = userLoc.longitude,
                destLat = destLat,
                destLng = destLng
            ).onSuccess { route ->
                _uiState.update {
                    it.copy(isLoading = false, routeInfo = route)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Could not fetch route. Check connection.",
                        routeInfo = null
                    )
                }
            }
        }
    }

    suspend fun getFreshLocation(): GeoPoint? {
        return try {
            val loc = locationProvider.getLastLocation()
            GeoPoint(loc.latitude, loc.longitude)
        } catch (e: Exception) {
            null
        }
    }
}
