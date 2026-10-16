package com.nagarsetu.dashboard.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.components.MapBrandedOverlay
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.map.WardHeatmapOverlay
import com.nagarsetu.core.ui.map.loadWardHeatmap
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.dashboard.domain.model.MapMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityMapScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    locationProvider: LocationProvider? = null,
    onBack: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val markers by viewModel.markers.collectAsState()
    val isMapLoading by viewModel.isMapLoading.collectAsState()
    val currentMode by viewModel.mapMode.collectAsState()
    val searchText by viewModel.searchQuery.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val layers = listOf(
        LayerItem("All",             MapMode.CIVIC,       Icons.Default.Apps,          Color.Gray),
        LayerItem("Police Stations", MapMode.EMERGENCY,   Icons.Default.Security,      Color.Blue),
        LayerItem("Hospitals",       MapMode.HOSPITALS,    Icons.Default.LocalHospital, Color.Red),
        LayerItem("EV Charging",     MapMode.EV_CHARGING,  Icons.Default.EvStation,     Color(0xFF43A047)),
        LayerItem("Risk Heatmap",    MapMode.TRAFFIC,      Icons.Default.Waves,         Color(0xFFE53935))
    )
    
    // Mock risk data for dashboard heatmap mode
    val wardRiskData = mapOf(
        "W01" to 0.15f, "W02" to 0.42f, "W03" to 0.68f, "W04" to 0.85f,
        "W05" to 0.33f, "W06" to 0.55f, "W07" to 0.12f, "W08" to 0.45f,
        "W09" to 0.77f, "W10" to 0.22f, "W11" to 0.52f, "W12" to 0.10f, "W13" to 0.38f
    )
    val wardPolygons = remember { loadWardHeatmap(context, wardRiskData) }

    Scaffold(
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Full Screen Map
            Box(Modifier.fillMaxSize()) {
                OSMMapView(
                    modifier = Modifier.fillMaxSize(),
                    locationProvider = locationProvider,
                    enableGps = true,
                    overlayFactory = { mapView ->
                        if (currentMode == MapMode.TRAFFIC) {
                            WardHeatmapOverlay(wardPolygons)
                        } else {
                            null
                        }
                    },
                    markers = markers
                )
            }

            // Map Branding Overlay
            MapBrandedOverlay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 16.dp),
                opacity = 0.18f
            )

            // Professional Branding Watermark
            AppWatermark(
                alignment = Alignment.BottomEnd,
                opacity = 0.12f,
                modifier = Modifier.padding(bottom = 96.dp)
            )

            // Loading Indicator
            if (isMapLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    color = NagarSetuColors.Accent
                )
            }

            // Top Search Bar Overlay
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = NagarSetuColors.TextSecondary, 
                        modifier = Modifier.clickable { onBack() }
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = searchText,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(s.map.searchLocation, color = NagarSetuColors.TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Icon(Icons.Default.Search, contentDescription = null, tint = NagarSetuColors.TextSecondary)
                }
            }

            // Layer Selector at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(layers) { layer ->
                        FilterChip(
                            selected = currentMode == layer.mode,
                            onClick = { 
                                viewModel.setMapMode(layer.mode)
                            },
                            label = { Text(layer.name, fontSize = 12.sp) },
                            leadingIcon = { Icon(layer.icon, null, modifier = Modifier.size(16.dp), tint = if (currentMode == layer.mode) Color.White else layer.color) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = layer.color,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

data class LayerItem(val name: String, val mode: MapMode, val icon: ImageVector, val color: Color)
