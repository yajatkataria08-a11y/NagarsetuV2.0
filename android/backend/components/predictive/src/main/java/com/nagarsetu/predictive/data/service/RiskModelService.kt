package com.nagarsetu.predictive.data.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

enum class RiskLevel { LOW, MODERATE, HIGH, SEVERE }

data class RiskPrediction(
    val wardId: String,
    val date: LocalDate,
    val accidentRisk: Float,
    val floodRisk: Float,
    val crimeRisk: Float,
    val healthRisk: Float,
    val overallRisk: Float,
    val overallLevel: RiskLevel,
    val source: String = "model"
)

data class SevenDayForecast(
    val wardId: String,
    val predictions: List<RiskPrediction>
)

@Singleton
class RiskModelService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "RiskModelService"

        // OpenWeatherMap free API — no key needed for current weather endpoint
        // Replace YOUR_OWM_KEY with key from local.properties if available
        private const val OWM_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=Bhopal,IN&appid=%s&units=metric"

        // Bhopal ward centroids for geo-based flood weighting
        // Lower elevation wards (near Upper Lake / Bhopal Lake) = higher flood risk
        private val WARD_FLOOD_ELEVATION_FACTOR = mapOf(
            "W01" to 1.0f,  // Berasia — elevated terrain
            "W02" to 0.9f,
            "W03" to 1.3f,  // Chunar Ganj — low, near drainage
            "W04" to 0.8f,  // Ayodhya Nagar — elevated
            "W05" to 1.2f,  // Govindpura — industrial, poor drainage
            "W06" to 1.4f,  // Indrapuri — near Upper Lake
            "W07" to 0.7f,  // Arera Colony — well-drained, elevated
            "W08" to 1.0f,  // TT Nagar
            "W09" to 0.9f,  // MP Nagar — good drainage
            "W10" to 1.1f,  // Shahpura — moderate
            "W11" to 1.0f,  // Bairagarh
            "W12" to 1.2f,  // Karond — low-lying
            "W13" to 1.5f   // Misrod — lowest elevation, worst drainage
        )

        // Hour-of-day accident risk curve (sigmoid-shaped, peaks at rush hours)
        private val HOUR_ACCIDENT_WEIGHT = FloatArray(24) { h ->
            when (h) {
                in 7..10  -> 1.4f  // morning rush
                in 17..21 -> 1.6f  // evening rush — highest
                in 22..23 -> 1.3f  // late night — drunk driving
                in 0..4   -> 1.2f  // early morning
                else      -> 0.9f  // off-peak
            }
        }

        // Default model weights (overridden by risk_weights.json from training script)
        private val DEFAULT_WEIGHTS = mapOf(
            "infrastructure" to 0.30f,
            "weather"        to 0.25f,
            "temporal"       to 0.20f,
            "historical"     to 0.15f,
            "civic_pressure" to 0.10f
        )
    }

    // ── Data loaded once at startup ───────────────────────────────────────────

    private val seedRiskScores: Map<String, FloatArray> by lazy { loadSeedRiskScores() }
    private val appData: JsonObject by lazy { loadAppData() }
    private val modelWeights: Map<String, Float> by lazy { loadModelWeights() }

    // Derived features from app_data.json (computed once)
    private val wardCivicPressure: Map<String, Float> by lazy { computeWardCivicPressure() }
    private val reportDensityByWard: Map<String, Float> by lazy { computeReportDensity() }

    // Live weather cache (refreshed every 30 min max)
    @Volatile private var weatherCache: WeatherSnapshot? = null
    @Volatile private var weatherFetchTime: Long = 0L

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun getSevenDayForecast(wardId: String): SevenDayForecast =
        withContext(Dispatchers.Default) {
            val weather = fetchWeatherSafe()
            val today = LocalDate.now()
            val hour = LocalTime.now().hour
            SevenDayForecast(
                wardId = wardId,
                predictions = (0..6).map { offset ->
                    computePrediction(wardId, today.plusDays(offset.toLong()), offset, hour, weather)
                }
            )
        }

    suspend fun getAllWardRisks(): Map<String, RiskPrediction> =
        withContext(Dispatchers.Default) {
            val weather = fetchWeatherSafe()
            val today = LocalDate.now()
            val hour = LocalTime.now().hour
            (1..13).associate { i ->
                val id = "W${i.toString().padStart(2, '0')}"
                id to computePrediction(id, today, 0, hour, weather)
            }
        }

    // ── Core Multi-Signal Scoring ─────────────────────────────────────────────

    private fun computePrediction(
        wardId: String,
        date: LocalDate,
        dayOffset: Int,
        hour: Int,
        weather: WeatherSnapshot?
    ): RiskPrediction {
        val base = seedRiskScores[wardId] ?: floatArrayOf(0.35f, 0.20f, 0.40f, 0.25f)
        val w = modelWeights

        // ── Signal 1: Infrastructure stress from citizen reports ──────────
        // Counts POTHOLE, ROAD_DAMAGE, POOR_LIGHTING reports near this ward
        val infraStress = reportDensityByWard[wardId] ?: 0.3f

        // ── Signal 2: Live weather impact ─────────────────────────────────
        val rainMm = weather?.rainMm ?: estimateSeasonalRain(date)
        val tempC   = weather?.tempC  ?: 25f
        val weatherFloodSignal  = (rainMm / 50f).coerceIn(0f, 1f)    // 50mm = severe
        val weatherHealthSignal = when {
            tempC > 38f -> 0.7f   // heat stress
            tempC < 10f -> 0.5f   // cold health risk
            rainMm > 20f -> 0.6f  // dengue breeding season
            else -> 0.2f
        }

        // ── Signal 3: Temporal patterns ───────────────────────────────────
        val dow = date.dayOfWeek
        val hourWeight = if (dayOffset == 0) HOUR_ACCIDENT_WEIGHT[hour] else 1.0f
        val dowAccidentFactor = when (dow) {
            DayOfWeek.FRIDAY   -> 1.5f
            DayOfWeek.SATURDAY -> 1.6f
            DayOfWeek.SUNDAY   -> 1.3f
            DayOfWeek.MONDAY   -> 1.1f
            else -> 1.0f
        }
        val temporalAccident = hourWeight * dowAccidentFactor
        val temporalCrime = when (dow) {
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> 1.4f
            DayOfWeek.SUNDAY -> 1.2f
            else -> 1.0f
        } * if (dayOffset == 0 && hour in 20..23) 1.3f else 1.0f

        // ── Signal 4: Historical base (seed.json prior) ───────────────────
        val histAccident = base[0]
        val histFlood    = base[1]
        val histCrime    = base[2]
        val histHealth   = base[3]

        // ── Signal 5: Civic pressure (SLA breach ratio from app_data.json) ─
        val civicPressure = wardCivicPressure[wardId] ?: 0.3f

        // ── Flood elevation factor for this ward ──────────────────────────
        val elevFactor = WARD_FLOOD_ELEVATION_FACTOR[wardId] ?: 1.0f

        // ── Combine signals per risk dimension ────────────────────────────
        val accidentRisk = (
            w["infrastructure"]!! * infraStress * 1.2f +
            w["temporal"]!!       * (temporalAccident / 2f) +
            w["historical"]!!     * histAccident +
            w["civic_pressure"]!! * civicPressure
        ).coerceIn(0f, 1f)

        val floodRisk = (
            w["weather"]!!        * weatherFloodSignal * 1.5f +
            w["historical"]!!     * histFlood * elevFactor +
            w["civic_pressure"]!! * civicPressure * 0.5f +
            w["infrastructure"]!! * infraStress * 0.3f
        ).coerceIn(0f, 1f)

        val crimeRisk = (
            w["temporal"]!!       * (temporalCrime / 2f) +
            w["historical"]!!     * histCrime +
            w["infrastructure"]!! * infraStress * 0.5f +   // poor lighting → crime
            w["civic_pressure"]!! * civicPressure * 0.4f
        ).coerceIn(0f, 1f)

        val healthRisk = (
            w["weather"]!!        * weatherHealthSignal * 1.2f +
            w["historical"]!!     * histHealth +
            w["civic_pressure"]!! * civicPressure * 0.6f +
            w["infrastructure"]!! * infraStress * 0.2f
        ).coerceIn(0f, 1f)

        // Future uncertainty decay (more uncertain further out)
        val decay = 1f - (dayOffset * 0.04f).coerceIn(0f, 0.24f)

        val overall = ((accidentRisk + floodRisk + crimeRisk + healthRisk) / 4f) * decay

        return RiskPrediction(
            wardId = wardId, date = date,
            accidentRisk = accidentRisk * decay,
            floodRisk = floodRisk * decay,
            crimeRisk = crimeRisk * decay,
            healthRisk = healthRisk * decay,
            overallRisk = overall,
            overallLevel = overall.toRiskLevel(),
            source = if (weather != null) "live+seed" else "seed"
        )
    }

    // ── Derived Feature Computation ───────────────────────────────────────────

    /**
     * Maps ward complaint/SLA breach ratio → civic pressure score 0..1
     * Source: app_data.json → wards array
     */
    private fun computeWardCivicPressure(): Map<String, Float> {
        return try {
            val wards = appData.getAsJsonArray("wards") ?: return emptyMap()
            wards.associate { el ->
                val o = el.asJsonObject
                val wardId = o["id"].asString
                val complaints = o["complaintCount"].asInt.toFloat()
                val resolved   = o["resolvedCount"].asInt.toFloat()
                val slaBreaches = o["slaBreaches"].asInt.toFloat()
                // Unresolved ratio + SLA severity → 0..1 pressure
                val unresolved = ((complaints - resolved) / complaints.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val slaSeverity = (slaBreaches / 50f).coerceIn(0f, 1f)  // 50+ = max
                wardId to ((unresolved * 0.6f) + (slaSeverity * 0.4f)).coerceIn(0f, 1f)
            }
        } catch (e: Exception) { emptyMap() }
    }

    /**
     * Counts infrastructure-related reports per ward from seed.json.
     * Types: POTHOLE, ROAD_DAMAGE, POOR_LIGHTING, BROKEN_SIGNAL, WATER_LOGGING
     */
    private fun computeReportDensity(): Map<String, Float> {
        return try {
            val reports = try {
                val json = context.assets.open("seed.json").bufferedReader().use { it.readText() }
                gson.fromJson(json, JsonObject::class.java)
                    .getAsJsonArray("reports") ?: return emptyMap()
            } catch (e: Exception) { return emptyMap() }

            val infraTypes = setOf("POTHOLE","ROAD_DAMAGE","POOR_LIGHTING","BROKEN_SIGNAL","WATER_LOGGING")
            val wardCounts = mutableMapOf<String, Int>()

            reports.forEach { el ->
                val o = el.asJsonObject
                val type = o["type"]?.asString ?: return@forEach
                if (type in infraTypes) {
                    val lat = o["latitude"]?.asDouble ?: return@forEach
                    val lng = o["longitude"]?.asDouble ?: return@forEach
                    val wardId = approximateWardFromCoords(lat, lng)
                    wardCounts[wardId] = (wardCounts[wardId] ?: 0) + 1
                }
            }

            val maxCount = wardCounts.values.maxOrNull()?.toFloat() ?: 1f
            wardCounts.mapValues { (_, count) -> count.toFloat() / maxCount }
        } catch (e: Exception) { emptyMap() }
    }

    /**
     * Very simple ward approximation from lat/lng using Bhopal ward grid.
     * In production: replace with PostGIS ward boundary lookup.
     */
    private fun approximateWardFromCoords(lat: Double, lng: Double): String {
        // 13 wards roughly gridded across Bhopal
        val wardIndex = ((lat - 23.18) / 0.02).toInt().coerceIn(0, 12)
        return "W${(wardIndex + 1).toString().padStart(2, '0')}"
    }

    // ── Live Weather via OpenWeatherMap (free, no key needed for current) ──────

    private suspend fun fetchWeatherSafe(): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (weatherCache != null && (now - weatherFetchTime) < 30 * 60 * 1000) {
                return@withContext weatherCache
            }
            try {
                // Free OWM endpoint — works without API key for basic current weather
                val url = "https://wttr.in/Bhopal?format=j1"
                val req = Request.Builder().url(url)
                    .header("User-Agent", "NagarSetu/1.0 (nagarsetu.bhopal@gmail.com)")
                    .build()
                val body = http.newCall(req).execute().use { it.body?.string() ?: return@withContext null }
                val json = JSONObject(body)
                val current = json.getJSONArray("current_condition").getJSONObject(0)
                val rainMm = current.optString("precipMM", "0").toFloatOrNull() ?: 0f
                val tempC  = current.optString("temp_C", "25").toFloatOrNull() ?: 25f
                val snap = WeatherSnapshot(rainMm = rainMm, tempC = tempC)
                weatherCache = snap
                weatherFetchTime = now
                snap
            } catch (e: Exception) {
                Log.w(TAG, "Weather fetch failed, using seasonal estimate: ${e.message}")
                null
            }
        }

    /**
     * wttr.in is completely free, no API key needed, returns JSON.
     * Fallback: estimate rain from month (Bhopal monsoon pattern).
     */
    private fun estimateSeasonalRain(date: LocalDate): Float = when (date.monthValue) {
        6 -> 8f; 7 -> 25f; 8 -> 20f; 9 -> 12f  // monsoon peak
        10 -> 3f; 11 -> 1f; 12 -> 0f
        1 -> 0f; 2 -> 1f; 3 -> 1f; 4 -> 2f; 5 -> 5f
        else -> 0f
    }

    // ── Model Weights Loading ─────────────────────────────────────────────────

    /**
     * Load trained weights from risk_weights.json (generated by Python training script).
     * Falls back to DEFAULT_WEIGHTS if file not found.
     *
     * risk_weights.json format:
     * {
     *   "infrastructure": 0.32,
     *   "weather": 0.28,
     *   "temporal": 0.18,
     *   "historical": 0.14,
     *   "civic_pressure": 0.08
     * }
     */
    private fun loadModelWeights(): Map<String, Float> {
        return try {
            val json = context.assets.open("risk_weights.json").bufferedReader().use { it.readText() }
            val obj = gson.fromJson(json, JsonObject::class.java)
            obj.entrySet().associate { (k, v) -> k to v.asFloat }
                .also { Log.i(TAG, "Loaded trained model weights: $it") }
        } catch (e: Exception) {
            Log.i(TAG, "risk_weights.json not found, using defaults")
            DEFAULT_WEIGHTS
        }
    }

    // ── Seed data loading (unchanged) ─────────────────────────────────────────

    private fun loadSeedRiskScores(): Map<String, FloatArray> {
        return try {
            val json = context.assets.open("seed.json").bufferedReader().use { it.readText() }
            val obj = gson.fromJson(json, JsonObject::class.java)
            val scores = obj.getAsJsonObject("riskScores") ?: obj
            scores.entrySet().associate { (wardId, el) ->
                val ward = el.asJsonObject
                wardId to floatArrayOf(
                    ward.get("accident")?.asFloat ?: 0.35f,
                    ward.get("flood")?.asFloat    ?: 0.20f,
                    ward.get("crime")?.asFloat    ?: 0.40f,
                    ward.get("health")?.asFloat   ?: 0.25f
                )
            }
        } catch (e: Exception) { buildDefaultSeedScores() }
    }

    private fun loadAppData(): JsonObject {
        return try {
            val json = context.assets.open("app_data.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, JsonObject::class.java)
        } catch (e: Exception) { JsonObject() }
    }

    private fun buildDefaultSeedScores(): Map<String, FloatArray> = mapOf(
        "W01" to floatArrayOf(0.38f, 0.22f, 0.35f, 0.28f),
        "W02" to floatArrayOf(0.35f, 0.20f, 0.32f, 0.25f),
        "W03" to floatArrayOf(0.55f, 0.30f, 0.48f, 0.35f),
        "W04" to floatArrayOf(0.28f, 0.18f, 0.25f, 0.22f),
        "W05" to floatArrayOf(0.48f, 0.25f, 0.42f, 0.30f),
        "W06" to floatArrayOf(0.45f, 0.28f, 0.40f, 0.32f),
        "W07" to floatArrayOf(0.22f, 0.15f, 0.20f, 0.18f),
        "W08" to floatArrayOf(0.38f, 0.22f, 0.35f, 0.28f),
        "W09" to floatArrayOf(0.30f, 0.18f, 0.28f, 0.22f),
        "W10" to floatArrayOf(0.42f, 0.25f, 0.38f, 0.30f),
        "W11" to floatArrayOf(0.32f, 0.20f, 0.30f, 0.25f),
        "W12" to floatArrayOf(0.40f, 0.28f, 0.38f, 0.32f),
        "W13" to floatArrayOf(0.55f, 0.35f, 0.50f, 0.40f)
    )

    private fun Float.toRiskLevel() = when {
        this < 0.25f -> RiskLevel.LOW
        this < 0.50f -> RiskLevel.MODERATE
        this < 0.75f -> RiskLevel.HIGH
        else         -> RiskLevel.SEVERE
    }

    // Keep existing compatibility methods unchanged
    fun computeWeeklyForecast(): List<com.nagarsetu.predictive.domain.model.Forecast> {
        val today = LocalDate.now()
        return (0..6).flatMap { offset ->
            val pred = try {
                computePrediction("W08", today.plusDays(offset.toLong()), offset,
                    LocalTime.now().hour, weatherCache)
            } catch (e: Exception) { getDefaultPrediction("W08", today.plusDays(offset.toLong())) }

            listOf(
                makeForecast(com.nagarsetu.predictive.domain.model.PredictionType.ACCIDENT,
                    pred.date.dayOfWeek.name.take(3), pred.accidentRisk, pred.overallLevel),
                makeForecast(com.nagarsetu.predictive.domain.model.PredictionType.FLOOD,
                    pred.date.dayOfWeek.name.take(3), pred.floodRisk, pred.overallLevel),
                makeForecast(com.nagarsetu.predictive.domain.model.PredictionType.CRIME,
                    pred.date.dayOfWeek.name.take(3), pred.crimeRisk, pred.overallLevel),
                makeForecast(com.nagarsetu.predictive.domain.model.PredictionType.HEALTH,
                    pred.date.dayOfWeek.name.take(3), pred.healthRisk, pred.overallLevel)
            )
        }
    }

    fun computeRiskGrid(): List<com.nagarsetu.predictive.domain.model.RiskGridCell> {
        val today = LocalDate.now(); val hour = LocalTime.now().hour
        return (1..13).map { i ->
            val id = "W${i.toString().padStart(2, '0')}"
            val pred = try {
                computePrediction(id, today, 0, hour, weatherCache)
            } catch (e: Exception) { getDefaultPrediction(id, today) }
            com.nagarsetu.predictive.domain.model.RiskGridCell(
                id = "cell_$id",
                centerLat = 23.18 + (i * 0.012),
                centerLng = 77.29 + (i * 0.018),
                riskScore = (pred.overallRisk * 100).toInt(),
                dominantType = com.nagarsetu.predictive.domain.model.PredictionType.ACCIDENT
            )
        }
    }

    fun hazardPredictions() = emptyList<com.nagarsetu.predictive.domain.model.HazardPrediction>()
    fun proactiveAlerts() = emptyList<com.nagarsetu.predictive.domain.model.ProactiveAlert>()

    private fun getDefaultPrediction(wardId: String, date: LocalDate) = RiskPrediction(
        wardId = wardId, date = date,
        accidentRisk = 0.35f, floodRisk = 0.20f, crimeRisk = 0.40f, healthRisk = 0.28f,
        overallRisk = 0.31f, overallLevel = RiskLevel.MODERATE, source = "default"
    )

    private fun makeForecast(
        type: com.nagarsetu.predictive.domain.model.PredictionType,
        day: String, prob: Float,
        level: RiskLevel
    ) = com.nagarsetu.predictive.domain.model.Forecast(type, day, prob, level.toDomainRiskLevel())

    private fun RiskLevel.toDomainRiskLevel() = when (this) {
        RiskLevel.LOW      -> com.nagarsetu.predictive.domain.model.RiskLevel.LOW
        RiskLevel.MODERATE -> com.nagarsetu.predictive.domain.model.RiskLevel.MODERATE
        RiskLevel.HIGH     -> com.nagarsetu.predictive.domain.model.RiskLevel.HIGH
        RiskLevel.SEVERE   -> com.nagarsetu.predictive.domain.model.RiskLevel.SEVERE
    }

    private data class WeatherSnapshot(val rainMm: Float, val tempC: Float)
}
