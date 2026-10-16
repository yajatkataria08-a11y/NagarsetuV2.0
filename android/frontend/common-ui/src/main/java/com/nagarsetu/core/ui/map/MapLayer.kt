package com.nagarsetu.core.ui.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Satellite
import androidx.compose.ui.graphics.vector.ImageVector

enum class MapLayer {
    OSM_STANDARD,       // OpenStreetMap — default, no key needed
    GOOGLE_NORMAL,      // Google Maps standard street view
    GOOGLE_SATELLITE,   // Google satellite imagery
    GOOGLE_HYBRID,      // Satellite + street labels (BEST for routing)
    GOOGLE_TERRAIN      // Terrain elevation map
}

data class MapLayerConfig(
    val layer: MapLayer,
    val label: String,
    val icon: ImageVector,
    val requiresGoogleKey: Boolean
)

val ALL_MAP_LAYERS = listOf(
    MapLayerConfig(MapLayer.OSM_STANDARD,    "Standard",   Icons.Outlined.Map,          false),
    MapLayerConfig(MapLayer.GOOGLE_NORMAL,   "Google",     Icons.Outlined.Place,         true),
    MapLayerConfig(MapLayer.GOOGLE_SATELLITE,"Satellite",  Icons.Outlined.Satellite,     true),
    MapLayerConfig(MapLayer.GOOGLE_HYBRID,   "Hybrid",     Icons.Outlined.Layers,        true),
    MapLayerConfig(MapLayer.GOOGLE_TERRAIN,  "Terrain",    Icons.Outlined.Landscape,     true),
)
