package com.nagarsetu.core.ui.map

import android.graphics.DashPathEffect
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nagarsetu.core.R
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

@Composable
fun OSMRouteMapView(
    modifier: Modifier = Modifier,
    routePoints: List<GeoPoint>,
    userLocation: GeoPoint?,
    destination: GeoPoint?,
    destinationName: String,
    destinationType: RouteDestinationType,
    isNavigating: Boolean = false
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
                controller.setZoom(14.0)
                destination?.let { controller.setCenter(it) }

                // Enable map rotation with device compass/bearing
                val compassOverlay = org.osmdroid.views.overlay.compass.CompassOverlay(
                    ctx, org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider(ctx), this
                ).apply {
                    enableCompass()
                }
                overlays.add(compassOverlay)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Re-add compass overlay as clear() removes all
            val compassOverlay = org.osmdroid.views.overlay.compass.CompassOverlay(
                context, org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider(context), mapView
            ).apply { enableCompass() }
            mapView.overlays.add(compassOverlay)

            // ── Draw route polyline ──────────────────────────────────────────
            // Only draw real polyline if we have > 2 points (road-following)
            if (routePoints.size > 2) {
                // White border under route for contrast
                val polylineBorder = Polyline(mapView).apply {
                    setPoints(routePoints)
                    outlinePaint.apply {
                        color = android.graphics.Color.WHITE
                        strokeWidth = 18f
                        isAntiAlias = true
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                }
                mapView.overlays.add(polylineBorder)

                val polyline = Polyline(mapView).apply {
                    setPoints(routePoints)
                    outlinePaint.apply {
                        color = android.graphics.Color.parseColor("#1565C0")
                        strokeWidth = 14f
                        isAntiAlias = true
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                }
                mapView.overlays.add(polyline)
            } else if (routePoints.size == 2) {
                // Show dashed straight line if OSRM failed but we have points
                val dashedPolyline = Polyline(mapView).apply {
                    setPoints(routePoints)
                    outlinePaint.apply {
                        color = android.graphics.Color.parseColor("#9E9E9E")
                        strokeWidth = 6f
                        pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f)
                    }
                }
                mapView.overlays.add(dashedPolyline)
            }

            // ── User location marker ─────────────────────────────────────────
            userLocation?.let { loc ->
                val marker = Marker(mapView).apply {
                    position = loc
                    title = "You"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                    if (isNavigating && routePoints.size >= 2) {
                        // Use navigation arrow icon
                        icon = ContextCompat.getDrawable(context, com.nagarsetu.core.R.drawable.ic_navigation_arrow)
                        // Rotate icon to match direction of travel
                        val nearest = routePoints.minByOrNull { it.distanceToAsDouble(loc) }
                        val nearestIdx = routePoints.indexOf(nearest)
                        if (nearestIdx >= 0 && nearestIdx < routePoints.size - 1) {
                            rotation = routePoints[nearestIdx].bearingTo(routePoints[nearestIdx + 1]).toFloat()
                        }
                    } else {
                        icon = ContextCompat.getDrawable(context, com.nagarsetu.core.R.drawable.ic_user_location_modern)
                    }
                }
                mapView.overlays.add(marker)
            }

            // ── Destination marker ───────────────────────────────────────────
            destination?.let { dest ->
                val marker = Marker(mapView).apply {
                    position = dest
                    title = destinationName
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(
                        context,
                        destinationType.markerDrawableRes()
                    )
                }
                mapView.overlays.add(marker)
            }

            // ── Fit markers on screen or focus on user ─────────────────────
            if (isNavigating && userLocation != null) {
                mapView.controller.setCenter(userLocation)
                if (mapView.zoomLevelDouble < 15.5) {
                    mapView.controller.setZoom(16.5)
                }
                
                // Compute bearing from the last two route points near the user and rotate the map
                if (routePoints.size >= 2) {
                    val nearest = routePoints.minByOrNull { it.distanceToAsDouble(userLocation) }
                    val nearestIdx = routePoints.indexOf(nearest)
                    if (nearestIdx >= 0 && nearestIdx < routePoints.size - 1) {
                        val from = routePoints[nearestIdx]
                        val to   = routePoints[nearestIdx + 1]
                        val bearing = from.bearingTo(to).toFloat()
                        mapView.mapOrientation = -bearing  // OSMDroid rotates map opposite to bearing
                    }
                }
            } else if (userLocation != null && destination != null && userLocation.latitude != 0.0) {
                val box = BoundingBox.fromGeoPoints(listOf(userLocation, destination))
                
                // Safety: if bounding box is wider than 0.5 degrees (~55km), 
                // something is wrong with coords — center on destination at zoom 15 instead
                val latSpan = abs(box.latNorth - box.latSouth)
                val lngSpan = abs(box.lonEast - box.lonWest)

                if (latSpan > 0.5 || lngSpan > 0.5) {
                    Log.w("RouteMap", "Bounding box too large (${latSpan}° lat, ${lngSpan}° lng). Coords likely wrong.")
                    mapView.controller.setZoom(15.0)
                    mapView.controller.setCenter(destination)
                } else {
                    mapView.zoomToBoundingBox(box.increaseByScale(1.4f), true, 80)
                    // Ensure minimum zoom level (don't go below street level)
                    if (mapView.zoomLevelDouble < 13.0) {
                        mapView.controller.setZoom(14.0)
                        mapView.controller.setCenter(box.centerWithDateLine)
                    }
                }
            }

            mapView.invalidate()
        }
    )
}
