package com.nagarsetu.chargeup.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open Charge Map API v3 – completely free.
 * Pass an empty string for [apiKey] to use anonymous access
 * (rate-limited to ~10 req/min; sufficient for a civic app).
 * Register at https://openchargemap.org/site/develop/api to get a free key
 * and remove the rate limit.
 */
interface ChargingApiService {

    @GET("https://api.openchargemap.io/v3/poi/")
    suspend fun getChargingStations(
        @Query("key")          apiKey: String  = "",        // empty = anonymous
        @Query("latitude")     lat: Double     = 23.2599,   // Bhopal centre
        @Query("longitude")    lng: Double     = 77.4126,
        @Query("distance")     distance: Double = 25.0,
        @Query("distanceunit") unit: String    = "KM",
        @Query("maxresults")   maxResults: Int  = 15,
        @Query("compact")      compact: Boolean = true,
        @Query("verbose")      verbose: Boolean = false
    ): List<OpenChargeMapResponse>
}

data class OpenChargeMapResponse(
    val ID: Int,
    val AddressInfo: AddressInfo,
    val Connections: List<Connection>? = null,
    val StatusType: StatusType? = null
)

data class AddressInfo(
    val Title: String,
    val Latitude: Double,
    val Longitude: Double,
    val AccessComments: String? = null,
    val AddressLine1: String? = null,
    val Town: String? = null
)

data class Connection(
    val ConnectionTypeID: Int,
    val PowerKW: Double? = null,
    val StatusTypeID: Int? = null
)

data class StatusType(
    val IsOperational: Boolean? = null
)
