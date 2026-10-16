/**
 * PredictiveScreen.kt — Optimized for Hackathon Demo
 */
package com.nagarsetu.predictive.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.components.NagarSetuScreenBackground
import com.nagarsetu.core.ui.components.NagarSetuTopBar
import com.nagarsetu.core.ui.components.SectionHeader
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.predictive.domain.model.*
import com.nagarsetu.predictive.presentation.components.*

// ─── Default city coordinates (Bhopal city centre) ────────────────────────────
private const val DEFAULT_LAT = 23.2599
private const val DEFAULT_LNG = 77.4126

@Composable
fun PredictiveScreen(
    viewModel: PredictiveViewModel = hiltViewModel(),
    locationProvider: LocationProvider? = null
) {
    val forecasts     by viewModel.forecasts.collectAsState()
    val ragResult     by viewModel.ragResult.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val bimstecCities by viewModel.bimstecCities.collectAsState()
    val proactiveAlerts by viewModel.proactiveAlerts.collectAsState()

    val pointRisk     by viewModel.pointRisk.collectAsState()
    val riskGrid      by viewModel.riskGrid.collectAsState()
    val isRiskLoading by viewModel.isRiskLoading.collectAsState()
    val riskLabel     by viewModel.riskLabel.collectAsState()
    val dataSource    by viewModel.dataSource.collectAsState()

    var selectedGridCell by remember { mutableStateOf<RiskGridCell?>(null) }
    var query by remember { mutableStateOf("") }
    var showHeatmapOnly by remember { mutableStateOf(false) }

    val s = LocalAppStrings.current

    LaunchedEffect(Unit) {
        viewModel.loadRiskData(lat = DEFAULT_LAT, lng = DEFAULT_LNG)
    }

    NagarSetuScreenBackground {
        LazyColumn(
            modifier      = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "topbar") {
                NagarSetuTopBar(
                    title    = s.service.predictiveTitle,
                    subtitle = s.service.predictiveSub
                )
            }

            // ── Hero Risk Gauge ──────────────────────────────────────────
            item(key = "risk_gauge") {
                RiskGaugeCard(
                    pointRisk = pointRisk,
                    isLoading = isRiskLoading,
                    riskLabel = riskLabel,
                    onRefresh = { viewModel.loadRiskData(DEFAULT_LAT, DEFAULT_LNG) }
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Interactive Heatmap ────────────────────────────────────────
            item(key = "heatmap_header") {
                SectionHeader(
                    title = s.predictive.cityRiskHeatmap,
                    action = if (isRiskLoading) s.predictive.syncing else s.predictive.live
                )
            }
            item(key = "heatmap_card") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
                ) {
                    Column {
                        RiskGridMap(
                            modifier = Modifier.fillMaxWidth().height(320.dp)
                        )
                        RiskGridLegend()
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── 7-Day Forecast ─────────────────────────────────────────────
            item(key = "forecast_header") {
                Column {
                    SectionHeader(title = s.predictive.sevenDayForecast)
                    if (dataSource != "model") {
                        Text(
                            text = "⚠ Estimated · Model training pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            item(key = "forecast_row") {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(forecasts) { fc ->
                        ForecastCard(fc)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Legend ──────────────────────────────────────────────────────
            item(key = "legend_row") {
                PredictionLegendRow()
                Spacer(Modifier.height(16.dp))
            }

            // ── Proactive Alerts ────────────────────────────────────────────
            if (proactiveAlerts.isNotEmpty()) {
                item(key = "alerts_header") {
                    SectionHeader(title = s.predictive.proactiveSafetyAlerts)
                }
                items(proactiveAlerts) { alert ->
                    ProactiveAlertCard(alert)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Civic Knowledge RAG ─────────────────────────────────────────
            item(key = "rag_box") {
                CivicKnowledgeCard(
                    query     = query,
                    onQuery   = { query = it },
                    ragResult = ragResult,
                    isLoading = isLoading,
                    onSearch  = { viewModel.queryCivicRAG(query) }
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── BIMSTEC Regional Data ───────────────────────────────────────
            item(key = "bimstec_header") {
                SectionHeader(title = s.predictive.bimstecDataHub)
            }
            items(bimstecCities) { city ->
                BimstecCityCard(city)
            }
        }

        // Professional Branding Watermark
        AppWatermark(
            alignment = Alignment.BottomCenter,
            opacity = 0.15f,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // ── Detail Sheet Overlay ──────────────────────────────────────────
        selectedGridCell?.let { cell ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(0.4f))
                    .clickable { selectedGridCell = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                RiskCellDetailSheet(cell = cell, onDismiss = { selectedGridCell = null })
            }
        }
    }
}

@Composable
private fun RiskGaugeCard(
    pointRisk: Double?,
    isLoading: Boolean,
    riskLabel: String,
    onRefresh: () -> Unit
) {
    val s = LocalAppStrings.current
    val progress   = ((pointRisk ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val gaugeColor = when {
        pointRisk == null       -> NagarSetuColors.TextSecondary
        pointRisk <= 25.0       -> NagarSetuColors.SuccessGreen
        pointRisk <= 55.0       -> WarnAmber
        pointRisk <= 75.0       -> Color(0xFFFF9800)
        else                    -> AlertRed
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        Row(
            modifier          = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isLoading) Color.Gray else gaugeColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text  = if (isLoading) s.common.loading else s.predictive.liveMlRiskScore,
                        color = NagarSetuColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                val displayLabel = if (s.profile.langHindi == "हिन्दी") {
                    riskLabel.replace("Low Risk", s.predictive.riskLow)
                         .replace("Moderate", s.predictive.riskModerate)
                         .replace("High Risk", s.predictive.riskHigh)
                         .replace("Extreme", s.predictive.riskSevere)
                         .replace("Risk model loading…", s.predictive.riskModelLoading)
                } else riskLabel

                Text(text = displayLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gaugeColor)
                Text(text = "Bhopal Civic ML • Real-time", color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(56.dp), color = NagarSetuColors.Accent, strokeWidth = 4.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        color = gaugeColor,
                        trackColor = NagarSetuColors.SurfaceVariant,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(text = if (pointRisk != null) "${pointRisk.toInt()}%" else "—", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = NagarSetuColors.Accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ForecastCard(forecast: Forecast) {
    val s = LocalAppStrings.current
    val color = when (forecast.riskLevel) {
        RiskLevel.LOW      -> NagarSetuColors.SuccessGreen
        RiskLevel.MODERATE -> WarnAmber
        RiskLevel.HIGH     -> Color(0xFFFF9800)
        else               -> AlertRed
    }
    val icon = when (forecast.type) {
        PredictionType.ACCIDENT -> Icons.Default.CarCrash
        PredictionType.FLOOD    -> Icons.Default.Water
        PredictionType.CRIME    -> Icons.Default.Shield
        PredictionType.HEALTH   -> Icons.Default.HealthAndSafety
    }

    val typeLabel = when (forecast.type) {
        PredictionType.ACCIDENT -> s.predictive.legendAccidents
        PredictionType.FLOOD    -> s.predictive.legendFloods
        PredictionType.CRIME    -> s.predictive.legendCrime
        PredictionType.HEALTH   -> s.predictive.legendHealth
    }

    Column(
        modifier = Modifier.width(110.dp).clip(RoundedCornerShape(16.dp)).background(NagarSetuColors.Surface).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = forecast.day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(text = "${(forecast.probability * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = typeLabel, fontSize = 9.sp, color = NagarSetuColors.TextSecondary)
    }
}

@Composable
private fun PredictionLegendRow() {
    val s = LocalAppStrings.current
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PredictionType.entries) { type ->
            val color = when(type) {
                PredictionType.ACCIDENT -> AlertRed
                PredictionType.FLOOD -> NagarSetuColors.Accent
                PredictionType.CRIME -> WarnAmber
                PredictionType.HEALTH -> NagarSetuColors.SuccessGreen
            }
            AssistChip(
                onClick = {},
                label = { Text(type.name, fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Circle, null, Modifier.size(8.dp), tint = color) },
                colors = AssistChipDefaults.assistChipColors(containerColor = NagarSetuColors.Surface)
            )
        }
    }
}

@Composable
private fun CivicKnowledgeCard(
    query: String,
    onQuery: (String) -> Unit,
    ragResult: RAGResult?,
    isLoading: Boolean,
    onSearch: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(s.predictive.civicKnowledgeAi, fontWeight = FontWeight.Bold, color = NagarSetuColors.Accent)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.predictive.ragSearchPlaceholder, fontSize = 12.sp) },
                trailingIcon = {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = onSearch) { Icon(Icons.Default.Search, null) }
                },
                shape = RoundedCornerShape(12.dp)
            )
            ragResult?.let {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().background(NagarSetuColors.Accent.copy(0.1f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text(it.answer, fontSize = 13.sp)
                }
            }
        }
    }
}
