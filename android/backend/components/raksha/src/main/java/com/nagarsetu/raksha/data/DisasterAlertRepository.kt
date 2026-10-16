package com.nagarsetu.raksha.data

import com.nagarsetu.raksha.data.network.NdmaApiService
import com.nagarsetu.raksha.domain.model.DisasterAlert
import javax.inject.Inject
import javax.inject.Singleton

interface DisasterAlertRepository {
    suspend fun getActiveAlerts(): List<DisasterAlert>
}

/**
 * Fetches live disaster alerts from NDMA SACHET CAP feed.
 * Automatically falls back to Hindi-localised Bhopal seed data
 * if the network is unavailable.
 */
@Singleton
class DisasterAlertRepositoryImpl @Inject constructor(
    private val ndmaService: NdmaApiService
) : DisasterAlertRepository {

    override suspend fun getActiveAlerts(): List<DisasterAlert> =
        ndmaService.fetchActiveAlerts()
}
