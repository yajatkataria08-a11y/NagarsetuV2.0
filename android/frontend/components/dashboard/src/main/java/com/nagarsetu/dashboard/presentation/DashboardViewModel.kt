package com.nagarsetu.dashboard.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.dashboard.data.repository.DashboardRepository
import com.nagarsetu.dashboard.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CityReportUi(val label: String, val count: Int, val color: Color)
data class SmartServiceItemUi(val name: String, val desc: String, val icon: ImageVector, val color: Color, val route: String)
data class MapPinUi(val x: Float, val y: Float, val color: Color)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val locationProvider: com.nagarsetu.core.utils.LocationProvider
) : ViewModel() {

    private val _mapMode = MutableStateFlow(MapMode.CIVIC)
    val mapMode = _mapMode.asStateFlow()

    val crisisLevel = repository.crisisLevel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrisisLevel.NORMAL)
    val alerts = repository.alerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveAlerts = repository.liveAlerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weather = repository.weather.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val aqi = repository.aqi.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val news = repository.news.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _markers = MutableStateFlow<List<MapMarker>>(emptyList())
    
    private val _isMapLoading = MutableStateFlow(false)
    val isMapLoading = _isMapLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val markers = combine(_markers, _searchQuery) { markers, query ->
        if (query.isBlank()) markers
        else markers.filter { it.title.contains(query, ignoreCase = true) || it.snippet?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cityStats = MutableStateFlow(CityStats(total = 0, pending = 0, resolved = 0))
    val cityStats = _cityStats.asStateFlow()

    private val _reports = MutableStateFlow<List<CityReportUi>>(emptyList())
    val reports = _reports.asStateFlow()

    val smartServices = MutableStateFlow(listOf(
        SmartServiceItemUi("NagarSetu AI",  "Your intelligent city guide",    Icons.Filled.AutoAwesome,    Color(0xFF6200EE), "assistant"),
        SmartServiceItemUi("Road Watch",    "AI Road hazard reporting",       Icons.Filled.AddRoad,        Color(0xFFCE93D8), "road_watch"),
        SmartServiceItemUi("Authority",     "Contact ward officers",          Icons.Filled.AccountBalance, Color(0xFF90CAF9), "authority"),
        SmartServiceItemUi("ParkEase",      "Find & reserve parking spots",   Icons.Filled.DirectionsCar,  Color(0xFF4FC3F7), "park_ease"),
        SmartServiceItemUi("ChargeUp",      "Locate EV charging stations",    Icons.Filled.ElectricBolt,   Color(0xFFFFD54F), "charge_up"),
        SmartServiceItemUi("GreenRoute",    "Eco-friendly navigation",        Icons.Filled.Park,           Color(0xFF81C784), "green_route"),
        SmartServiceItemUi("Predictive AI", "Risk forecast & RAG assistant",  Icons.Filled.Psychology,     Color(0xFFCE93D8), "predictive"),
        SmartServiceItemUi("DriveLegal",    "Traffic rules & legal support",  Icons.Filled.Gavel,          Color(0xFF90CAF9), "drive_legal"),
        SmartServiceItemUi("HealthWatch",   "City health & outbreak alerts",  Icons.Filled.LocalHospital,  Color(0xFFEF9A9A), "health_watch"),
    ))

    // Fix R5: mockPins were hardcoded and always visible in production.
    // In DEBUG builds they provide a visual sanity-check for the map overlay;
    // in release they are an empty list so real Supabase-backed markers take over.
    val mockPins: StateFlow<List<MapPinUi>> = MutableStateFlow(
        if (com.nagarsetu.dashboard.BuildConfig.DEBUG) listOf(
            MapPinUi(0.22f, 0.38f, Color.Red),
            MapPinUi(0.47f, 0.52f, NagarSetuColors.Accent),
            MapPinUi(0.63f, 0.31f, NagarSetuColors.WarningOrange),
            MapPinUi(0.75f, 0.65f, NagarSetuColors.SuccessGreen),
        ) else emptyList()
    ).asStateFlow()

    init {
        refreshMarkers()
        startFeed()
        fetchSupabaseStats()
        startPeriodicMapUpdate()
    }

    private fun startPeriodicMapUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // Every 30 seconds
                if (_mapMode.value == MapMode.TRAFFIC) {
                    refreshMarkers()
                }
            }
        }
    }

    private fun fetchSupabaseStats() {
        viewModelScope.launch {
            val kpis = repository.fetchWardKpis()
            if (kpis.isNotEmpty()) {
                val total = kpis.sumOf { it.totalReports }
                val resolved = kpis.sumOf { it.resolvedReports }
                val critical = kpis.sumOf { it.criticalOpen }
                
                _cityStats.value = CityStats(total = total, pending = total - resolved, resolved = resolved)
                _reports.value = listOf(
                    CityReportUi("Resolved",    resolved, NagarSetuColors.SuccessGreen),
                    CityReportUi("Pending",      total - resolved, NagarSetuColors.WarningOrange),
                    CityReportUi("Critical",     critical, Color.Red),
                )
            }
            // If kpis is empty, leave reports empty (skeleton loaders stay visible)
        }
    }

    fun setMapMode(mode: MapMode) {
        _mapMode.value = mode
        refreshMarkers()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun allWards() = repository.allWards()

    private fun refreshMarkers() {
        viewModelScope.launch {
            _isMapLoading.value = true
            try {
                val loc = locationProvider.getLastLocation()
                _markers.value = repository.markersForMode(
                    mode = _mapMode.value,
                    lat = loc.latitude,
                    lng = loc.longitude
                )
            } catch (e: Exception) {
                // Log or handle error
            } finally {
                _isMapLoading.value = false
            }
        }
    }

    private fun startFeed() {
        // Fix #13: mock alert generator was running in production, showing fabricated civic signals
        // to real users. Now gated behind DEBUG; production will use a Supabase Realtime
        // subscription here once the backend is live.
        if (!com.nagarsetu.dashboard.BuildConfig.DEBUG) return

        viewModelScope.launch {
            while (true) {
                delay(12_000)
                repository.pushAlert(
                    DashboardAlert(
                        UUID.randomUUID().toString(),
                        "Live Update",
                        "[DEBUG] Simulated civic signal near Bhopal center",
                        AlertType.CIVIC,
                        "Ward ${(1..13).random()}",
                        System.currentTimeMillis()
                    )
                )
                _cityStats.update { it.copy(total = it.total + 1, pending = it.pending + 1) }
            }
        }
    }
}
