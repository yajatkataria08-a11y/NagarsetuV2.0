package com.nagarsetu.greenroute.data.network

/**
 * Routing is now handled by [OsrmApiService].
 *
 * This interface is kept for backward-compat; GtfsRepositoryImpl
 * uses OsrmApiService directly.
 */
typealias GreenRouteApiService = OsrmApiService
