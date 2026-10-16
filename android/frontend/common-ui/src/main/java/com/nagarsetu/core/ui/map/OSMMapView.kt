package com.nagarsetu.core.ui.map

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.nagarsetu.core.ui.theme.BHOPAL_LAT
import com.nagarsetu.core.ui.theme.BHOPAL_LNG
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.core.utils.LatLngPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * OSM map composable with built-in GPS support.
 *
 * When [enableGps] is true (default):
 *  • A blue "my location" dot follows the user in real time using osmdroid's
 *    MyLocationNewOverlay (no extra permission dialog — the app already declares
 *    ACCESS_FINE_LOCATION in the manifest).
 *  • A FAB in the bottom-right corner re-centres the map on the user's position.
 *  • The map initially centres on the user's last-known location (Bhopal centre
 *    as fallback if GPS is unavailable or permission is denied).
 *
 * All existing call sites work unchanged — [centerLat]/[centerLng] are still
 * honoured when [enableGps] is false (or when passed explicitly).
 */
@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    centerLat: Double = BHOPAL_LAT,
    centerLng: Double = BHOPAL_LNG,
    zoom: Double = 14.0,
    markers: List<MapMarker> = emptyList(),
    overlayFactory: (MapView) -> Overlay? = { null },
    onMapReady: (MapView) -> Unit = {},
    enableGps: Boolean = true,
    locationProvider: LocationProvider? = null,
    onMapClick: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Track live GPS centre; default to passed-in lat/lng (which is usually Bhopal centre)
    var gpsCentre by remember { mutableStateOf(LatLngPoint(centerLat, centerLng, isGpsFix = false)) }

    // Resolve initial location once (fast — uses last-known cache from FusedLocation)
    LaunchedEffect(locationProvider) {
        if (enableGps && locationProvider != null) {
            val loc = locationProvider.getLastLocation()
            gpsCentre = loc
        }
    }

    // Subscribe to live updates so the user dot and (optionally) re-centre work
    LaunchedEffect(locationProvider, enableGps) {
        if (enableGps && locationProvider != null) {
            locationProvider.locationFlow().collect { loc ->
                gpsCentre = loc
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(gpsCentre.latitude, gpsCentre.longitude))
        }
    }

    // My-Location overlay (black pinpoint + halo)
    val myLocationOverlay = remember(enableGps) {
        if (enableGps) {
            MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                enableMyLocation()
                // Replace default blue dot with modern pinpoint
                val personIcon = androidx.core.content.ContextCompat.getDrawable(context, com.nagarsetu.core.R.drawable.ic_user_location_modern)
                if (personIcon != null) {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        personIcon.intrinsicWidth,
                        personIcon.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    personIcon.setBounds(0, 0, canvas.width, canvas.height)
                    personIcon.draw(canvas)
                    setPersonIcon(bitmap)
                    setPersonHotspot(bitmap.width / 2f, bitmap.height / 2f)
                }
            }
        } else null
    }

    DisposableEffect(markers) {
        mapView.overlays.removeAll { it is Marker }
        mapView.setMarkers(markers)
        // Ensure MyLocation overlay is always on top of markers
        myLocationOverlay?.let { ov ->
            if (!mapView.overlays.contains(ov)) mapView.overlays.add(ov)
        }
        onDispose {
            myLocationOverlay?.disableMyLocation()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.also { onMapReady(it) }
            },
            update = { map ->
                // Removed: Automatic camera centering on every update to keep map on Bhopal by default.
                // Centering is now only performed on initial load or via the "My Location" FAB.

                map.overlays.removeAll { it is Marker }
                map.setMarkers(markers)
                myLocationOverlay?.let { ov ->
                    if (!map.overlays.contains(ov)) map.overlays.add(ov)
                }
                overlayFactory(map)?.let { ov ->
                    if (!map.overlays.any { it === ov }) map.overlays.add(ov)
                }

                // Map Click Events
                val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onMapClick(p.latitude, p.longitude)
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean = false
                })
                map.overlays.add(eventsOverlay)

                map.invalidate()
            }
        )

        // "My Location" FAB — re-centres map on user's current GPS position
        if (enableGps && locationProvider != null) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val loc = locationProvider.getLastLocation()
                        gpsCentre = loc
                        mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Centre on my location",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
