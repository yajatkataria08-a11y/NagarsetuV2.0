package com.nagarsetu.roadwatch.data

import android.content.Context
import android.graphics.Bitmap
import com.nagarsetu.core.data.local.dao.RoadReportDao
import com.nagarsetu.core.data.local.entity.RoadReportEntity
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.roadwatch.data.tflite.PotholeDetector
import com.nagarsetu.roadwatch.domain.model.*
import com.nagarsetu.roadwatch.domain.usecase.CalculateSlaUseCase
import com.nagarsetu.roadwatch.domain.usecase.ClusterReportsUseCase
import com.nagarsetu.roadwatch.domain.usecase.NearbyReportsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// ── Supabase DTO ──────────────────────────────────────────────────────────────
@Serializable
data class RoadReportDto(
    val id: String,
    @SerialName("ticket_id")          val ticketId: String,
    val uid: String,
    @SerialName("report_type")        val reportType: String,
    val severity: String,
    val status: String,
    val description: String = "",
    val lat: Double,
    val lng: Double,
    val ward: String = "",
    @SerialName("ward_number")        val wardNumber: Int? = null,
    @SerialName("photo_url")          val photoUrl: String = "",
    @SerialName("ai_confidence")      val aiConfidence: Double? = null,
    @SerialName("depth_estimate_mm")  val depthEstimateMm: Double? = null,
    @SerialName("verified_by_ai")     val verifiedByAi: Boolean = false,
    val upvotes: Int = 0,
    @SerialName("authority_level")    val authorityLevel: String = "WARD",
    @SerialName("escalation_count")   val escalationCount: Int = 0,
    @SerialName("sla_deadline_at")    val slaDeadlineAt: String? = null,
    @SerialName("created_at")         val createdAt: String? = null
) {
    fun toDomain() = RoadReport(
        id             = id,
        type           = runCatching { ReportType.valueOf(reportType) }.getOrDefault(ReportType.OTHER),
        latitude       = lat,
        longitude      = lng,
        severity       = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.MEDIUM),
        depthMm        = depthEstimateMm?.toFloat(),
        timestamp      = System.currentTimeMillis(),
        status         = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.SUBMITTED),
        slaDeadline    = System.currentTimeMillis() + (48 * 3600 * 1000L),
        description    = description,
        wardId         = wardNumber?.toString(),
        authorityLevel = runCatching { AuthorityLevel.valueOf(authorityLevel) }.getOrDefault(AuthorityLevel.WARD),
        escalationCount = escalationCount,
        verifiedByAi   = verifiedByAi,
        upvotes        = upvotes,
        imageUri       = photoUrl.ifBlank { null }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class RoadWatchRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val supabase: SupabaseClient,
    private val roadReportDao: RoadReportDao,
    private val userRepository: SupabaseUserRepository,
    private val slaUseCase: CalculateSlaUseCase,
    private val clusterUseCase: ClusterReportsUseCase,
    private val nearbyUseCase: NearbyReportsUseCase
) {
    private val scope   = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val detector = PotholeDetector(context)
    private val reportTable get() = supabase.postgrest["road_reports"]

    // ── In-memory + reactive stream ───────────────────────────────────────────
    private val _reports = MutableStateFlow<List<RoadReport>>(emptyList())
    val reports: StateFlow<List<RoadReport>> = _reports.asStateFlow()

    init {
        // Load from Room cache immediately (offline-first)
        scope.launch { loadFromRoomCache() }
        // Then fetch fresh from Supabase
        scope.launch { refreshFromSupabase() }
        // Subscribe to real-time inserts from any device
        startRealtimeSubscription()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI Detection
    // ─────────────────────────────────────────────────────────────────────────

    fun detectPothole(bitmap: Bitmap): PotholeDetection = detector.detect(bitmap)

    // ─────────────────────────────────────────────────────────────────────────
    // Submit report (full flow)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full submission pipeline:
     * 1. (Optional) Upload photo to Supabase Storage
     * 2. Insert report row to Supabase Postgrest
     * 3. Save to Room for offline access
     * 4. Log activity
     * 5. Update local StateFlow
     *
     * @return ticket ID on success
     */
    suspend fun submitReport(
        uid: String,
        report: RoadReport,
        photoBitmap: Bitmap? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1 — Upload photo if provided
            val photoUrl = photoBitmap?.let { uploadPhoto(report.id, it) } ?: ""

            val ticketId = generateTicketId()

            // Step 2 — Insert to Supabase
            reportTable.insert(buildJsonObject {
                put("id",               report.id)
                put("ticket_id",        ticketId)
                put("uid",              uid)
                put("report_type",      report.type.name)
                put("severity",         report.severity.name)
                put("status",           "SUBMITTED")
                put("description",      report.description ?: "")
                put("lat",              report.latitude)
                put("lng",              report.longitude)
                put("ward",             report.wardId ?: "")
                put("photo_url",        photoUrl)
                put("verified_by_ai",   report.verifiedByAi)
                report.depthMm?.let { put("depth_estimate_mm", it.toDouble()) }
                put("authority_level",  report.authorityLevel.name)
            })

            // Step 3 — Save to Room (offline fallback)
            roadReportDao.insert(report.toEntity())

            // Step 4 — Optimistic UI update
            _reports.value = _reports.value + report.copy(imageUri = photoUrl)

            // Step 5 — Activity log (fire-and-forget)
            userRepository.logActivity(uid, "REPORT_SUBMIT", report.id, "road_report")

            ticketId
        }
    }

    /**
     * Fetches reports from Supabase filtered by ward, limited to last 200.
     * Falls back to Room cache on failure.
     */
    suspend fun getReportsByWard(wardNumber: Int): List<RoadReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                reportTable
                    .select(Columns.ALL) {
                        filter { eq("ward_number", wardNumber) }
                        order("created_at", Order.DESCENDING)
                        limit(200)
                    }
                    .decodeList<RoadReportDto>()
                    .map { it.toDomain() }
            }.getOrElse {
                roadReportDao.getAll().map { it.toDomain() }
            }
        }

    /**
     * Upvotes a report. Uses the report_upvotes join table to prevent duplicates.
     */
    suspend fun upvoteReport(reportId: String, uid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["report_upvotes"].upsert(buildJsonObject {
                    put("report_id", reportId)
                    put("uid", uid)
                })
                // Optimistic update
                _reports.value = _reports.value.map {
                    if (it.id == reportId) it.copy(upvotes = it.upvotes + 1) else it
                }
            }
        }

    /** Escalates a report to the next authority level in Supabase. */
    suspend fun escalateReport(reportId: String): Result<RoadReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val report = _reports.value.find { it.id == reportId }
                    ?: error("Report $reportId not found in cache")

                val nextLevel = when (report.authorityLevel) {
                    AuthorityLevel.WARD         -> AuthorityLevel.ZONAL
                    AuthorityLevel.ZONAL        -> AuthorityLevel.COMMISSIONER
                    AuthorityLevel.COMMISSIONER -> AuthorityLevel.STATE
                    AuthorityLevel.STATE        -> AuthorityLevel.STATE
                }

                reportTable.update(buildJsonObject {
                    put("authority_level",  nextLevel.name)
                    put("status",           "ESCALATED")
                    put("escalation_count", report.escalationCount + 1)
                }) { filter { eq("id", reportId) } }

                val escalated = report.copy(
                    authorityLevel  = nextLevel,
                    status          = ReportStatus.ESCALATED,
                    escalationCount = report.escalationCount + 1
                )
                _reports.value = _reports.value.map {
                    if (it.id == reportId) escalated else it
                }
                escalated
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Aggregation helpers (delegate to use cases)
    // ─────────────────────────────────────────────────────────────────────────

    fun slaFor(report: RoadReport): SlaInfo = slaUseCase.execute(report)

    fun clusterReports(reports: List<RoadReport>): List<PotholeCluster> =
        clusterUseCase.execute(reports)

    fun nearbyReports(lat: Double, lng: Double, radiusKm: Double = 2.0): List<Pair<RoadReport, Double>> =
        nearbyUseCase.execute(_reports.value, lat, lng, radiusKm)

    fun generateTicketId(): String = "RW-2026-${Random.nextInt(1000, 9999)}"

    // ─────────────────────────────────────────────────────────────────────────
    // Real-time subscription
    // ─────────────────────────────────────────────────────────────────────────

    private fun startRealtimeSubscription() {
        scope.launch {
            runCatching {
                val channel = supabase.realtime.channel("road_reports_live")
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "road_reports"
                }.onEach { action ->
                    // New report inserted by anyone — update the live feed
                    runCatching {
                        val dto = kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                        }.decodeFromString<RoadReportDto>(action.record.toString())
                        val domain = dto.toDomain()
                        if (_reports.value.none { it.id == domain.id }) {
                            _reports.value = listOf(domain) + _reports.value
                        }
                    }
                }.launchIn(this)
                
                channel.subscribe()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun refreshFromSupabase() {
        runCatching {
            val dtos = reportTable
                .select(Columns.ALL) {
                    order("created_at", Order.DESCENDING)
                    limit(300)
                }
                .decodeList<RoadReportDto>()
            val domainList = dtos.map { it.toDomain() }
            _reports.value = domainList
            // Sync to Room
            roadReportDao.insertAll(domainList.map { it.toEntity() })
        }
    }

    private fun loadFromRoomCache() {
        scope.launch {
            val cached = roadReportDao.getAll()
            if (cached.isNotEmpty()) {
                _reports.value = cached.map { it.toDomain() }
            }
        }
    }

    /**
     * Uploads bitmap to Supabase Storage bucket `road-report-photos`.
     * Returns the public URL string.
     */
    private suspend fun uploadPhoto(reportId: String, bitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
        val path = "reports/$reportId.jpg"
        val bucket = supabase.storage["road-report-photos"]
        bucket.upload(path, bytes, upsert = true)
        return bucket.publicUrl(path)
    }

    // ── Entity mappers ────────────────────────────────────────────────────────

    private fun RoadReport.toEntity() = RoadReportEntity(
        id          = id,
        type        = type.name,
        lat         = latitude,
        lng         = longitude,
        severity    = severity.name,
        status      = status.name,
        ticketId    = "RW-${id.take(8)}",
        description = description ?: ""
    )

    private fun RoadReportEntity.toDomain() = RoadReport(
        id          = id,
        type        = runCatching { ReportType.valueOf(type) }.getOrDefault(ReportType.OTHER),
        latitude    = lat,
        longitude   = lng,
        severity    = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.MEDIUM),
        depthMm     = null,
        timestamp   = System.currentTimeMillis(),
        status      = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.SUBMITTED),
        slaDeadline = System.currentTimeMillis() + 48 * 3600 * 1000L,
        description = description
    )
}
