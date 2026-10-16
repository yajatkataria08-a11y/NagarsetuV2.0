package com.nagarsetu.raksha.data

import android.content.Context
import android.util.Log
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.raksha.data.crime.CrimeDataLoader
import com.nagarsetu.raksha.data.crime.RiskCalculator
import com.nagarsetu.raksha.data.incident.HazardZoneFetcher
import com.nagarsetu.raksha.data.incident.IncidentRepository
import com.nagarsetu.raksha.data.routing.GeoUtils
import com.nagarsetu.raksha.data.routing.LatLonPoint
import com.nagarsetu.raksha.data.routing.RouteGenerator
import com.nagarsetu.raksha.domain.model.DisasterAlert
import com.nagarsetu.raksha.domain.model.DisasterSeverity
import com.nagarsetu.raksha.domain.model.DisasterType
import com.nagarsetu.raksha.domain.model.HazardZone
import com.nagarsetu.raksha.domain.model.SafeRoute
import com.nagarsetu.raksha.domain.model.SafetyContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository for the Raksha safety module.
 *
 * This version is upgraded with Raksha's full backend capabilities:
 *  • Crime-data-based risk scoring ([RiskCalculator])
 *  • Safe route generation ([RouteGenerator] – Fastest / Balanced / Safest)
 *  • Live crowd-sourced incident reports ([IncidentRepository])
 *  • Live hazard zone overlays ([HazardZoneFetcher])
 *  • ML risk-grid API ([com.nagarsetu.raksha.data.incident.RiskApiRepository])
 *
 * All original NagarSetu features (trusted contacts, disaster alerts,
 * women's helplines, safe-zone markers) are preserved unchanged.
 */
@Singleton
class RakshaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assets: AssetDataRepository,
    val incidentRepository: IncidentRepository,
    val hazardZoneFetcher: HazardZoneFetcher
) {
    companion object {
        private const val TAG = "RakshaRepository"
    }

    // ── Risk engine (lazy-initialised on first use) ──────────────────────────

    private val riskCalculator: RiskCalculator by lazy {
        val crimes = CrimeDataLoader.load(context)
        Log.i(TAG, "RiskCalculator initialised with ${crimes.size} crime points")
        RiskCalculator(crimes)
    }

    private val routeGenerator: RouteGenerator by lazy {
        RouteGenerator(riskCalculator)
    }

    /**
     * Refreshes crowd-report data in the risk calculator.
     * Call after [IncidentRepository.getActiveIncidents] returns new data.
     */
    suspend fun syncCrowdReports() {
        val reports = incidentRepository.getActiveIncidents()
        riskCalculator.setCrowdReports(reports)
        Log.d(TAG, "Synced ${reports.size} crowd reports into RiskCalculator")
    }

    // ── Safe routing ─────────────────────────────────────────────────────────

    /**
     * Generates Fastest / Balanced / Safest routes from [origin] to [destination].
     * Fetches live hazard zones before planning to include dynamic overlays.
     *
     * @param stickToMainRoads doubles road penalty on minor roads (outside 500 m endpoints)
     */
    suspend fun generateRoutes(
        origin: LatLonPoint,
        destination: LatLonPoint,
        stickToMainRoads: Boolean = false
    ): List<SafeRoute> = withContext(Dispatchers.Default) {
        val hazards = hazardZoneFetcher.fetchActiveZones()
        routeGenerator.generate(origin, destination, stickToMainRoads, hazards)
    }

    /**
     * Returns the risk score at a single coordinate (0.0 = safe, higher = riskier).
     * Includes both crime-data risk and any active hazard zones.
     */
    suspend fun riskAt(
        lat: Double,
        lng: Double,
        hazards: List<HazardZone> = emptyList()
    ): Double = withContext(Dispatchers.Default) {
        riskCalculator.riskAt(lat, lng, hazards = hazards)
    }

    /**
     * Returns crime/incident heatmap points for map overlay rendering.
     * Each pair is (coordinate, weight).
     */
    fun heatmapPoints(limit: Int = 800): List<Pair<LatLonPoint, Double>> =
        riskCalculator.riskyPoints(limit)

    // ── Existing NagarSetu features (unchanged) ──────────────────────────────

    fun trustedContacts(): List<SafetyContact> = listOf(
        SafetyContact("Mom",             "+919876543210", true),
        SafetyContact("Dad",             "+919876543211", true),
        SafetyContact("Neha (Friend)",   "+919876543212", true)
    )

    fun disasterAlerts(): List<DisasterAlert> {
        val raksha = assets.loadAppData().getAsJsonObject("raksha") ?: return defaultAlerts()
        val alerts = mutableListOf<DisasterAlert>()
        raksha.getAsJsonArray("unsafeZones")?.forEachIndexed { i, el ->
            val o = el.asJsonObject
            alerts += DisasterAlert(
                id       = "unsafe_$i",
                type     = DisasterType.FLOOD,
                severity = when (o["riskLevel"]?.asString) {
                    "HIGH" -> DisasterSeverity.WARNING
                    else   -> DisasterSeverity.WATCH
                },
                message   = "${o["name"].asString}: ${o["reason"]?.asString ?: "Stay alert"}",
                timestamp = System.currentTimeMillis() - i * 3_600_000L
            )
        }
        alerts += DisasterAlert(
            "flood_1", DisasterType.FLOOD, DisasterSeverity.WARNING,
            "Heavy rainfall alert for Kolar & Misrod — avoid low-lying roads.",
            System.currentTimeMillis()
        )
        alerts += DisasterAlert(
            "fire_1", DisasterType.FIRE, DisasterSeverity.EXTREME,
            "Industrial fire drill near Govindpura — follow NDMA advisory.",
            System.currentTimeMillis() - 7_200_000L
        )
        return alerts.ifEmpty { defaultAlerts() }
    }

    fun womenHelplines(): List<SafetyContact> {
        val arr = assets.loadAppData()
            .getAsJsonObject("raksha")?.getAsJsonArray("womenHelplines")
            ?: return emptyList()
        return arr.map { el ->
            val o = el.asJsonObject
            SafetyContact(o["name"].asString, o["phone"].asString, true)
        }
    }

    fun safeZoneMarkers(): List<Triple<Double, Double, String>> {
        val zones = assets.loadAppData()
            .getAsJsonObject("raksha")?.getAsJsonArray("safeZones")
            ?: return emptyList()
        return zones.map { z ->
            val o = z.asJsonObject
            Triple(o["latitude"].asDouble, o["longitude"].asDouble, o["name"].asString)
        }
    }

    private fun defaultAlerts() = listOf(
        DisasterAlert(
            "1", DisasterType.FLOOD, DisasterSeverity.WARNING,
            "Heavy rainfall expected in Bhopal north.", System.currentTimeMillis()
        )
    )
}
