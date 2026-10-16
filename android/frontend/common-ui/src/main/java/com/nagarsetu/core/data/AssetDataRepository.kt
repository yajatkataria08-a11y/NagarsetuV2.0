package com.nagarsetu.core.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nagarsetu.core.utils.WardPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cachedAppData: JsonObject? = null
    private var cachedSeed: JsonArray? = null
    private var cachedGeo: JsonObject? = null

    fun loadAppData(): JsonObject {
        return try {
            cachedAppData?.let { return it }
            val json = context.assets.open("app_data.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, JsonObject::class.java).also { cachedAppData = it }
        } catch (e: Exception) {
            Log.e("AssetDataRepository", "Failed to load app_data.json", e)
            JsonObject()
        }
    }

    fun loadSeedReports(): JsonArray {
        return try {
            cachedSeed?.let { return it }
            val json = context.assets.open("seed.json").bufferedReader().use { it.readText() }
            val element = gson.fromJson(json, com.google.gson.JsonElement::class.java)
            if (element.isJsonArray) {
                element.asJsonArray.also { cachedSeed = it }
            } else {
                val obj = element.asJsonObject
                (obj.getAsJsonArray("reports") ?: JsonArray()).also { cachedSeed = it }
            }
        } catch (e: Exception) {
            Log.e("AssetDataRepository", "Failed to load seed.json", e)
            JsonArray()
        }
    }

    fun loadGeoJson(): JsonObject {
        return try {
            cachedGeo?.let { return it }
            val json = context.assets.open("geo.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, JsonObject::class.java).also { cachedGeo = it }
        } catch (e: Exception) {
            Log.e("AssetDataRepository", "Failed to load geo.json", e)
            JsonObject()
        }
    }

    fun wards(): List<WardPoint> {
        return try {
            val arr = loadAppData().getAsJsonArray("wards") ?: return emptyList()
            arr.map { w ->
                WardPoint(
                    name = w.asJsonObject["name"].asString,
                    latitude = w.asJsonObject["latitude"].asDouble,
                    longitude = w.asJsonObject["longitude"].asDouble,
                    authorityName = w.asJsonObject["authorityName"].asString
                )
            }
        } catch (e: Exception) {
            Log.e("AssetDataRepository", "Failed to parse wards", e)
            emptyList()
        }
    }
}
