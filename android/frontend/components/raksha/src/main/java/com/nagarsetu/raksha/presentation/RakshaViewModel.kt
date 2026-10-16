package com.nagarsetu.raksha.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.raksha.data.RakshaRepository
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.core.data.local.ProfileLocalRepository
import com.nagarsetu.raksha.data.incident.IncidentReport
import com.nagarsetu.raksha.data.incident.IncidentType
import com.nagarsetu.raksha.data.incident.RiskApiRepository
import com.nagarsetu.raksha.data.routing.GeoUtils
import com.nagarsetu.raksha.data.routing.LatLonPoint
import com.nagarsetu.raksha.data.sensor.RakshaShakeDetector
import com.nagarsetu.raksha.data.wearos.WatchSosManager
import com.nagarsetu.raksha.domain.model.DisasterAlert
import com.nagarsetu.raksha.domain.model.SafeRoute
import com.nagarsetu.raksha.domain.model.SafetyContact
import com.nagarsetu.raksha.domain.model.PoliceStation
import com.nagarsetu.raksha.domain.model.PoliceStationProvider
import com.nagarsetu.backend.core.assistant.AiChatbotService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SosTriggerSource { WATCH, SHAKE, MANUAL_TAP }

@HiltViewModel
class RakshaViewModel @Inject constructor(
    application: Application,
    private val repository: RakshaRepository,
    private val profileRepo: ProfileLocalRepository,
    private val riskApiRepository: RiskApiRepository,
    private val watchSosManager: WatchSosManager,
    val locationProvider: LocationProvider,
    private val authRepository: com.nagarsetu.auth.data.AuthRepository,
    private val twilioOtpService: com.nagarsetu.auth.data.TwilioOtpService,
    private val aiService: AiChatbotService
) : AndroidViewModel(application) {

    // ── SOS and Events ───────────────────────────────────────────────────────
    private val _sosEvent = MutableSharedFlow<String>()
    val sosEvent = _sosEvent.asSharedFlow()

    private val _isSosLoading = MutableStateFlow(false)
    val isSosLoading = _isSosLoading.asStateFlow()

    // ── Emergency AI State ──────────────────────────────────────────────────
    private val _emergencyDescription = MutableStateFlow("")
    val emergencyDescription = _emergencyDescription.asStateFlow()

    private val _aiGuidanceResponse = MutableStateFlow<String?>(null)
    val aiGuidanceResponse = _aiGuidanceResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // ── original state ───────────────────────────────────────────────────────

    private val _isSharingLocation = MutableStateFlow(false)
    val isSharingLocation = _isSharingLocation.asStateFlow()

    private val _userLocation = MutableStateFlow(com.nagarsetu.core.utils.LatLngPoint(23.2599, 77.4126))
    val userLocation = _userLocation.asStateFlow()

    private val _disasterAlerts = MutableStateFlow<List<DisasterAlert>>(emptyList())
    val disasterAlerts = _disasterAlerts.asStateFlow()

    private val _trustedContacts = MutableStateFlow<List<SafetyContact>>(emptyList())
    val trustedContacts = _trustedContacts.asStateFlow()

    private val _isFakeCallIncoming = MutableStateFlow(false)
    val isFakeCallIncoming = _isFakeCallIncoming.asStateFlow()

    private val _sosTriggered = MutableStateFlow(false)
    val sosTriggered = _sosTriggered.asStateFlow()

    private val _sosTriggerSource = MutableStateFlow<SosTriggerSource?>(null)
    val sosTriggerSource = _sosTriggerSource.asStateFlow()

    private val _navigateToLiveTrack = MutableStateFlow(false)
    val navigateToLiveTrack = _navigateToLiveTrack.asStateFlow()

    val isWatchConnected = watchSosManager.isWatchConnected
    val heartRate = watchSosManager.heartRate

    // ── new: routes & risk (from Raksha upgrade) ─────────────────────────────

    private val _routes = MutableStateFlow<List<SafeRoute>>(emptyList())
    val routes = _routes.asStateFlow()

    private val _isLoadingRoutes = MutableStateFlow(false)
    val isLoadingRoutes = _isLoadingRoutes.asStateFlow()

    /** Risk score at the user's current location (0.0 = safe). */
    private val _currentRisk = MutableStateFlow(0.0)
    val currentRisk = _currentRisk.asStateFlow()

    /** Crowd-sourced incident reports loaded from the backend. */
    private val _incidents = MutableStateFlow<List<IncidentReport>>(emptyList())
    val incidents = _incidents.asStateFlow()

    /** Heatmap data: list of (lat, lng, weight) for the map overlay. */
    private val _heatmapPoints = MutableStateFlow<List<Triple<Double, Double, Double>>>(emptyList())
    val heatmapPoints = _heatmapPoints.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError = _routeError.asStateFlow()

    // ── location sharing ────────────────────────────────────────────────────

    private var locationSharingJob: Job? = null
    private val _sharingStatus = MutableStateFlow("Sharing stopped")
    val sharingStatus = _sharingStatus.asStateFlow()

    private var activeSosId: String? = null

    // ── sensors ──────────────────────────────────────────────────────────────

    private var shakeDetector: RakshaShakeDetector? = null

    // ── init ─────────────────────────────────────────────────────────────────

    init {
        _disasterAlerts.value = repository.disasterAlerts()
        
        // Combine Profile Contacts with Helplines
        profileRepo.trustedContacts.onEach { contacts ->
            val safetyContacts = contacts.map { SafetyContact(it.name, it.phone, true) }
            _trustedContacts.value = safetyContacts + repository.womenHelplines()
        }.launchIn(viewModelScope)

        // Shake detector disabled for pure SOS button focus
        /*
        shakeDetector = RakshaShakeDetector(getApplication()) {
            triggerSos(SosTriggerSource.SHAKE)
        }
        shakeDetector?.start()
        */

        // Collect real-time incidents from Supabase
        viewModelScope.launch {
            repository.incidentRepository.incidentFlow.collect { list ->
                _incidents.value = list
                // Update heatmap points based on live incidents
                val points = list.map { Triple(it.latitude, it.longitude, it.severity.toDouble()) }
                _heatmapPoints.value = points
            }
        }

        // Continuous location updates for tracking and risk calculation
        viewModelScope.launch {
            locationProvider.locationFlow(intervalMs = 5000L, minDistanceM = 2f).collect { loc ->
                _userLocation.value = loc
                updateCurrentRisk(loc.latitude, loc.longitude)
            }
        }

        // Collect watch SOS events
        viewModelScope.launch {
            watchSosManager.watchSosReceived.collect { triggered ->
                if (triggered) {
                    triggerSos(SosTriggerSource.WATCH)
                    watchSosManager.clearWatchSos()
                    watchSosManager.sendAcknowledgement()
                }
            }
        }

        // Seed heatmap from local crime data immediately (no network required)
        refreshHeatmap()
    }

    // ── public actions ────────────────────────────────────────────────────────

    fun triggerSos(source: SosTriggerSource) {
        if (_sosTriggered.value) return // already triggered

        viewModelScope.launch {
            _isSosLoading.value = true
            
            // 1. Validation: Trusted Contacts
            val contacts = _trustedContacts.value.filter { it.isEmergencyContact }
            if (contacts.isEmpty()) {
                _sosEvent.emit("Please add at least one trusted contact.")
                _isSosLoading.value = false
                return@launch
            }
            
            // Note: Permissions are handled in UI layer (inline request)
            // but we check if we actually have location for the payload
            val initialLoc = locationProvider.currentLocation.value
            if (!initialLoc.isGpsFix) {
                _sosEvent.emit("Location permission needed for emergency alerts.")
                // We continue anyway with default location as fallback
            }

            // 2. Trigger SOS State
            _sosTriggered.value = true
            _sosTriggerSource.value = source

            // 3. Vibration feedback
            try {
                val vibrator = getApplication<Application>().getSystemService(android.os.Vibrator::class.java)
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (e: Exception) {
                Log.e("RakshaVM", "Vibration failed: ${e.message}")
            }

            var dashboardSuccess = false
            var smsSuccess = false

            // 4. Start sharing (Fail-safe: non-blocking)
            try {
                if (!_isSharingLocation.value) {
                    toggleLocationSharing()
                }
            } catch (e: Exception) {
                Log.e("RakshaVM", "Sharing failed: ${e.message}")
            }

            // 5. Trigger Backend SOS Alert
            try {
                val loc = locationProvider.currentLocation.value // use latest from flow
                val session = authRepository.getSession()
                
                val batteryLevel = try {
                    val bm = getApplication<Application>().getSystemService(android.os.BatteryManager::class.java)
                    bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                } catch (e: Exception) { -1 }

                activeSosId = repository.incidentRepository.submitSos(
                    latitude    = loc.latitude,
                    longitude   = loc.longitude,
                    contact     = "SOS triggered via NagarSetu",
                    userId      = session?.uid ?: "anonymous",
                    userName    = session?.name ?: "NagarSetu User",
                    phone       = session?.phone ?: "",
                    ward        = "Unknown",
                    batteryLevel = batteryLevel
                )
                dashboardSuccess = activeSosId != null
            } catch (e: Exception) {
                Log.e("RakshaVM", "Backend alert failed: ${e.message}")
            }

            // 6. Send SMS to all trusted contacts (Twilio + Local Fallback)
            try {
                val currentLoc = locationProvider.currentLocation.value
                val mapsUrl = "https://maps.google.com/?q=${currentLoc.latitude},${currentLoc.longitude}"
                val message = "🚨 SOS! EMERGENCY from NagarSetu user. Track my live location here: $mapsUrl"

                var smsSentCount = 0
                for (contact in contacts) {
                    // Try Twilio first
                    val result = twilioOtpService.sendSosSms(contact.phoneNumber, message)
                    if (result is com.nagarsetu.auth.domain.model.AuthResult.Success) {
                        smsSentCount++
                    } else {
                        Log.e("RakshaVM", "Twilio SMS failed for ${contact.phoneNumber}")
                    }
                }
                smsSuccess = smsSentCount > 0
            } catch (e: Exception) {
                Log.e("RakshaVM", "SMS loop failed: ${e.message}")
            }

            // 7. Results
            _isSosLoading.value = false
            when {
                dashboardSuccess && smsSuccess -> 
                    _sosEvent.emit("Success and SMS sent")
                dashboardSuccess -> 
                    _sosEvent.emit("Success")
                smsSuccess ->
                    _sosEvent.emit("SMS sent (Dashboard offline)")
                else -> 
                    _sosEvent.emit("Emergency services temporarily unavailable.")
            }
        }
    }

    fun toggleLocationSharing() {
        val newState = !_isSharingLocation.value
        _isSharingLocation.value = newState
        
        if (newState) {
            startSharing()
        } else {
            stopSharing()
        }
    }

    private fun startSharing() {
        locationSharingJob?.cancel()
        locationSharingJob = viewModelScope.launch {
            _sharingStatus.value = "Initialising tracking..."
            
            // 1. Get current location
            val initialLoc = locationProvider.getLastLocation()
            val mapsUrl = "https://maps.google.com/?q=${initialLoc.latitude},${initialLoc.longitude}"
            
            // 2. Alert trusted contacts via SMS (one-time alert)
            val contacts = _trustedContacts.value.filter { it.isEmergencyContact }
            if (contacts.isNotEmpty()) {
                contacts.forEach { contact ->
                    sendLocationSms(contact.phoneNumber, mapsUrl)
                }
                _sharingStatus.value = "Live sharing active (${contacts.size} contacts notified)"
            } else {
                _sharingStatus.value = "Sharing active (no trusted contacts found)"
            }

            // 3. Continuous background update loop (simulated sharing with server)
            while (isActive) {
                val currentLoc = _userLocation.value
                val updateUrl = "https://maps.google.com/?q=${currentLoc.latitude},${currentLoc.longitude}"
                
                val batteryLevel = try {
                    val bm = getApplication<Application>().getSystemService(android.os.BatteryManager::class.java)
                    bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                } catch (e: Exception) { -1 }

                // Update Admin Dashboard
                if (activeSosId != null) {
                    repository.incidentRepository.updateSosLocation(
                        sosId = activeSosId!!,
                        latitude = currentLoc.latitude,
                        longitude = currentLoc.longitude,
                        batteryLevel = batteryLevel
                    )
                } else {
                    // Fallback to legacy SOS if ID is missing
                    repository.incidentRepository.submitSos(
                        currentLoc.latitude, 
                        currentLoc.longitude, 
                        "Live Tracking Update"
                    )
                }
                
                Log.d("RakshaVM", "Live location updated: $updateUrl")
                delay(20_000L) // Update every 20 seconds
            }
        }
    }

    private fun stopSharing() {
        locationSharingJob?.cancel()
        locationSharingJob = null
        _sharingStatus.value = "Sharing stopped"
    }

    private fun sendLocationSms(phone: String, url: String) {
        viewModelScope.launch {
            val message = "🚨 NAGARSETU ALERT: I am sharing my live location with you for my safety. Follow me here: $url"
            val result = twilioOtpService.sendSosSms(phone, message)
            if (result is com.nagarsetu.auth.domain.model.AuthResult.Success) {
                Log.i("RakshaVM", "Location sharing SMS sent to $phone")
            } else {
                Log.e("RakshaVM", "Failed to send location sharing SMS to $phone")
            }
        }
    }

    fun triggerFakeCall(delaySeconds: Int) {
        viewModelScope.launch {
            delay(delaySeconds * 1000L)
            _isFakeCallIncoming.value = true
        }
    }

    fun simulateWatchSos() = watchSosManager.simulateWatchSos()
    fun answerFakeCall()   { _isFakeCallIncoming.value = false }
    fun rejectFakeCall()   { _isFakeCallIncoming.value = false }
    fun callContact(context: android.content.Context, phoneNumber: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RakshaVM", "Failed to call contact: ${e.message}")
        }
    }

    fun messageContact(context: android.content.Context, phoneNumber: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", "🚨 SOS! I need help. My current location is: https://maps.google.com/?q=${_userLocation.value.latitude},${_userLocation.value.longitude}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RakshaVM", "Failed to message contact: ${e.message}")
        }
    }

    fun clearSos()         { 
        _sosTriggered.value = false
        _sosTriggerSource.value = null
        _aiGuidanceResponse.value = null
        _emergencyDescription.value = ""
        activeSosId = null
    }

    fun saveTrustedContact(phone: String) {
        viewModelScope.launch {
            val contacts = profileRepo.trustedContacts.first().toMutableList()
            if (contacts.isEmpty()) {
                contacts.add(com.nagarsetu.auth.domain.model.TrustedContact("Emergency Contact", phone, "Emergency"))
            } else {
                contacts[0] = contacts[0].copy(phone = phone)
            }
            profileRepo.saveContacts(contacts)
            
            // Sync to remote if logged in
            val user = authRepository.getSession()
            if (user != null && !user.isGuest) {
                authRepository.saveTrustedContactsRemote(user.uid, contacts)
            }
        }
    }

    fun triggerSosTap()    { triggerSos(SosTriggerSource.MANUAL_TAP) }
    fun openLiveTrack()    { _navigateToLiveTrack.value = true }
    fun liveTrackNavigated() { _navigateToLiveTrack.value = false }

    fun getNearestPoliceStation(): PoliceStation? {
        val loc = _userLocation.value
        return PoliceStationProvider.stations.minByOrNull { station ->
            GeoUtils.distanceMeters(loc.latitude, loc.longitude, station.lat, station.lng)
        }
    }

    fun navigateToPolice(context: android.content.Context) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:112")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RakshaVM", "Failed to open dialer: ${e.message}")
        }
    }

    fun navigateToAmbulance(context: android.content.Context) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:108")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RakshaVM", "Failed to open dialer: ${e.message}")
        }
    }

    fun getAiGuidance(description: String) {
        if (description.isBlank()) return
        _emergencyDescription.value = description
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val prompt = "EMERGENCY SITUATION: $description. Provide brief, life-saving first-aid or safety instructions in 2-3 sentences."
                val response = withContext(Dispatchers.IO) {
                    aiService.getAiResponse(prompt)
                }
                _aiGuidanceResponse.value = response.answer
            } catch (e: Exception) {
                _aiGuidanceResponse.value = "Stay calm. Emergency services are being notified. If there's bleeding, apply pressure."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun shareEmergencyLocation(context: android.content.Context) {
        val loc = _userLocation.value
        val locUrl = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        val msg = "🚨 EMERGENCY! I need help. This is my current location:\n$locUrl"
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, msg)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Emergency Location"))
    }

    // ── routing (Raksha upgrade) ──────────────────────────────────────────────

    /**
     * Plans Fastest / Balanced / Safest routes from [origin] to [destination].
     * Also syncs crowd-report data into the risk engine first.
     */
    fun planRoutes(
        origin: LatLonPoint,
        destination: LatLonPoint,
        stickToMainRoads: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoadingRoutes.value = true
            _routeError.value = null
            try {
                repository.syncCrowdReports()
                _routes.value = repository.generateRoutes(origin, destination, stickToMainRoads)
            } catch (e: Exception) {
                _routeError.value = "Could not calculate routes: ${e.message}"
            } finally {
                _isLoadingRoutes.value = false
            }
        }
    }

    fun clearRoutes() {
        _routes.value = emptyList()
    }

    // ── risk at current location ──────────────────────────────────────────────

    /**
     * Updates [currentRisk] for the given coordinates.
     * Fetches active hazard zones to include dynamic overlays.
     */
    fun updateCurrentRisk(lat: Double, lng: Double) {
        viewModelScope.launch {
            val hazards = repository.hazardZoneFetcher.fetchActiveZones()
            _currentRisk.value = repository.riskAt(lat, lng, hazards)
        }
    }

    // ── incidents ─────────────────────────────────────────────────────────────

    fun loadIncidents() {
        viewModelScope.launch {
            _incidents.value = repository.incidentRepository.getActiveIncidents()
        }
    }

    fun submitIncidentReport(
        lat: Double,
        lng: Double,
        type: IncidentType,
        description: String,
        severity: Int = 3
    ) {
        viewModelScope.launch {
            val report = IncidentReport(
                type        = type,
                latitude    = lat,
                longitude   = lng,
                description = description,
                severity    = severity
            )
            repository.incidentRepository.submitReport(report)
            // Reload after submission
            loadIncidents()
        }
    }

    fun sendSos(lat: Double, lng: Double, contact: String) {
        viewModelScope.launch {
            _sosTriggered.value = true
            repository.incidentRepository.submitSos(lat, lng, contact)
        }
    }

    // ── heatmap ───────────────────────────────────────────────────────────────

    private fun refreshHeatmap() {
        viewModelScope.launch {
            val raw = repository.heatmapPoints(limit = 800)
            _heatmapPoints.value = raw.map { (pt, w) -> Triple(pt.lat, pt.lng, w) }
        }
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        shakeDetector?.stop()
        super.onCleared()
    }
}
