package com.nagarsetu.greenroute.data

import com.nagarsetu.greenroute.domain.model.RouteOption
import com.nagarsetu.greenroute.domain.model.TransitStation

interface GtfsRepository {
    suspend fun getNearbyArrivals(lat: Double, lng: Double): List<TransitStation>
    suspend fun routeOptions(destination: String): List<RouteOption>
}
