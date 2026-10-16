package com.nagarsetu.predictive.data

import com.nagarsetu.predictive.data.service.RAGService
import com.nagarsetu.predictive.data.service.RiskModelService
import com.nagarsetu.predictive.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PredictiveRepository — single source of truth for all predictive data.
 * Aggregates RiskModelService + RAGService + BIMSTEC seed data.
 * Offline-first: data is cached in-memory after first load.
 */
@Singleton
class PredictiveRepository @Inject constructor(
    private val riskModel: RiskModelService,
    private val ragService: RAGService
) {
    // ── Lazy in-memory cache (offline-first) ──────────────────────────────────
    private val forecastCache: List<Forecast> by lazy { riskModel.computeWeeklyForecast() }
    private val gridCache: List<RiskGridCell> by lazy { riskModel.computeRiskGrid() }
    private val hazardCache: List<HazardPrediction> by lazy { riskModel.hazardPredictions() }
    private val alertCache: List<ProactiveAlert> by lazy { riskModel.proactiveAlerts() }

    fun weeklyForecast(): List<Forecast> = forecastCache
    fun riskGrid(): List<RiskGridCell> = gridCache
    fun hazardPredictions(): List<HazardPrediction> = hazardCache
    fun proactiveAlerts(): List<ProactiveAlert> = alertCache

    suspend fun queryRag(query: String): RAGResult = ragService.query(query)

    /** Forecasts for a specific day index (0=Mon). */
    fun forecastForDay(dayIndex: Int): List<Forecast> =
        forecastCache.filter { it.day == listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")[dayIndex] }

    /** Top risks for summary card. */
    fun topRisks(): List<Forecast> = forecastCache
        .groupBy { it.type }
        .mapValues { (_, forecasts) -> forecasts.maxByOrNull { it.probability }!! }
        .values
        .sortedByDescending { it.probability }

    fun bimstecCityData(): List<BimstecCityData> = listOf(
        BimstecCityData("Bhopal",     "India",      "🇮🇳", 23, 8,  12, 42, "Live",       "May 2025"),
        BimstecCityData("New Delhi",  "India",      "🇮🇳", 47, 15, 31, 61, "Live",       "May 2025"),
        BimstecCityData("Dhaka",      "Bangladesh", "🇧🇩", 38, 22, 18, 58, "Historical", "Jan 2025"),
        BimstecCityData("Colombo",    "Sri Lanka",  "🇱🇰", 19, 11,  9, 34, "Historical", "Feb 2025"),
        BimstecCityData("Kathmandu",  "Nepal",      "🇳🇵", 28, 14,  7, 48, "Projected",  "Q1 2025"),
        BimstecCityData("Yangon",     "Myanmar",    "🇲🇲", 33, 19, 15, 52, "Projected",  "Q1 2025"),
        BimstecCityData("Thimphu",    "Bhutan",     "🇧🇹",  5,  3,  2, 18, "Projected",  "2024"),
        BimstecCityData("Bangkok",    "Thailand",   "🇹🇭", 41, 17, 22, 45, "Historical", "Mar 2025")
    )
}
