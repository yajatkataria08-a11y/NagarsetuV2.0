package com.nagarsetu.parkease.data

import com.nagarsetu.backend.core.data.CivicDataHub
import com.nagarsetu.parkease.data.network.OsmOverpassApiService
import com.nagarsetu.parkease.data.network.OverpassQueries
import com.nagarsetu.parkease.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parking data from two sources (merged, deduped):
 *  1. Local CivicDataHub seed (always available, instant)
 *  2. OSM Overpass API   (live, no key, free)
 *
 * Traffic / flow data is no longer fetched here (TomTom removed).
 * OSRM routing in GreenRoute gives relative congestion signals instead.
 */
@Singleton
class ParkingRepositoryImpl @Inject constructor(
    private val hub: CivicDataHub,
    private val supabase: SupabaseClient,
    private val osmApi: OsmOverpassApiService   // ← was TomTomApiService
) : ParkingRepository {

    private val table get() = supabase.postgrest["parking_bookings"]

    override suspend fun getLots(lat: Double, lng: Double): List<ParkingLot> =
        withContext(Dispatchers.IO) {

            // ── 1. Seed lots from local asset JSON ─────────────────────────
            val localLots = hub.parkingLots().map { el ->
                val o         = el.asJsonObject
                val total     = o["totalSlots"].asInt
                val available = o["availableSlots"].asInt
                val hourly    = o["pricing"].asJsonObject["hourly"].asInt
                ParkingLot(
                    id            = o["id"].asString,
                    name          = o["name"].asString,
                    latitude      = o["latitude"].asDouble,
                    longitude     = o["longitude"].asDouble,
                    totalSlots    = total,
                    occupiedSlots = total - available,
                    ratePerHour   = hourly.toDouble()
                )
            }

            // ── 2. Live lots from OSM Overpass ─────────────────────────────
            val osmLots = runCatching {
                val query    = OverpassQueries.parking(lat, lng, radiusM = 2500, limit = 20)
                val response = osmApi.query(query)

                response.elements.mapNotNull { el ->
                    val elLat = if (el.lat != 0.0) el.lat else el.tags["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
                    val elLon = if (el.lon != 0.0) el.lon else el.tags["lon"]?.toDoubleOrNull() ?: return@mapNotNull null
                    val name  = el.tags["name"]
                        ?: el.tags["operator"]
                        ?: "पार्किंग (OSM ${el.id})"
                    val cap   = el.tags["capacity"]?.toIntOrNull() ?: 50
                    val fee   = el.tags["fee"]

                    ParkingLot(
                        id            = "osm_${el.id}",
                        name          = name,
                        latitude      = elLat,
                        longitude     = elLon,
                        totalSlots    = cap,
                        occupiedSlots = (cap * (0.3 + Math.random() * 0.6)).toInt(),   // Random but realistic occupancy 30-90%
                        ratePerHour   = if (fee == "no" || fee == null) 0.0 else 20.0
                    )
                }
            }.getOrDefault(emptyList())

            (localLots + osmLots)
                .distinctBy { "${it.latitude.toBits()},${it.longitude.toBits()}" }
                .sortedBy { lot ->
                    com.nagarsetu.core.utils.LocationUtils
                        .haversineMeters(lat, lng, lot.latitude, lot.longitude)
                }
        }

    override suspend fun holdSlot(lotId: String): String =
        "PARK-${lotId.takeLast(4)}-${System.currentTimeMillis() % 10000}"

    override suspend fun bookSlot(
        uid: String,
        lotId: String,
        lotName: String,
        slotNumber: Int,
        durationHours: Int
    ): Result<ParkingBooking> = withContext(Dispatchers.IO) {
        runCatching {
            val bookingId = UUID.randomUUID().toString()
            val qrData    = "NAGARSETU:PKG:$bookingId"
            val startTime = Instant.now()
            val expiresAt = startTime.plus(durationHours.toLong(), ChronoUnit.HOURS)

            table.insert(buildJsonObject {
                put("id",          bookingId)
                put("uid",         uid)
                put("lot_id",      lotId)
                put("lot_name",    lotName)
                put("slot_number", slotNumber)
                put("qr_data",     qrData)
                put("expires_at",  expiresAt.toString())
                put("status",      "ACTIVE")
            })

            ParkingBooking(
                id             = bookingId,
                lotId          = lotId,
                slotNumber     = slotNumber,
                startTime      = startTime.toEpochMilli(),
                holdExpiryTime = expiresAt.toEpochMilli(),
                status         = BookingStatus.ACTIVE,
                qrData         = qrData
            )
        }
    }

    override suspend fun getUserBookings(uid: String): List<ParkingBooking> =
        withContext(Dispatchers.IO) {
            runCatching {
                table.select {
                    filter { eq("uid", uid) }
                }.decodeList<ParkingBookingDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                table.update(buildJsonObject { put("status", "CANCELLED") }) {
                    filter { eq("id", bookingId) }
                }
                Unit
            }
        }
}

@Serializable
private data class ParkingBookingDto(
    val id: String,
    @SerialName("lot_id")      val lotId: String,
    @SerialName("slot_number") val slotNumber: Int,
    @SerialName("qr_data")     val qrData: String,
    @SerialName("expires_at")  val expiresAt: String,
    val status: String
) {
    fun toDomain() = ParkingBooking(
        id             = id,
        lotId          = lotId,
        slotNumber     = slotNumber,
        startTime      = 0L,
        holdExpiryTime = runCatching {
            Instant.parse(expiresAt).toEpochMilli()
        }.getOrDefault(0L),
        status         = runCatching { BookingStatus.valueOf(status) }.getOrDefault(BookingStatus.ACTIVE),
        qrData         = qrData
    )
}
