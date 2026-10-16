package com.nagarsetu.raksha.data.incident

import android.content.Context
import android.util.Log
import com.nagarsetu.raksha.data.routing.LatLonPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A predicted risk cell returned by the backend ML model.
 * @param location centre of the grid cell
 * @param score    normalised risk score in [0, 1]
 */
data class RiskCell(val location: LatLonPoint, val score: Double)

/**
 * Pulls grid risk predictions from the backend ML model (`/api/risk-grid`).
 *
 * Ported from Raksha (com.safepath.indore.data.RiskApiRepository) and
 * rewritten as a Hilt-injectable coroutine repository.
 *
 * Endpoint:
 *   GET /api/risk-grid?minLat=&maxLat=&minLng=&maxLng=&steps=&hour=&day=
 *   Response: { "cells": [ { "lat": Double, "lng": Double, "score": Double }, ... ] }
 */
@Singleton
class RiskApiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "RiskApiRepository"
    }

    private fun baseUrl(): String {
        val prefs = context.getSharedPreferences("nagarsetu_prefs", Context.MODE_PRIVATE)
        val override = prefs.getString("raksha_api_endpoint", null)
        return if (!override.isNullOrBlank()) {
            if (override.startsWith("http")) override else "http://$override"
        } else {
            "http://10.197.165.59:3000"
        }.removeSuffix("/")
    }

    /**
     * Fetches an ML-predicted risk grid for the given bounding box.
     *
     * @param minLat   south bound
     * @param maxLat   north bound
     * @param minLng   west bound
     * @param maxLng   east bound
     * @param steps    grid resolution (default 20 × 20 cells)
     * @param hour     hour of day override (null = current hour)
     * @param day      Python weekday override Mon=0..Sun=6 (null = current day)
     */
    suspend fun fetchGrid(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        steps: Int = 20,
        hour: Int? = null,
        day: Int? = null
    ): List<RiskCell> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val h = hour ?: cal.get(Calendar.HOUR_OF_DAY)
        // Calendar.DAY_OF_WEEK: SUN=1..SAT=7
        // Python weekday(): Mon=0..Sun=6
        val pyDay = day ?: ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)

        val urlStr = "${baseUrl()}/api/risk-grid" +
            "?minLat=$minLat&maxLat=$maxLat&minLng=$minLng&maxLng=$maxLng" +
            "&steps=$steps&hour=$h&day=$pyDay"

        Log.d(TAG, "Fetching risk grid: $urlStr")

        val request = Request.Builder().url(urlStr).get().build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchGrid HTTP ${response.code}")
                    return@withContext emptyList()
                }
                parseGrid(response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            Log.e(TAG, "fetchGrid network error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches a single-point ML risk score for a GPS position.
     *
     * @param lat   latitude
     * @param lng   longitude
     * @param hour  hour override (null = current)
     * @param day   Python weekday override Mon=0..Sun=6 (null = current)
     * @return 0–100 risk score, or null if server returns 503 (model training)
     */
    suspend fun fetchRiskScore(
        lat:  Double,
        lng:  Double,
        hour: Int? = null,
        day:  Int? = null
    ): Double? = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val h = hour ?: cal.get(Calendar.HOUR_OF_DAY)
        val pyDay = day ?: ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)

        val urlStr = "${baseUrl()}/api/risk?lat=$lat&lng=$lng&hour=$h&day=$pyDay"
        Log.d(TAG, "Fetching point risk: $urlStr")

        val request = Request.Builder().url(urlStr).get().build()

        try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 503 -> {
                        Log.i(TAG, "fetchRiskScore: model training (503)")
                        null
                    }
                    !response.isSuccessful -> {
                        Log.e(TAG, "fetchRiskScore HTTP ${response.code}")
                        null
                    }
                    else -> {
                        val body = response.body?.string().orEmpty()
                        if (body.isBlank()) return@withContext null
                        JSONObject(body).optDouble("score", 0.0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchRiskScore network/parse error: ${e.message}", e)
            null
        }
    }

    private fun parseGrid(raw: String): List<RiskCell> {
        if (raw.isBlank()) return emptyList()
        return try {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("cells") ?: return emptyList()
            (0 until arr.length()).map { i ->
                val c = arr.getJSONObject(i)
                RiskCell(
                    location = LatLonPoint(c.getDouble("lat"), c.getDouble("lng")),
                    score    = c.getDouble("score")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseGrid failed: ${e.message}", e)
            emptyList()
        }
    }
}
