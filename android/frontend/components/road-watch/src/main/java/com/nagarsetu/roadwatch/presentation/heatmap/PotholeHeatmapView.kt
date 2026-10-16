package com.nagarsetu.roadwatch.presentation.heatmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.map.OSMMapView

@Composable
fun PotholeHeatmapView(markers: List<MapMarker>) {
    OSMMapView(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        markers = markers
    )
}
