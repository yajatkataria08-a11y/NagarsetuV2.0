package com.nagarsetu.roadwatch.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.theme.BHOPAL_LAT
import com.nagarsetu.core.ui.theme.BHOPAL_LNG
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.roadwatch.data.RoadWatchRepository
import com.nagarsetu.roadwatch.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── Tab indices ───────────────────────────────────────────────────────────────
object RoadWatchTab {
    const val REPORT = 0
    const val TRACK  = 1
}

// ─── Guided report wizard state ───────────────────────────────────────────────
data class ReportWizardState(
    val step: Int = 0,                       // 0=type 1=photo 2=severity 3=confirm
    val type: ReportType? = null,
    val photo: Bitmap? = null,
    val latitude: Double = BHOPAL_LAT,       // overwritten by GPS on wizard open
    val longitude: Double = BHOPAL_LNG,
    val severity: Severity = Severity.MEDIUM,
    val description: String = "",
    val detectionResult: PotholeDetection? = null,
    val isDetecting: Boolean = false,
    val isSubmitting: Boolean = false,
    val submittedReport: RoadReport? = null,
    val error: String? = null
)

// ─── Tracker filter ───────────────────────────────────────────────────────────
data class TrackerFilter(
    val statusFilter: ReportStatus? = null,
    val typeFilter: ReportType? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class RoadWatchViewModel @Inject constructor(
    private val repository: RoadWatchRepository,
    private val userRepository: SupabaseUserRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    // ── Active tab ─────────────────────────────────────────────────────────────
    private val _activeTab = MutableStateFlow(RoadWatchTab.REPORT)
    val activeTab = _activeTab.asStateFlow()
    fun setTab(tab: Int) { _activeTab.value = tab }

    // ── Wizard ─────────────────────────────────────────────────────────────────
    private val _wizard = MutableStateFlow(ReportWizardState())
    val wizard = _wizard.asStateFlow()

    init {
        // Seed wizard lat/lng from GPS immediately so the first report doesn't
        // land on the static Bhopal-centre fallback.
        viewModelScope.launch {
            val loc = locationProvider.getLastLocation()
            _wizard.value = _wizard.value.copy(latitude = loc.latitude, longitude = loc.longitude)
        }
    }

    fun selectType(type: ReportType) {
        _wizard.value = _wizard.value.copy(type = type, step = 1)
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _wizard.value = _wizard.value.copy(photo = bitmap, isDetecting = true, step = 2)
        viewModelScope.launch {
            val detection = repository.detectPothole(bitmap)
            _wizard.value = _wizard.value.copy(
                detectionResult = detection,
                severity = detection.criticality,
                isDetecting = false
            )
        }
    }

    fun onLocationPicked(lat: Double, lng: Double) {
        _wizard.value = _wizard.value.copy(latitude = lat, longitude = lng)
    }

    fun setSeverity(severity: Severity) {
        _wizard.value = _wizard.value.copy(severity = severity)
    }

    fun setDescription(text: String) {
        _wizard.value = _wizard.value.copy(description = text)
    }

    fun setStep(step: Int) {
        _wizard.value = _wizard.value.copy(step = step.coerceIn(0, 3))
    }

    fun submitReport() {
        val w = _wizard.value
        val type = w.type ?: return
        val uid = userRepository.profileFlow.value?.uid ?: "guest_user"

        _wizard.value = w.copy(isSubmitting = true)
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            
            val report = RoadReport(
                id = id,
                type = type,
                latitude = w.latitude,
                longitude = w.longitude,
                severity = w.severity,
                depthMm = w.detectionResult?.depthEstimateMm,
                timestamp = System.currentTimeMillis(),
                status = ReportStatus.SUBMITTED,
                slaDeadline = System.currentTimeMillis() + (w.severity.slaDays * 86400000L),
                imageUri = null,
                description = w.description.ifBlank { null },
                verifiedByAi = w.detectionResult != null
            )

            val result = repository.submitReport(uid, report, w.photo)
            
            if (result.isSuccess) {
                _wizard.value = _wizard.value.copy(
                    isSubmitting = false,
                    submittedReport = report,
                    step = 3
                )
            } else {
                _wizard.value = _wizard.value.copy(
                    isSubmitting = false,
                    error = "Submission failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun resetWizard() {
        viewModelScope.launch {
            val loc = locationProvider.getLastLocation()
            _wizard.value = ReportWizardState(latitude = loc.latitude, longitude = loc.longitude)
        }
    }

    // ── Tracker ────────────────────────────────────────────────────────────────
    val allReports = repository.reports

    private val _filter = MutableStateFlow(TrackerFilter())
    val filter = _filter.asStateFlow()

    val filteredReports: StateFlow<List<RoadReport>> = combine(allReports, _filter) { reports, f ->
        reports.filter { report ->
            val matchStatus = f.statusFilter == null || report.status == f.statusFilter
            val matchType = f.typeFilter == null || report.type == f.typeFilter
            val matchSearch = f.searchQuery.isBlank() ||
                report.id.contains(f.searchQuery, ignoreCase = true) ||
                report.type.label.contains(f.searchQuery, ignoreCase = true) ||
                report.description?.contains(f.searchQuery, ignoreCase = true) == true
            matchStatus && matchType && matchSearch
        }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setStatusFilter(status: ReportStatus?) { _filter.value = _filter.value.copy(statusFilter = status) }
    fun setTypeFilter(type: ReportType?) { _filter.value = _filter.value.copy(typeFilter = type) }
    fun setSearchQuery(q: String) { _filter.value = _filter.value.copy(searchQuery = q) }
    fun clearFilters() { _filter.value = TrackerFilter() }

    // ── Report detail / escalation ─────────────────────────────────────────────
    private val _detailReport = MutableStateFlow<RoadReport?>(null)
    val detailReport = _detailReport.asStateFlow()

    fun openDetail(id: String) { _detailReport.value = allReports.value.find { it.id == id } }
    fun closeDetail() { _detailReport.value = null }

    fun escalateReport(id: String) {
        viewModelScope.launch {
            val result = repository.escalateReport(id)
            if (result.isSuccess) {
                val escalated = result.getOrNull()
                _detailReport.value = escalated
                _escalationEvent.emit("Escalated to ${escalated?.authorityLevel?.label ?: "next authority"}")
            }
        }
    }

    private val _escalationEvent = MutableSharedFlow<String>()
    val escalationEvent = _escalationEvent.asSharedFlow()

    // ── SLA helper ─────────────────────────────────────────────────────────────
    fun slaFor(report: RoadReport): SlaInfo = repository.slaFor(report)

    // ── Authority & contractor ─────────────────────────────────────────────────
    fun authorityFor(report: RoadReport): AuthorityMapping =
        AuthorityMapping(
            wardName = "Ward ${report.wardId}",
            wardOfficerName = "Officer",
            wardPhone = "0755-123456",
            zonalOfficeName = "Zonal Office",
            zonalPhone = "0755-654321"
        )

    fun contractorFor(report: RoadReport): Contractor =
        Contractor("CON_1", "Contractor", "9876543210", report.type.name, emptyList(), 24, 4.0f)

    // ── Heatmap ────────────────────────────────────────────────────────────────
    val clusters: StateFlow<List<PotholeCluster>> = allReports
        .map { reports -> repository.clusterReports(reports) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val heatmapMarkers: StateFlow<List<MapMarker>> = clusters.map { clusterList ->
        if (clusterList.isEmpty()) {
            // Realistic Bhopal Fallback data (Hazard/Accident prone spots)
            listOf(
                MapMarker(23.2324, 77.4294, "Hazard Intensity: High", "MP Nagar Zone 1", android.graphics.Color.argb(160, 244, 67, 54), 450f),
                MapMarker(23.2842, 77.3694, "Hazard Intensity: Severe", "Lalghati Square", android.graphics.Color.argb(180, 183, 28, 28), 500f),
                MapMarker(23.2325, 77.4045, "Hazard Intensity: Medium", "New Market", android.graphics.Color.argb(140, 255, 152, 0), 300f),
                MapMarker(23.2100, 77.4050, "Hazard Intensity: High", "Misrod / Hoshangabad Rd", android.graphics.Color.argb(160, 244, 67, 54), 400f),
                MapMarker(23.1800, 77.4100, "Hazard Intensity: Moderate", "Kolar Main Road", android.graphics.Color.argb(120, 255, 235, 59), 350f)
            )
        } else {
            clusterList.map { cluster ->
                val color = when (cluster.dominantSeverity) {
                    Severity.LOW -> android.graphics.Color.argb(100, 76, 175, 80)
                    Severity.MEDIUM -> android.graphics.Color.argb(120, 255, 235, 59)
                    Severity.HIGH -> android.graphics.Color.argb(140, 255, 152, 0)
                    Severity.CRITICAL -> android.graphics.Color.argb(160, 244, 67, 54)
                }
                // Intensity grows with report count
                val radius = (150f + (cluster.count * 40f)).coerceAtMost(600f)
                MapMarker(
                    lat = cluster.centerLat,
                    lng = cluster.centerLng,
                    title = "Cluster Hotspot",
                    snippet = "${cluster.count} Active Reports\nStatus: ${cluster.dominantSeverity.label}",
                    heatColor = color,
                    radius = radius
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Nearby — uses real GPS ─────────────────────────────────────────────────
    suspend fun nearbyReports(): List<Pair<RoadReport, Double>> {
        val loc = locationProvider.getLastLocation()
        return repository.nearbyReports(loc.latitude, loc.longitude)
    }
}
