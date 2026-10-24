package com.nagarsetu.core.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.nagarsetu.core.data.local.dao.PendingQueueDao
import com.nagarsetu.core.data.local.entity.PendingReportEntity
import com.nagarsetu.core.data.local.entity.PendingSosEntity
import com.nagarsetu.core.data.network.IncidentSubmissionDto
import com.nagarsetu.core.data.network.NagarSetuApiService
import com.nagarsetu.core.data.network.SosRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-First Queue & Sync Engine.
 *
 * Automatically monitors device network states and drains pending Room-stored
 * emergency SOS alerts and civic reports as soon as internet connectivity resumes.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingQueueDao: PendingQueueDao,
    private val apiService: NagarSetuApiService
) {
    companion object {
        private const val TAG = "OfflineSyncManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        registerNetworkMonitoring()
    }

    private fun registerNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "Network restored! Initiating offline queue sync.")
                        scope.launch { syncPendingQueues() }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    /**
     * Enqueues an SOS alert to local Room DB when the device is offline.
     */
    suspend fun enqueueSos(
        userId: String,
        userName: String,
        phone: String,
        lat: Double,
        lng: Double,
        ward: String,
        triageLevel: String,
        contact: String,
        batteryLevel: Int,
        heartRate: Int
    ): String {
        val id = "offline_sos_${UUID.randomUUID()}"
        val entity = PendingSosEntity(
            id = id,
            userId = userId,
            userName = userName,
            phone = phone,
            lat = lat,
            lng = lng,
            ward = ward,
            triageLevel = triageLevel,
            contact = contact,
            batteryLevel = batteryLevel,
            heartRate = heartRate
        )
        pendingQueueDao.enqueueSos(entity)
        Log.w(TAG, "Queued offline SOS alert: $id")
        return id
    }

    /**
     * Enqueues a civic report to local Room DB when the device is offline.
     */
    suspend fun enqueueReport(
        type: String,
        description: String,
        lat: Double,
        lng: Double,
        severity: Int,
        reportedBy: String,
        ward: String,
        photoUrl: String = ""
    ): String {
        val id = "offline_rep_${UUID.randomUUID()}"
        val entity = PendingReportEntity(
            id = id,
            type = type,
            description = description,
            lat = lat,
            lng = lng,
            severity = severity,
            reportedBy = reportedBy,
            ward = ward,
            photoUrl = photoUrl
        )
        pendingQueueDao.enqueueReport(entity)
        Log.i(TAG, "Queued offline civic report: $id")
        return id
    }

    /**
     * Drains both pending SOS and report queues by posting each to the backend.
     */
    suspend fun syncPendingQueues() {
        syncPendingSos()
        syncPendingReports()
    }

    private suspend fun syncPendingSos() {
        val pendingSosList = pendingQueueDao.getPendingSosList()
        if (pendingSosList.isEmpty()) return

        Log.i(TAG, "Draining ${pendingSosList.size} queued SOS events...")
        for (item in pendingSosList) {
            try {
                val dto = SosRequestDto(
                    userId = item.userId,
                    userName = item.userName,
                    phone = item.phone,
                    lat = item.lat,
                    lng = item.lng,
                    ward = item.ward,
                    triageLevel = item.triageLevel,
                    batteryLevel = item.batteryLevel,
                    heartRate = item.heartRate,
                    address = item.contact
                )
                val response = apiService.triggerSos(dto)
                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully synced pending SOS #${item.id}")
                    pendingQueueDao.deleteSos(item.id)
                } else {
                    Log.e(TAG, "Failed syncing SOS #${item.id}: HTTP ${response.code()}")
                    pendingQueueDao.updateSosStatus(item.id, "FAILED")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error on SOS #${item.id}: ${e.message}")
                pendingQueueDao.updateSosStatus(item.id, "RETRY_QUEUED")
            }
        }
    }

    private suspend fun syncPendingReports() {
        val pendingReports = pendingQueueDao.getPendingReports()
        if (pendingReports.isEmpty()) return

        Log.i(TAG, "Draining ${pendingReports.size} queued civic reports...")
        for (item in pendingReports) {
            try {
                val dto = IncidentSubmissionDto(
                    type = item.type,
                    description = item.description,
                    lat = item.lat,
                    lng = item.lng,
                    severity = item.severity,
                    reportedBy = item.reportedBy,
                    ward = item.ward
                )
                val response = apiService.submitIncident(dto)
                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully synced pending report #${item.id}")
                    pendingQueueDao.deleteReport(item.id)
                } else {
                    Log.e(TAG, "Failed syncing report #${item.id}: HTTP ${response.code()}")
                    pendingQueueDao.updateReportStatus(item.id, "FAILED")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error on report #${item.id}: ${e.message}")
                pendingQueueDao.updateReportStatus(item.id, "RETRY_QUEUED")
            }
        }
    }
}
