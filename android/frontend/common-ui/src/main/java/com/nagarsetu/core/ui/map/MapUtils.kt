package com.nagarsetu.core.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import androidx.core.content.ContextCompat
import com.nagarsetu.core.utils.LocationUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.json.JSONObject
import android.content.Context

data class MapMarker(
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String = "",
    val heatColor: Int? = null,
    val radius: Float = 0f,
    val iconRes: Int? = null,
    val tintColor: Int? = null
)

// ── PROBLEM 1 FIX: Ward Heatmap Logic ─────────────────────────────────────────

data class WardPolygon(
    val wardId: String,
    val wardName: String,
    val coordinates: List<GeoPoint>,
    val riskScore: Float,
    val riskLevel: RiskLevel
)

enum class RiskLevel { LOW, MODERATE, HIGH, SEVERE }

/**
 * Custom OSMDroid Overlay to draw semi-transparent ward polygons.
 */
class WardHeatmapOverlay(
    private val wardPolygons: List<WardPolygon>
) : Overlay() {

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 70 // High transparency for visibility
    }
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = AndroidColor.parseColor("#444444")
        strokeWidth = 2f
        alpha = 100
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        wardPolygons.forEach { ward ->
            fillPaint.color = ward.riskLevel.toColorInt()
            val path = Path()
            ward.coordinates.forEachIndexed { idx, geoPoint ->
                val pt = projection.toPixels(geoPoint, null)
                if (idx == 0) path.moveTo(pt.x.toFloat(), pt.y.toFloat())
                else path.lineTo(pt.x.toFloat(), pt.y.toFloat())
            }
            path.close()
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun RiskLevel.toColorInt(): Int = when (this) {
        RiskLevel.LOW -> AndroidColor.parseColor("#4FC3F7")
        RiskLevel.MODERATE -> AndroidColor.parseColor("#FFB74D")
        RiskLevel.HIGH -> AndroidColor.parseColor("#EF5350")
        RiskLevel.SEVERE -> AndroidColor.parseColor("#B71C1C")
    }
}

/**
 * Parses geo.json and merges with risk data.
 */
fun loadWardHeatmap(context: Context, wardRiskData: Map<String, Float>): List<WardPolygon> {
    return try {
        val json = context.assets.open("geo.json").bufferedReader().use { it.readText() }
        val featureCollection = JSONObject(json)
        val features = featureCollection.getJSONArray("features")
        
        (0 until features.length()).map { i ->
            val feature = features.getJSONObject(i)
            val props = feature.getJSONObject("properties")
            val geom = feature.getJSONObject("geometry")
            
            // Handle both Polygon and MultiPolygon simply for demo
            val ring = if (geom.getString("type") == "MultiPolygon") {
                geom.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0)
            } else {
                geom.getJSONArray("coordinates").getJSONArray(0)
            }
            
            val points = (0 until ring.length()).map { j ->
                val pt = ring.getJSONArray(j)
                GeoPoint(pt.getDouble(1), pt.getDouble(0)) // Swap [lng, lat] -> [lat, lng]
            }
            
            val wardId = props.optString("ward_id", props.optString("ID", "W$i"))
            val score = wardRiskData[wardId] ?: 0.3f
            
            WardPolygon(
                wardId = wardId,
                wardName = props.optString("name", "Ward $i"),
                coordinates = points,
                riskScore = score,
                riskLevel = when {
                    score < 0.25f -> RiskLevel.LOW
                    score < 0.50f -> RiskLevel.MODERATE
                    score < 0.75f -> RiskLevel.HIGH
                    else -> RiskLevel.SEVERE
                }
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("MapUtils", "Error parsing geo.json", e)
        emptyList()
    }
}

fun MapView.setMarkers(markers: List<MapMarker>, clearExisting: Boolean = true) {
    if (clearExisting) {
        overlays.removeAll { it is Marker || it is org.osmdroid.views.overlay.Polygon }
    }
    markers.forEach { m ->
        if (m.heatColor != null) {
            // ── Realistic Heat Simulation (Concentric Radial Gradients) ────────
            val baseColor = m.heatColor
            val r = m.radius.toDouble().coerceAtLeast(100.0)
            
            // 1. Outer Glow (Large, Very Faint)
            val outer = org.osmdroid.views.overlay.Polygon(this)
            outer.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(GeoPoint(m.lat, m.lng), r * 1.5)
            outer.fillPaint.apply {
                color = baseColor
                alpha = 25 // Very transparent outer ring
                style = Paint.Style.FILL
            }
            outer.outlinePaint.strokeWidth = 0f
            overlays.add(outer)

            // 2. Middle Heat Zone
            val middle = org.osmdroid.views.overlay.Polygon(this)
            middle.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(GeoPoint(m.lat, m.lng), r)
            middle.fillPaint.apply {
                color = baseColor
                alpha = 60 // Medium transparency
                style = Paint.Style.FILL
            }
            middle.outlinePaint.strokeWidth = 0f
            overlays.add(middle)

            // 3. Inner Intense Core
            val inner = org.osmdroid.views.overlay.Polygon(this)
            inner.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(GeoPoint(m.lat, m.lng), r * 0.4)
            inner.fillPaint.apply {
                color = baseColor
                alpha = 130 // Most opaque core
                style = Paint.Style.FILL
            }
            inner.outlinePaint.apply {
                color = baseColor
                strokeWidth = 2f
                alpha = 180
                isAntiAlias = true
            }
            inner.title = m.title
            inner.snippet = m.snippet
            inner.setOnClickListener { polygon, _, _ ->
                polygon.showInfoWindow()
                true
            }
            overlays.add(inner)

            // ── Center Focal Marker ───────────────────────────────────────────
            val focalMarker = Marker(this).apply {
                position = GeoPoint(m.lat, m.lng)
                title = m.title
                snippet = m.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                
                // Use a soft pulse-like dot for the center
                val drawable = ContextCompat.getDrawable(context, com.nagarsetu.core.R.drawable.ic_marker_modern_pin)
                drawable?.let {
                    val wrapped = androidx.core.graphics.drawable.DrawableCompat.wrap(it).mutate()
                    androidx.core.graphics.drawable.DrawableCompat.setTint(wrapped, baseColor)
                    icon = wrapped
                }
                alpha = 0.85f
            }
            overlays.add(focalMarker)

        } else {
            val marker = Marker(this).apply {
                position = GeoPoint(m.lat, m.lng)
                title = m.title
                snippet = m.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                // Determine icon and color
                val finalIconRes = m.iconRes ?: when {
                    m.title.contains("🚔") || m.snippet.contains("Police", true) -> com.nagarsetu.core.R.drawable.ic_marker_police
                    m.title.contains("🏥") || m.snippet.contains("Hospital", true) -> com.nagarsetu.core.R.drawable.ic_marker_hospital
                    m.title.contains("⚡") || m.snippet.contains("EV Station", true) -> com.nagarsetu.core.R.drawable.ic_marker_ev
                    m.title.contains("Destination") || m.snippet.contains("Destination", true) -> com.nagarsetu.core.R.drawable.ic_marker_destination_pin
                    m.snippet.contains("BUS", true) -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
                    m.snippet.contains("CYCLE", true) -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
                    m.snippet.contains("AUTO", true) -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
                    else -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
                }

                val finalTintColor = m.tintColor ?: when {
                    m.snippet.contains("BUS", true) -> android.graphics.Color.parseColor("#4FC3F7")
                    m.snippet.contains("CYCLE", true) -> android.graphics.Color.parseColor("#4CAF50")
                    m.snippet.contains("AUTO", true) -> android.graphics.Color.parseColor("#FFB74D")
                    m.snippet.contains("WALK", true) -> android.graphics.Color.parseColor("#81C784")
                    else -> null
                }

                val drawable = ContextCompat.getDrawable(context, finalIconRes)
                if (drawable != null && finalTintColor != null) {
                    val wrapped = androidx.core.graphics.drawable.DrawableCompat.wrap(drawable).mutate()
                    androidx.core.graphics.drawable.DrawableCompat.setTint(wrapped, finalTintColor)
                    icon = wrapped
                } else {
                    icon = drawable
                }
            }
            overlays.add(marker)
        }
    }
    invalidate()
}

fun distanceMeters(a: GeoPoint, b: GeoPoint): Double =
    LocationUtils.haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)

