package com.nagarsetu.greenroute.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * OSRM (Open Source Routing Machine) – completely free & open, no key.
 *
 * Public demo server: https://router.project-osrm.org
 * (For production, self-host with Bhopal/India OSM extract.)
 *
 * Driving profile used; adapt to "cycling" or "foot" for other modes.
 */
interface OsrmApiService {

    /**
     * Get a route between two coordinates.
     *
     * @param profile  "driving", "cycling", or "foot"
     * @param coords   "lng1,lat1;lng2,lat2"  (OSRM is lng-first!)
     */
    @GET("route/v1/{profile}/{coords}")
    suspend fun getRoute(
        @Path("profile")  profile: String = "driving",
        @Path("coords")   coords: String,
        @Query("overview") overview: String = "false",
        @Query("steps")    steps: Boolean   = false
    ): OsrmRouteResponse
}

// ── Response models ───────────────────────────────────────────────────────────
data class OsrmRouteResponse(
    val code: String           = "Ok",
    val routes: List<OsrmRoute> = emptyList()
)

data class OsrmRoute(
    val duration: Double = 0.0,  // seconds
    val distance: Double = 0.0   // metres
)

/** Returns the first route, or null when the server returns no routes. */
fun OsrmRouteResponse.firstRoute(): OsrmRoute? = routes.firstOrNull()
