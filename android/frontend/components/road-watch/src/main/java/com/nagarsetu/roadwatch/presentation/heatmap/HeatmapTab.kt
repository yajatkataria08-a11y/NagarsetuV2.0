package com.nagarsetu.roadwatch.presentation.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.roadwatch.domain.model.*
import com.nagarsetu.roadwatch.presentation.RoadWatchViewModel
import com.nagarsetu.roadwatch.presentation.components.SeverityBadge
import com.nagarsetu.roadwatch.presentation.components.color

@Composable
fun HeatmapTab(viewModel: RoadWatchViewModel) {
    val markers by viewModel.heatmapMarkers.collectAsState()
    val clusters by viewModel.clusters.collectAsState()
    var showHeatmap by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        // ── Map Layer (Blended OSM with Heat Overlays) ──────────────────────
        OSMMapView(
            modifier = Modifier.fillMaxSize(),
            markers = if (showHeatmap) markers else emptyList(),
            centerLat = 23.2599,
            centerLng = 77.4126,
            zoom = 13.0,
            enableGps = false
        )

        // ── Modern Floating Legend ──────────────────────────────────────────
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp), // Positioned above stats bar
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(0.85f)
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ROAD RISK INTENSITY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = NagarSetuColors.TextPrimary,
                    letterSpacing = 1.sp
                )
                
                val legendItems = listOf(
                    "Severe / Critical" to NagarSetuColors.SOSRed,
                    "High Risk" to NagarSetuColors.WarningOrange,
                    "Moderate" to Color(0xFFFFD54F),
                    "Low / Safe" to NagarSetuColors.SuccessGreen
                )

                legendItems.forEach { (label, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, CircleShape)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = NagarSetuColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Modern Layer Switcher FAB ──────────────────────────────────────
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .statusBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (showHeatmap) Icons.Default.Layers else Icons.Default.LayersClear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (showHeatmap) NagarSetuColors.Accent else NagarSetuColors.TextSecondary
                )
                Text(
                    "Smart Heatmap",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (showHeatmap) NagarSetuColors.TextPrimary else NagarSetuColors.TextSecondary
                )
                Switch(
                    checked = showHeatmap,
                    onCheckedChange = { showHeatmap = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NagarSetuColors.Accent,
                        uncheckedThumbColor = NagarSetuColors.TextDisabled,
                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }

        // ── Analytics Summary Card ────────────────────────────────────────
        val nearby by produceState<List<Pair<RoadReport, Double>>>(emptyList()) {
            value = viewModel.nearbyReports()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(0.95f)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Civic Hazard Pulse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NagarSetuColors.TextPrimary
                        )
                        Text(
                            "Live Bhopal City Analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = NagarSetuColors.TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NagarSetuColors.SuccessGreen.copy(0.1f)
                    ) {
                        Text(
                            "${markers.size} Hotspots",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NagarSetuColors.SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Severity.CRITICAL to NagarSetuColors.SOSRed,
                        Severity.HIGH to NagarSetuColors.WarningOrange,
                        Severity.MEDIUM to Color(0xFFFFD54F)
                    ).forEach { (sev, color) ->
                        val count = clusters.count { it.dominantSeverity == sev }
                        if (count >= 0) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = color.copy(0.12f),
                                border = if (count > 0) androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.2f)) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "$count",
                                        fontWeight = FontWeight.Black,
                                        color = color,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        sev.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (nearby.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = NagarSetuColors.TextDisabled.copy(0.2f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Live hazards within 2km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NagarSetuColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    nearby.take(2).forEach { (report, distKm) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(report.severity.color().copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(report.type.emoji, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    report.type.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NagarSetuColors.TextPrimary
                                )
                                Text(
                                    "${"%.1f".format(distKm)} km away",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NagarSetuColors.TextSecondary
                                )
                            }
                            SeverityBadge(report.severity)
                        }
                    }
                }
            }
        }
    }
}
