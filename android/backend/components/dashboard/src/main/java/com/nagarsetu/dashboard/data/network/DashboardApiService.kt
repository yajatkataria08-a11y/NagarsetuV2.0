package com.nagarsetu.dashboard.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Dashboard network endpoints.
 *
 *  1. OpenWeatherMap  – current weather (free tier, key required)
 *  2. OpenAQ v2       – real-time AQI for Bhopal (completely free, no key)
 *  3. GNews           – civic news feed (free tier, key required)
 */
interface DashboardApiService {

    // ── 1. Weather ────────────────────────────────────────────────────────
    @GET("https://api.openweathermap.org/data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q")     city: String = "Bhopal,IN",
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    // ── 2. Air Quality – WAQI (World Air Quality Index) ──────────────────
    @GET("https://api.waqi.info/feed/bhopal/")
    suspend fun getAirQuality(
        @Query("token") token: String
    ): WaqiResponse

    // ── 3. News ───────────────────────────────────────────────────────────
    @GET("https://gnews.io/api/v4/search")
    suspend fun getNews(
        @Query("q")      query: String = "Bhopal civic",
        @Query("apikey") apiKey: String,
        @Query("lang")   lang: String  = "hi",
        @Query("max")    max: Int      = 5
    ): NewsResponse
}

// ── Weather models ────────────────────────────────────────────────────────────
data class WeatherResponse(val main: WeatherMain, val weather: List<WeatherDescription>, val name: String)
data class WeatherMain(val temp: Double, val humidity: Int)
data class WeatherDescription(val main: String, val description: String)

// ── WAQI models ──────────────────────────────────────────────────────────────
data class WaqiResponse(val status: String, val data: WaqiData)
data class WaqiData(val aqi: Int, val city: WaqiCity)
data class WaqiCity(val name: String)

// ── News models ───────────────────────────────────────────────────────────────
data class NewsResponse(val articles: List<NewsArticle> = emptyList())
data class NewsArticle(
    val title: String,
    val description: String,
    val url: String,
    val image: String? = null,
    val publishedAt: String,
    val source: NewsSource
)
data class NewsSource(val name: String)
