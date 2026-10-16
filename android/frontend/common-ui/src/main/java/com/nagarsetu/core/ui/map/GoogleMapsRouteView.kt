package com.nagarsetu.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import org.osmdroid.util.GeoPoint

@Composable
fun GoogleMapsRouteView(
    modifier: Modifier = Modifier,
    activeLayer: MapLayer,
    routePoints: List<GeoPoint>,
    userLocation: GeoPoint?,
    destination: GeoPoint?,
    destinationName: String,
    destinationType: RouteDestinationType,
    showTrafficLayer: Boolean,
    showTransitLayer: Boolean,
    onMapClick: ((GeoPoint) -> Unit)?
) {
    // Convert GeoPoint (osmdroid) to LatLng (Google Maps)
    fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            destination?.toLatLng() ?: LatLng(23.2599, 77.4126),
            14f
        )
    }

    // Convert route points to Google LatLng list
    val googleRoutePoints = remember(routePoints) {
        routePoints.map { it.toLatLng() }
    }

    // Zoom to fit both markers
    LaunchedEffect(userLocation, destination) {
        if (userLocation != null && destination != null && userLocation.latitude != 0.0) {
            val bounds = LatLngBounds.builder()
                .include(userLocation.toLatLng())
                .include(destination.toLatLng())
                .build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 120),
                durationMs = 800
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = when (activeLayer) {
                MapLayer.GOOGLE_SATELLITE -> MapType.SATELLITE
                MapLayer.GOOGLE_HYBRID    -> MapType.HYBRID
                MapLayer.GOOGLE_TERRAIN   -> MapType.TERRAIN
                else                      -> MapType.NORMAL
            },
            isTrafficEnabled = showTrafficLayer,   // LIVE Bhopal traffic overlay
            isMyLocationEnabled = false // We draw our own for consistency
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        ),
        onMapClick = { latLng ->
            onMapClick?.invoke(GeoPoint(latLng.latitude, latLng.longitude))
        }
    ) {
        // ── Route polyline ──────────────────────────────────────────────
        if (googleRoutePoints.size >= 2) {
            // White border for contrast
            Polyline(
                points = googleRoutePoints,
                color = Color.White,
                width = 18f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
            // Main route line
            Polyline(
                points = googleRoutePoints,
                color = Color(0xFF1565C0),      // deep blue
                width = 14f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND,
                zIndex = 1f
            )
        }

        // ── User location dot ──────────────────────────────────────────
        userLocation?.let { loc ->
            Circle(
                center = loc.toLatLng(),
                radius = 12.0,
                fillColor = Color(0xFF1976D2),
                strokeColor = Color.White,
                strokeWidth = 3f,
                zIndex = 2f
            )
            // Accuracy radius
            Circle(
                center = loc.toLatLng(),
                radius = 40.0,
                fillColor = Color(0x221976D2),
                strokeColor = Color(0x441976D2),
                strokeWidth = 1f
            )
        }

        // ── Destination marker ──────────────────────────────────────────
        destination?.let { dest ->
            Marker(
                state = MarkerState(position = dest.toLatLng()),
                title = destinationName,
                icon = BitmapDescriptorFactory.defaultMarker(
                    when (destinationType) {
                        RouteDestinationType.EV_STATION -> BitmapDescriptorFactory.HUE_GREEN
                        RouteDestinationType.PARKING    -> BitmapDescriptorFactory.HUE_AZURE
                        RouteDestinationType.HOSPITAL   -> BitmapDescriptorFactory.HUE_RED
                        RouteDestinationType.BUS_STOP   -> BitmapDescriptorFactory.HUE_ORANGE
                        else                            -> BitmapDescriptorFactory.HUE_VIOLET
                    }
                ),
                zIndex = 3f
            )
        }
    }
}