fun MapView.drawRoute(
    routePoints: List<GeoPoint>,
    userLocation: GeoPoint,
    destination: GeoPoint,
    destinationName: String,
    context: Context,
    iconType: RouteDestinationType = RouteDestinationType.GENERAL
) {
    overlays.clear()

    // ── Route polyline ────────────────────────────────────────────────────
    val polyline = Polyline(this).apply {
        setPoints(routePoints)
        outlinePaint.apply {
            color = android.graphics.Color.parseColor("#1565C0")  // deep blue
            strokeWidth = 12f
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
    }
    overlays.add(polyline)

    // ── User location marker (blue dot) ───────────────────────────────────
    val userMarker = Marker(this).apply {
        position = userLocation
        title = "You are here"
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = ContextCompat.getDrawable(context, com.nagarsetu.core.R.drawable.ic_user_location_modern)
            ?: icon  // fallback to default if icon missing
    }
    overlays.add(userMarker)

    // ── Destination marker (green with lightning bolt for EV) ─────────────
    val destMarker = Marker(this).apply {
        position = destination
        title = destinationName
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        val drawableRes = when (iconType) {
            RouteDestinationType.EV_STATION -> com.nagarsetu.core.R.drawable.ic_marker_ev
            RouteDestinationType.PARKING -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
            RouteDestinationType.HOSPITAL -> com.nagarsetu.core.R.drawable.ic_marker_hospital
            RouteDestinationType.BUS_STOP -> com.nagarsetu.core.R.drawable.ic_marker_modern_pin
            RouteDestinationType.GENERAL -> com.nagarsetu.core.R.drawable.ic_marker_destination_pin
        }
        
        val tintColor = when (iconType) {
            RouteDestinationType.PARKING -> android.graphics.Color.parseColor("#4FC3F7")
            RouteDestinationType.BUS_STOP -> android.graphics.Color.parseColor("#4FC3F7")
            else -> null
        }

        val drawable = ContextCompat.getDrawable(context, drawableRes)
        if (drawable != null && tintColor != null) {
            val wrapped = androidx.core.graphics.drawable.DrawableCompat.wrap(drawable).mutate()
            androidx.core.graphics.drawable.DrawableCompat.setTint(wrapped, tintColor)
            icon = wrapped
        } else {
            icon = drawable ?: icon
        }
    }
    overlays.add(destMarker)

    // ── Zoom to fit both markers ──────────────────────────────────────────
    if (userLocation.latitude != 0.0 && destination.latitude != 0.0) {
        val boundingBox = BoundingBox.fromGeoPoints(listOf(userLocation, destination))
        zoomToBoundingBox(boundingBox.increaseByScale(1.4f), true, 80)
    }

    invalidate()
}
