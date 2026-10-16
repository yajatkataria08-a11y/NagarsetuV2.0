package com.nagarsetu.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.osmdroid.util.GeoPoint

@Composable
fun UnifiedMapView(
    modifier: Modifier = Modifier,
    activeLayer: MapLayer,
    routePoints: List<GeoPoint>,
    userLocation: GeoPoint?,
    destination: GeoPoint?,
    destinationName: String,
    destinationType: RouteDestinationType,
    showTrafficLayer: Boolean = false,
    showTransitLayer: Boolean = false,
    isNavigating: Boolean = false,
    onMapClick: ((GeoPoint) -> Unit)? = null
) {
    when (activeLayer) {
        MapLayer.OSM_STANDARD -> {
            OSMRouteMapView(
                modifier = modifier,
                routePoints = routePoints,
                userLocation = userLocation,
                destination = destination,
                destinationName = destinationName,
                destinationType = destinationType,
                isNavigating = isNavigating
            )
        }
        MapLayer.GOOGLE_NORMAL,
        MapLayer.GOOGLE_SATELLITE,
        MapLayer.GOOGLE_HYBRID,
        MapLayer.GOOGLE_TERRAIN -> {
            GoogleMapsRouteView(
                modifier = modifier,
                activeLayer = activeLayer,
                routePoints = routePoints,
                userLocation = userLocation,
                destination = destination,
                destinationName = destinationName,
                destinationType = destinationType,
                showTrafficLayer = showTrafficLayer,
                showTransitLayer = showTransitLayer,
                onMapClick = onMapClick
            )
        }
    }
}
