package com.nagarsetu.predictive.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nagarsetu.core.ui.map.WardHeatmapOverlay
import com.nagarsetu.core.ui.map.loadWardHeatmap
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.predictive.domain.model.PredictionType
import com.nagarsetu.predictive.domain.model.RiskGridCell
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Enhanced realistic heatmap for Bhopal using ward polygons.
 */
@Composable
fun RiskGridMap(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val wardRiskData = mapOf(
        "W01" to 0.15f, "W02" to 0.42f, "W03" to 0.68f, "W04" to 0.85f,
        "W05" to 0.33f, "W06" to 0.55f, "W07" to 0.12f, "W08" to 0.45f,
        "W09" to 0.77f, "W10" to 0.22f, "W11" to 0.52f, "W12" to 0.10f, "W13" to 0.38f
    )
    
    val wardPolygons = remember { loadWardHeatmap(context, wardRiskData) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(23.2599, 77.4126))
                
                overlays.add(0, WardHeatmapOverlay(wardPolygons))
                invalidate()
            }
        },
        update = { mapView ->
            mapView.overlays.removeAll { it is WardHeatmapOverlay }
            mapView.overlays.add(0, WardHeatmapOverlay(wardPolygons))
            mapView.invalidate()
        }
    )
}

@Composable
fun RiskGridLegend(modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(s.predictive.riskLow to EmeraldGreen, s.predictive.riskModerate to WarnAmber, s.predictive.riskHigh to Color(0xFFFF6D00), s.predictive.riskSevere to AlertRed).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color.copy(0.8f), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun RiskCellDetailSheet(
    cell: RiskGridCell,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val color = when(cell.dominantType) {
        PredictionType.ACCIDENT -> Color(0xFFE53935)
        PredictionType.FLOOD    -> Color(0xFF1565C0)
        PredictionType.CRIME    -> Color(0xFFF57F17)
        PredictionType.HEALTH   -> Color(0xFFAD1457)
    }
    val level = when {
        cell.riskScore >= 75 -> "🔴 ${s.predictive.riskSevere}"
        cell.riskScore >= 55 -> "🟠 ${s.predictive.riskHigh}"
        cell.riskScore >= 35 -> "🟡 ${s.predictive.riskModerate}"
        else                  -> "🟢 ${s.predictive.riskLow}"
    }

    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.predictive.gridCellDetail, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss) { Text(s.common.closeBtn) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(color.copy(0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                    Text("${cell.riskScore}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall, color = color)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(level, fontWeight = FontWeight.SemiBold)
                    val typeLabel = when(cell.dominantType) {
                        PredictionType.ACCIDENT -> s.predictive.legendAccidents
                        PredictionType.FLOOD -> s.predictive.legendFloods
                        PredictionType.CRIME -> s.predictive.legendCrime
                        PredictionType.HEALTH -> s.predictive.legendHealth
                    }
                    Text("${s.predictive.dominantRisk}: $typeLabel", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { cell.riskScore / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = color.copy(0.15f))
        }
    }
}
