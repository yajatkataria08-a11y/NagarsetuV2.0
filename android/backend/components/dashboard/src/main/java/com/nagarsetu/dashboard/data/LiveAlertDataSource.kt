package com.nagarsetu.dashboard.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source that streams live city alerts.
 *
 * Production: connect via socket.io-client to `wss://api.nagarsetu.in/alerts`.
 * Currently exposes a [MutableSharedFlow] that DashboardViewModel seeds from
 * app_data.json so the UI always has data.
 *
 * Improvements over v1:
 *  - **Reconnection logic**: [connect] launches a supervisor coroutine that
 *    retries the socket connection with exponential back-off (up to [MAX_BACKOFF_MS])
 *    when the connection drops. In mock mode this is a no-op guard.
 *  - [disconnect] cancels the reconnection job so it does not leak.
 *  - [isConnected] is a thread-safe property backed by [kotlin.concurrent.Volatile].
 *  - [push] is safe to call from any dispatcher; the SharedFlow handles back-pressure.
 */
interface LiveAlertDataSource {
    fun connect(serverUrl: String = LiveAlertDataSourceImpl.SERVER_URL)
    fun disconnect()
    val isConnected: Boolean
}

@Singleton
class LiveAlertDataSourceImpl @Inject constructor() : LiveAlertDataSource {

    companion object {
        private const val TAG = "LiveAlertDataSource"
        const val SERVER_URL = "wss://api.nagarsetu.in/alerts"

        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS     = 30_000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    @Volatile
    override var isConnected: Boolean = false
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    // ── Connect / disconnect ──────────────────────────────────────────────────

    override fun connect(serverUrl: String) {
        if (isConnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch { connectWithRetry(serverUrl) }
    }

    override fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        isConnected = false
        Log.d(TAG, "disconnect()")
    }

    // ── Reconnect loop ────────────────────────────────────────────────────────

    private suspend fun connectWithRetry(serverUrl: String) {
        var backoffMs = INITIAL_BACKOFF_MS
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            try {
                doConnect(serverUrl)
                backoffMs = INITIAL_BACKOFF_MS   // reset on success
            } catch (e: Exception) {
                isConnected = false
                Log.w(TAG, "Connection failed (retry in ${backoffMs}ms): ${e.message}")
                delay(backoffMs)
                backoffMs = (backoffMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    /**
     * Establishes a single connection attempt.
     *
     * Production implementation:
     * ```
     * val socket = IO.socket(serverUrl)
     * socket.on(Socket.EVENT_CONNECT)    { isConnected = true }
     * socket.on(Socket.EVENT_DISCONNECT) { isConnected = false; throw IOException("Disconnected") }
     * socket.on("alert") { args -> scope.launch { _events.emit(args[0].toString()) } }
     * socket.connect()
     * ```
     * Currently in mock mode — DashboardViewModel seeds events from app_data.json.
     */
    private fun doConnect(serverUrl: String) {
        Log.d(TAG, "connect() → $serverUrl (mock mode — seeded from app_data.json)")
        isConnected = true
        // Production: throw IOException on socket error so the retry loop fires
    }

    // ── Push (mock / test helper) ─────────────────────────────────────────────

    /** Emits [payload] to all active collectors. Safe to call from any dispatcher. */
    suspend fun push(payload: String) { _events.emit(payload) }
}
