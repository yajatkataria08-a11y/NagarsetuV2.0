package com.nagarsetu.dashboard.data.repository

import android.util.Log
import com.google.gson.JsonObject
import com.nagarsetu.backend.core.config.ExternalApiConfig
import com.nagarsetu.backend.core.data.CivicDataHub
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.dashboard.data.network.DashboardApiService
import com.nagarsetu.chargeup.data.network.ChargingApiService
import com.nagarsetu.dashboard.data.service.CrisisManager
import com.nagarsetu.dashboard.data.service.LiveAlertService
import com.nagarsetu.dashboard.data.service.WardAuthorityRepository
import com.nagarsetu.dashboard.data.service.WardSortField
import com.nagarsetu.dashboard.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.nagarsetu.parkease.data.network.OsmOverpassApiService
import com.nagarsetu.parkease.data.network.OverpassQueries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LiveAlertDto(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val description: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val ward: String? = null,
    @SerialName("is_active")  val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class WardKpiDto(
    @SerialName("ward_number")     val wardNumber: Int,
    @SerialName("ward_name")       val wardName: String,
    val zone: String,
    @SerialName("officer_name")    val officerName: String,
    @SerialName("total_reports")   val totalReports: Int,
    @SerialName("resolved_reports") val resolvedReports: Int,
    @SerialName("critical_open")   val criticalOpen: Int,
    @SerialName("sla_breached")    val slaBreached: Int,
    @SerialName("resolution_pct")  val resolutionPct: Double
)

@Serializable
data class CitizenComplaintDto(
    val id: String,
    val category: String,
    val description: String,
    val status: String,
    @SerialName("ward_name") val wardName: String,
    @SerialName("submitted_at") val submittedAt: Long,
    @SerialName("resolved_at") val resolvedAt: Long? = null
)

@Serializable
data class BudgetTransparencyDto(
    val category: String,
    val allocated: Long,
    val spent: Long,
    val projects: Int
)

@Singleton
class DashboardRepository @Inject constructor(
    private val assets: AssetDataRepository,
    private val hub: CivicDataHub,
    val alertService: LiveAlertService,
    val wardRepo: WardAuthorityRepository,
    val crisisManager: CrisisManager,
    private val supabase: SupabaseClient,
    private val apiService: DashboardApiService,
    private val evApi: ChargingApiService,
    private val osmApi: OsmOverpassApiService,
    private val config: ExternalApiConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json  = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private var alertsChannel: RealtimeChannel? = null

    // ── Supabase Alerts ───────────────────────────────────────────────────────
    private val _liveAlerts = MutableStateFlow<List<LiveAlertDto>>(emptyList())
    val liveAlerts: StateFlow<List<LiveAlertDto>> = _liveAlerts.asStateFlow()

    // ── Weather & AQI ─────────────────────────────────────────────────────────
    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _aqi = MutableStateFlow<AqiInfo?>(null)
    val aqi: StateFlow<AqiInfo?> = _aqi.asStateFlow()

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    // ── Legacy Alerts (merged with live ones) ──────────────────────────────────
    private val _alerts = MutableStateFlow(seedAlerts())
    val alerts: StateFlow<List<DashboardAlert>> = _alerts.asStateFlow()

    val crisisLevel: StateFlow<CrisisLevel> = crisisManager.crisisLevel
    val radarAlerts: StateFlow<List<RadarAlert>> = crisisManager.radarAlerts

    init {
        // Fetch historical
        scope.launch { fetchInitialAlerts() }
        // Subscribe to real-time
        startLiveAlertSubscription()

        // Fetch Weather & AQI
        scope.launch { fetchWeather() }
        scope.launch { fetchAqi() }
        scope.launch { fetchNews() }

        // Existing local stream wiring
        scope.launch {
            alertService.alertStream().collect { newAlert ->
                val updated = (listOf(newAlert) + _alerts.value).take(20)
                _alerts.value = updated
                crisisManager.updateFromAlerts(updated)
            }
        }
    }

    // ── Supabase Actions ──────────────────────────────────────────────────────

    /** Fetches current active alerts from Supabase. */
    suspend fun fetchInitialAlerts(): List<LiveAlertDto> =
        runCatching {
            supabase.postgrest["live_alerts"]
                .select(Columns.ALL) {
                    filter { eq("is_active", true) }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<LiveAlertDto>()
                .also { _liveAlerts.value = it }
        }.getOrDefault(emptyList())

    /** Fetches ward KPI data from the `ward_kpi` Postgres VIEW. */
    suspend fun fetchWardKpis(): List<WardKpiDto> =
        runCatching {
            supabase.postgrest["ward_kpi"]
                .select(Columns.ALL) {
                    order("total_reports", Order.DESCENDING)
                }
                .decodeList<WardKpiDto>()
        }.getOrDefault(emptyList())

    suspend fun fetchCitizenComplaints(): List<CitizenComplaint> =
        runCatching {
            supabase.postgrest["citizen_complaints"]
                .select(Columns.ALL) {
                    order("submitted_at", Order.DESCENDING)
                    limit(20)
                }
                .decodeList<CitizenComplaintDto>()
                .map { dto ->
                    CitizenComplaint(
                        id = dto.id,
                        category = dto.category,
                        description = dto.description,
                        status = try { ComplaintStatus.valueOf(dto.status) } catch(e: Exception) { ComplaintStatus.PENDING },
                        wardName = dto.wardName,
                        submittedAt = dto.submittedAt,
                        resolvedAt = dto.resolvedAt
                    )
                }
        }.onFailure { e ->
            Log.e("DashboardRepository", "Failed to fetch complaints: ${e.message}", e)
        }.getOrElse { citizenComplaints() } // Fallback to mock for now if table missing

    suspend fun fetchBudgetTransparency(): List<BudgetTransparencyItem> =
        runCatching {
            supabase.postgrest["budget_transparency"]
                .select(Columns.ALL)
                .decodeList<BudgetTransparencyDto>()
                .map { dto ->
                    BudgetTransparencyItem(
                        category = dto.category,
                        allocated = dto.allocated,
                        spent = dto.spent,
                        projects = dto.projects
                    )
                }
        }.onFailure { e ->
            Log.e("DashboardRepository", "Failed to fetch budget transparency: ${e.message}", e)
        }.getOrElse { budgetTransparency() } // Fallback to mock

    private fun startLiveAlertSubscription() {
        scope.launch {
            runCatching {
                alertsChannel?.unsubscribe()
                val channel = supabase.realtime.channel("live_alerts_feed")
                alertsChannel = channel
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table  = "live_alerts"
                    filter = "is_active=eq.true"
                }.onEach { action ->
                    val dto = runCatching {
                        json.decodeFromString<LiveAlertDto>(action.record.toString())
                    }.getOrNull() ?: return@onEach
                    _liveAlerts.value = listOf(dto) + _liveAlerts.value
                }.launchIn(this)
                
                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "live_alerts"
                }.onEach { action ->
                    val dto = runCatching {
                        json.decodeFromString<LiveAlertDto>(action.record.toString())
                    }.getOrNull() ?: return@onEach
                    _liveAlerts.value = _liveAlerts.value.map {
                        if (it.id == dto.id) dto else it
                    }.filter { it.isActive }
                }.launchIn(this)

                channel.subscribe()
            }
        }
    }

    suspend fun fetchWeather() {
        runCatching {
            val response = apiService.getCurrentWeather(apiKey = config.openWeatherApiKey)
            _weather.value = WeatherInfo(
                temp = response.main.temp,
                condition = response.weather.firstOrNull()?.main ?: "Unknown",
                humidity = response.main.humidity,
                city = response.name
            )
        }
    }

    /**
     * Fetches air quality from WAQI API using the provided token.
     */
    suspend fun fetchAqi() {
        runCatching {
            val response = apiService.getAirQuality(token = config.waqiToken)
            val aqiValue = response.data.aqi
            _aqi.value = AqiInfo(
                aqi    = aqiValue,
                status = when {
                    aqiValue <= 50  -> "अच्छा"
                    aqiValue <= 100 -> "संतोषजनक"
                    aqiValue <= 150 -> "संवेदनशीलों के लिए अस्वस्थ"
                    aqiValue <= 200 -> "अस्वस्थ"
                    aqiValue <= 300 -> "बहुत अस्वस्थ"
                    else            -> "खतरनाक"
                }
            )
        }
    }

    suspend fun fetchNews() {
        runCatching {
            val response = apiService.getNews(apiKey = config.gNewsApiKey)
            _news.value = response.articles.map {
                NewsItem(
                    title = it.title,
                    description = it.description,
                    url = it.url,
                    image = it.image,
                    source = it.source.name,
                    publishedAt = it.publishedAt
                )
            }
        }
    }

    // ── Live Map logic ──────────────────────────────────────────────────

    suspend fun markersForMode(mode: MapMode, lat: Double? = null, lng: Double? = null): List<MapMarker> {
        val data = assets.loadAppData()
        // Use provided lat/lng or default to Bhopal centre
        val targetLat = lat ?: com.nagarsetu.backend.core.CivicConstants.BHOPAL_LAT
        val targetLng = lng ?: com.nagarsetu.backend.core.CivicConstants.BHOPAL_LNG

        return when (mode) {
            MapMode.EMERGENCY   -> policeMarkers(data)
            MapMode.CIVIC       -> allMarkersCombined(data, targetLat, targetLng)
            MapMode.HOSPITALS   -> hospitalMarkersLive(targetLat, targetLng)
            MapMode.EV_CHARGING -> evMarkersLive(targetLat, targetLng)
            MapMode.TRAFFIC     -> riskHotspotMarkers()
            else                -> emptyList()
        }
    }

    private fun riskHotspotMarkers(): List<MapMarker> {
        val hotspots = mutableListOf<MapMarker>()
        
        // Colors from requirement: Green -> Low, Yellow -> Moderate, Orange -> High, Red -> Severe
        val colorLow = android.graphics.Color.argb(100, 76, 175, 80)      // Green
        val colorMod = android.graphics.Color.argb(120, 255, 235, 59)     // Yellow
        val colorHigh = android.graphics.Color.argb(140, 255, 152, 0)     // Orange
        val colorSevere = android.graphics.Color.argb(160, 244, 67, 54)   // Red

        // MP Nagar - High Traffic
        hotspots += MapMarker(
            23.2324, 77.4294, 
            "MP Nagar", 
            "High Traffic Risk\nAccident Probability: 78%\nLast Updated: 5 mins ago",
            colorHigh, 400f
        )
        
        // New Market - Moderate Congestion
        hotspots += MapMarker(
            23.2325, 77.4045,
            "New Market",
            "Moderate Congestion\nActive shoppers: High\nLast Updated: 10 mins ago",
            colorMod, 300f
        )
        
        // Lalghati - Severe Accident Prone
        hotspots += MapMarker(
            23.2842, 77.3694,
            "Lalghati Junction",
            "Severe Accident Spot\nHistory: 12 incidents this month\nCaution: High Speed Zone",
            colorSevere, 500f
        )
        
        // Airport Road - Moderate Speed Risk
        hotspots += MapMarker(
            23.2870, 77.3400,
            "Airport Road",
            "Moderate Speed Risk\nVisibility: Good\nLast Updated: 15 mins ago",
            colorMod, 600f
        )
        
        // Kolar Road - Pollution Hotspot
        hotspots += MapMarker(
            23.1800, 77.4100,
            "Kolar Road",
            "High Pollution Level\nAQI: 210 (Poor)\nHealth Warning: Wear Mask",
            colorHigh, 450f
        )
        
        // Bittan Market - Low Risk
        hotspots += MapMarker(
            23.2180, 77.4300,
            "Bittan Market",
            "Low Risk Area\nNormal activity detected\nLast Updated: 2 mins ago",
            colorLow, 250f
        )
        
        // Hoshangabad Road - Severe Traffic
        hotspots += MapMarker(
            23.1900, 77.4500,
            "Hoshangabad Road",
            "Severe Traffic Congestion\nBottleneck at Board Office Square\nETA Delay: +25 mins",
            colorSevere, 700f
        )
        
        // ISBT Area - Crime/Safety Watch
        hotspots += MapMarker(
            23.2450, 77.4550,
            "ISBT Terminal",
            "Safety Watch Area\nHigh Footfall Detection\nSecurity: Patrolling Active",
            colorMod, 350f
        )

        return hotspots
    }

    private suspend fun allMarkersCombined(data: com.google.gson.JsonObject, lat: Double, lng: Double): List<MapMarker> {
        return policeMarkers(data) + hospitalMarkersLive(lat, lng) + evMarkersLive(lat, lng)
    }

    private fun policeMarkers(data: com.google.gson.JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        data.getAsJsonObject("emergencyAI")?.getAsJsonArray("policeStations")?.forEach { p ->
            val o = p.asJsonObject
            list += MapMarker(
                o["latitude"].asDouble, 
                o["longitude"].asDouble,
                "🚔 ${o["name"].asString}", 
                "Police Station"
            )
        }
        return list
    }

    private suspend fun hospitalMarkersLive(lat: Double, lng: Double): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        
        runCatching {
            // Use Overpass API for live hospitals, but fallback to seed if needed
            val q = OverpassQueries.hospitals(lat, lng)
            osmApi.query(q).elements.forEach {
                // Ensure the hospital is within a reasonable distance to avoid showing Indore hospitals
                if (distanceMeters(lat, lng, it.lat, it.lon) < 25000) {
                    list += MapMarker(it.lat, it.lon, "🏥 ${it.tags["name"] ?: "Hospital"}", "Hospital")
                }
            }
        }
        
        if (list.isEmpty()) {
            val data = assets.loadAppData()
            data.getAsJsonObject("healthWatch")?.getAsJsonArray("hospitals")?.forEach { h ->
                val o = h.asJsonObject
                list += MapMarker(
                    o["latitude"].asDouble,
                    o["longitude"].asDouble,
                    "🏥 ${o["name"].asString}",
                    "Hospital"
                )
            }
        }
        return list.distinctBy { it.title }
    }

    private suspend fun evMarkersLive(lat: Double, lng: Double): List<MapMarker> {
        val list = mutableListOf<MapMarker>()

        // 1. Add requested Bhopal EV stations as reliable seed
        val bhopalEvs = listOf(
            Triple("Ola Hypercharger - MP Nagar", 23.2380, 77.4620),
            Triple("Tata Power - New Market", 23.2325, 77.4045),
            Triple("EV Station - Airport Road", 23.2870, 77.3400),
            Triple("Charging Point - Hoshangabad Road", 23.2000, 77.4500),
            Triple("Kolar EV Hub", 23.1800, 77.4100),
            Triple("Lalghati Charging Station", 23.2800, 77.3700),
            Triple("Bittan Market EV Plaza", 23.2180, 77.4300)
        )
        
        bhopalEvs.forEach { (name, eLat, eLng) ->
            list += MapMarker(eLat, eLng, "⚡ $name", "EV Station")
        }

        // 2. Fetch live data but strictly filter for Bhopal region
        runCatching {
            val res = evApi.getChargingStations(
                apiKey = config.openChargeMapKey,
                lat = com.nagarsetu.backend.core.CivicConstants.BHOPAL_LAT,
                lng = com.nagarsetu.backend.core.CivicConstants.BHOPAL_LNG,
                distance = 15.0 // Tighter radius to stay in Bhopal
            )
            res.forEach {
                val eLat = it.AddressInfo.Latitude
                val eLng = it.AddressInfo.Longitude
                // Double check it's near Bhopal centre (max 20km) to prevent Indore markers
                if (distanceMeters(com.nagarsetu.backend.core.CivicConstants.BHOPAL_LAT, 
                        com.nagarsetu.backend.core.CivicConstants.BHOPAL_LNG, eLat, eLng) < 20000) {
                    list += MapMarker(eLat, eLng, "⚡ ${it.AddressInfo.Title}", "EV Station")
                }
            }
        }
        return list.distinctBy { it.title }
    }

    /** Haversine distance in metres. */
    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toInt()
    }

    private fun parkingMarkers(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        data.getAsJsonArray("parkEase")?.forEach { p ->
            val o = p.asJsonObject
            list += MapMarker(
                o["latitude"].asDouble,
                o["longitude"].asDouble,
                "🅿️ ${o["name"].asString}",
                "Parking"
            )
        }
        return list
    }

    private fun hospitalMarkers(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        data.getAsJsonObject("healthWatch")?.getAsJsonArray("hospitals")?.forEach { h ->
            val o = h.asJsonObject
            list += MapMarker(
                o["latitude"].asDouble,
                o["longitude"].asDouble,
                "🏥 ${o["name"].asString}",
                "Hospital"
            )
        }
        return list
    }

    private suspend fun evMarkersLive(): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        runCatching {
            val res = evApi.getChargingStations(config.openChargeMapKey)
            res.take(15).forEach {
                list += MapMarker(
                    it.AddressInfo.Latitude,
                    it.AddressInfo.Longitude,
                    "⚡ ${it.AddressInfo.Title}",
                    "EV Station"
                )
            }
        }
        return list
    }

    private fun trafficMarkers(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        // Mocking traffic incidents since TomTom is removed
        list += MapMarker(23.230, 77.410, "🚦 Slow Traffic: MP Nagar", "Traffic")
        list += MapMarker(23.245, 77.425, "🚦 Road Work: Hoshangabad Rd", "Traffic")
        list += MapMarker(23.255, 77.450, "🚦 Congestion: Bittan Market", "Traffic")
        return list
    }

    private fun emergencyMarkers(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()
        data.getAsJsonObject("healthWatch")?.getAsJsonArray("hospitals")?.forEach { h ->
            val o = h.asJsonObject
            list += MapMarker(o["latitude"].asDouble, o["longitude"].asDouble,
                "🏥 ${o["name"].asString}", "Hospital")
        }
        data.getAsJsonObject("emergencyAI")?.getAsJsonArray("policeStations")?.forEach { p ->
            val o = p.asJsonObject
            list += MapMarker(o["latitude"].asDouble, o["longitude"].asDouble,
                "🚔 ${o["name"].asString}", "Police")
        }
        list += MapMarker(23.238, 77.440, "🚒 Fire Station Shahpura", "Fire")
        list += MapMarker(23.252, 77.418, "🚒 Fire Station TT Nagar", "Fire")
        return list
    }

    private suspend fun civicMarkersLive(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()

        // Live EV stations from Open Charge Map (free, no key required)
        runCatching {
            val res = evApi.getChargingStations(config.openChargeMapKey)
            res.take(6).forEach {
                list += MapMarker(
                    it.AddressInfo.Latitude,
                    it.AddressInfo.Longitude,
                    "⚡ ${it.AddressInfo.Title}",
                    "EV Station"
                )
            }
        }

        // Parking from seed (OSM live parking is in ParkEaseScreen via ParkingRepositoryImpl)
        if (list.size < 3) {
            data.getAsJsonArray("parkEase")?.forEach { p ->
                val o = p.asJsonObject
                list += MapMarker(
                    o["latitude"].asDouble,
                    o["longitude"].asDouble,
                    "🅿️ ${o["name"].asString}",
                    "Parking"
                )
            }
        }
        return list
    }

    private suspend fun roadWatchMarkersLive(data: JsonObject): List<MapMarker> {
        val list = mutableListOf<MapMarker>()

        // Accident blackspots from local predictive seed (no paid traffic API)
        data.getAsJsonObject("predictiveHazards")?.getAsJsonArray("accidentBlackspots")?.forEach { b ->
            val o = b.asJsonObject
            list += MapMarker(
                o["latitude"].asDouble,
                o["longitude"].asDouble,
                "⚠️ ${o["name"].asString}",
                "जोखिम ${o["riskScore"].asInt}"
            )
        }
        // Live pothole/road reports from Supabase (real user reports)
        runCatching {
            supabase.postgrest["live_alerts"]
                .select { filter { eq("type", "ROAD") } }
                .decodeList<LiveAlertDto>()
                .take(5)
                .forEach { alert ->
                    if (alert.lat != null && alert.lng != null)
                        list += MapMarker(alert.lat, alert.lng, "🕳️ ${alert.title}", "सड़क")
                }
        }
        return list
    }

    private fun authorityMarkers(data: JsonObject): List<MapMarker> =
        data.getAsJsonArray("wards")?.map { w ->
            val o = w.asJsonObject
            MapMarker(o["latitude"].asDouble, o["longitude"].asDouble,
                "🏛️ ${o["name"].asString}", o["authorityName"].asString)
        } ?: emptyList()

    fun allWards() = wardRepo.allWards()
    fun sortedWards(field: WardSortField, ascending: Boolean) =
        wardRepo.sortedBy(wardRepo.allWards(), field, ascending)

    fun citizenComplaints(): List<CitizenComplaint> = listOf(
        CitizenComplaint("c1", "Pothole", "Large pothole near Arera Colony gate 4",
            ComplaintStatus.IN_PROGRESS, "Arera Colony", System.currentTimeMillis() - 86_400_000),
        CitizenComplaint("c2", "Streetlight", "5 lights out on MP Nagar main road",
            ComplaintStatus.PENDING, "MP Nagar", System.currentTimeMillis() - 43_200_000),
        CitizenComplaint("c3", "Water Supply", "No water for 2 days in Govindpura sector",
            ComplaintStatus.RESOLVED, "Govindpura", System.currentTimeMillis() - 172_800_000,
            System.currentTimeMillis() - 21_600_000),
        CitizenComplaint("c4", "Garbage", "Overflowing bins near bus stand",
            ComplaintStatus.PENDING, "TT Nagar", System.currentTimeMillis() - 10_800_000)
    )

    fun budgetTransparency(): List<BudgetTransparencyItem> = listOf(
        BudgetTransparencyItem("Road Infrastructure", 45_000_000, 38_500_000, 12),
        BudgetTransparencyItem("Water & Sanitation",  32_000_000, 28_200_000,  8),
        BudgetTransparencyItem("Street Lighting",     12_000_000,  9_800_000,  6),
        BudgetTransparencyItem("Public Parks",         8_500_000,  6_200_000,  4),
        BudgetTransparencyItem("Waste Management",    15_000_000, 13_100_000,  5)
    )

    fun pushAlert(alert: DashboardAlert) {
        val updated = (listOf(alert) + _alerts.value).take(20)
        _alerts.value = updated
        crisisManager.updateFromAlerts(updated)
    }

    fun applyCrisisDeepLink(crisis: String?) = crisisManager.applyCrisisDeepLink(crisis)

    private fun seedAlerts() = listOf(
        DashboardAlert("1","Water Supply","Low pressure in Arera Colony",
            AlertType.CIVIC,"Arera Colony",System.currentTimeMillis() - 300_000,AlertSeverity.INFO),
        DashboardAlert("2","Traffic Snarl","Congestion on Hoshangabad Road",
            AlertType.TRAFFIC,"MP Nagar",System.currentTimeMillis() - 120_000,AlertSeverity.WARNING),
        DashboardAlert("3","SLA Breach","Chunar Ganj ward exceeded SLA for 5 complaints",
            AlertType.SLA_BREACH,"Chunar Ganj",System.currentTimeMillis() - 60_000,AlertSeverity.CRITICAL),
        DashboardAlert("4","Pothole Alert","New report near Shahpura roundabout",
            AlertType.HAZARD,"Shahpura",System.currentTimeMillis() - 900_000,AlertSeverity.INFO)
    )
}
