package com.nagarsetu.core.data

import android.util.Log
import com.nagarsetu.core.ui.map.RouteInfo
import com.nagarsetu.core.ui.map.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getRoute(
        originLat: Double, originLng: Double,
        destLat: Double,   destLng: Double
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        var url = ""
        try {
            // OSRM URL format: /route/v1/driving/{lng},{lat};{lng},{lat}
            // NOTE: OSRM takes LONGITUDE first, then LATITUDE
            url = "https://router.project-osrm.org/route/v1/driving/" +
                "$originLng,$originLat;$destLng,$destLat" +
                "?overview=full&geometries=geojson&steps=true"

            Log.d("RouteRepository", "Calling OSRM: $url")

            val request = Request.Builder().url(url).build()
            val responseBody = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("RouteRepository", "OSRM HTTP error: ${response.code}")
                    return@withContext Result.failure(
                        Exception("OSRM HTTP ${response.code}")
                    )
                }
                response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty OSRM response")
                )
            }

            Log.d("RouteRepository", "OSRM response received (length: ${responseBody.length})")

            val json = JSONObject(responseBody)
            val code = json.getString("code")
            if (code != "Ok") {
                Log.e("RouteRepository", "OSRM returned code: $code")
                return@withContext Result.failure(Exception("OSRM code=$code"))
            }

            val route = json.getJSONArray("routes").getJSONObject(0)
            val distanceM = route.getDouble("distance")
            val durationS = route.getDouble("duration")

            // Parse steps
            val steps = mutableListOf<RouteStep>()
            var nextTurnInstruction = ""
            var nextTurnDistanceText = ""
            try {
                val legs = route.getJSONArray("legs")
                for (legIdx in 0 until legs.length()) {
                    val legSteps = legs.getJSONObject(legIdx).getJSONArray("steps")
                    for (i in 0 until legSteps.length()) {
                        val step = legSteps.getJSONObject(i)
                        val stepDist = step.getDouble("distance")
                        val maneuver = step.getJSONObject("maneuver")
                        val mType     = maneuver.getString("type")
                        val mModifier = if (maneuver.has("modifier")) maneuver.getString("modifier") else ""
                        val loc       = maneuver.getJSONArray("location")
                        // OSRM location = [lng, lat]
                        val stepStart = GeoPoint(loc.getDouble(1), loc.getDouble(0))

                        val instruction = buildInstruction(mType, mModifier, step)
                        val distText = formatDistance(stepDist)

                        if (legIdx == 0 && i == 0) {
                            nextTurnInstruction = instruction
                            nextTurnDistanceText = distText
                        }

                        steps += RouteStep(
                            instruction    = instruction,
                            distanceMeters = stepDist,
                            distanceText   = distText,
                            maneuverType   = mType,
                            maneuverModifier = mModifier,
                            startPoint     = stepStart
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("RouteRepository", "Step parsing failed: ${e.message}")
            }

            // Decode GeoJSON geometry
            // OSRM returns coordinates as [[lng, lat], [lng, lat], ...]
            val coords = route
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            val points = (0 until coords.length()).map { i ->
                val pair = coords.getJSONArray(i)
                // GeoJSON = [longitude, latitude] -> osmdroid GeoPoint(lat, lng)
                GeoPoint(pair.getDouble(1), pair.getDouble(0))
            }

            Log.d("RouteRepository", "Route decoded: ${points.size} points, " +
                "${formatDistance(distanceM)}, ${formatDuration(durationS)}")

            Result.success(
                RouteInfo(
                    polylinePoints = points,
                    distanceMeters = distanceM,
                    durationSeconds = durationS,
                    distanceText = formatDistance(distanceM),
                    etaText = formatDuration(durationS),
                    nextTurnInstruction = nextTurnInstruction,
                    nextTurnDistanceText = nextTurnDistanceText,
                    steps = steps
                )
            )
        } catch (e: Exception) {
            Log.e("RouteRepository", "OSRM fetch failed: ${e.message}", e)
            Log.e("RouteRepository", "URL was: $url")
            Result.failure(e)
        }
    }

    private fun buildInstruction(type: String, modifier: String, stepJson: JSONObject): String {
        // Use OSRM's own ref/name when available for street name
        val streetName = runCatching {
            val name = stepJson.getString("name")
            if (name.isNotBlank() && name != "null") " onto $name" else ""
        }.getOrDefault("")

        return when (type) {
            "depart"     -> "Head ${modifier.ifBlank { "forward" }}$streetName"
            "arrive"     -> "You have arrived at your destination"
            "turn"       -> when (modifier) {
                "left"         -> "Turn left$streetName"
                "right"        -> "Turn right$streetName"
                "slight left"  -> "Keep left$streetName"
                "slight right" -> "Keep right$streetName"
                "sharp left"   -> "Take a sharp left$streetName"
                "sharp right"  -> "Take a sharp right$streetName"
                "straight"     -> "Continue straight$streetName"
                else           -> "Turn $modifier$streetName"
            }
            "new name"   -> "Continue$streetName"
            "merge"      -> "Merge ${modifier.ifBlank { "" }}$streetName"
            "on ramp"    -> "Take the ramp${modifier.let { if(it.isNotBlank()) " on the $it" else "" }}$streetName"
            "off ramp"   -> "Take the exit${modifier.let { if(it.isNotBlank()) " on the $it" else "" }}$streetName"
            "fork"       -> "Keep ${modifier.ifBlank { "straight" }} at the fork$streetName"
            "end of road"-> "Turn ${modifier.ifBlank { "" }} at the end of the road$streetName"
            "roundabout", "rotary" -> "Enter the roundabout$streetName"
            "roundabout turn"      -> "At the roundabout, turn $modifier$streetName"
            "continue"   -> "Continue$streetName"
            "notification" -> "Continue$streetName"
            else         -> "${type.replaceFirstChar { it.uppercase() }}$streetName"
        }
    }

    private fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.toInt()} m"
        else "${"%.1f".format(meters / 1000)} km"

    private fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        return if (mins < 60) "~$mins min" else "~${mins / 60}h ${mins % 60}m"
    }
}
