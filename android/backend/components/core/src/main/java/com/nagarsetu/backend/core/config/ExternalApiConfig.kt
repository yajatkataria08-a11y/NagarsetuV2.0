package com.nagarsetu.backend.core.config

/**
 * Configuration for third-party APIs.
 *
 * FREE / NO-KEY APIs now integrated:
 *   - OpenAQ          → air quality (no key)
 *   - Open Charge Map → EV stations (key optional for basic)
 *   - OSM Overpass    → parking, hospitals (no key)
 *   - OSRM            → routing / transit (no key)
 *   - NDMA SACHET     → disaster CAP alerts (no key)
 *
 * KEYED APIs (free tiers):
 *   - OpenWeatherMap  → weather
 *   - data.gov.in     → epidemic / health data
 *   - GNews           → civic news feed
 */
data class ExternalApiConfig(
    val openWeatherApiKey: String,        // OpenWeatherMap free tier
    val dataGovInKey: String,             // data.gov.in open government data
    val openChargeMapKey: String = "",    // Open Charge Map – empty = anonymous (rate-limited but works)
    val gNewsApiKey: String,              // GNews free tier
    val waqiToken: String                 // World Air Quality Index project
)
