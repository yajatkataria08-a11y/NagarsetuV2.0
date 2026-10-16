package com.nagarsetu.healthwatch.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.healthwatch.domain.model.AirQualityData
import com.nagarsetu.healthwatch.domain.model.Pollutant
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-visibility AQI color mapper.
 */
private fun getAqiColor(value: Int): Color = when {
    value <= 50  -> NagarSetuColors.SuccessGreen
    value <= 100 -> NagarSetuColors.WarningOrange
    value <= 200 -> Color(0xFFFF9800) // Orange
    else         -> NagarSetuColors.SOSRed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthWatchScreen(
    viewModel: HealthWatchViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val airQuality by viewModel.airQuality.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    
    val locationName = "Bhopal City Center"
    val backgroundColor = Color(0xFFF1F5F9) 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.service.healthWatchTitle, fontWeight = FontWeight.ExtraBold, color = NagarSetuColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NagarSetuColors.Accent)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = NagarSetuColors.Accent)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NagarSetuColors.Accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = NagarSetuColors.TextPrimary,
                    navigationIconContentColor = NagarSetuColors.Accent
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && airQuality == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NagarSetuColors.Accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Analysing Bhopal Air Quality...", color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Connecting to live monitoring stations", color = NagarSetuColors.TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Status
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = NagarSetuColors.Accent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(locationName, fontSize = 15.sp, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                }
                                val ts = airQuality?.timestamp ?: System.currentTimeMillis()
                                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
                                Text("Last update: $timeStr", fontSize = 12.sp, color = NagarSetuColors.TextSecondary, fontWeight = FontWeight.Medium)
                            }
                            Surface(
                                color = NagarSetuColors.SuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, NagarSetuColors.SuccessGreen.copy(alpha = 0.4f))
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(NagarSetuColors.SuccessGreen, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text("LIVE DATA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NagarSetuColors.SuccessGreen)
                                }
                            }
                        }
                    }

                    // HERO CARD
                    item {
                        airQuality?.let { data ->
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(32.dp)),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "AIR QUALITY INDEX", 
                                        style = MaterialTheme.typography.labelLarge, 
                                        color = NagarSetuColors.TextSecondary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    
                                    AQIGauge(data.aqi, data.status)
                                    
                                    Spacer(Modifier.height(28.dp))
                                    
                                    val aqiColor = getAqiColor(data.aqi)
                                    Surface(
                                        color = aqiColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, aqiColor.copy(alpha = 0.2f))
                                    ) {
                                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "AQI ${data.aqi} • ${data.status}",
                                                color = aqiColor,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = data.description,
                                                color = NagarSetuColors.TextPrimary,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pollutants Section Header
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Detailed Pollutants", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NagarSetuColors.TextPrimary)
                            Spacer(Modifier.width(12.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NagarSetuColors.TextDisabled.copy(alpha = 0.3f))
                        }
                    }

                    // Pollutants Grid
                    item {
                        airQuality?.let { data ->
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                data.pollutants.chunked(2).forEach { rowItems ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        rowItems.forEach { pollutant ->
                                            PollutantCard(pollutant, modifier = Modifier.weight(1f))
                                        }
                                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Advice Card
                    item {
                        airQuality?.let { data ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Primary),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.HealthAndSafety, 
                                            contentDescription = null, 
                                            tint = if(data.aqi <= 100) Color(0xFF81C784) else Color(0xFFFFD54F),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(20.dp))
                                    Column {
                                        Text("Citizen Health Advice", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 17.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = data.recommendation,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Map Section
                    item {
                        Text("City Health Hotspots", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NagarSetuColors.TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                                .shadow(4.dp, RoundedCornerShape(24.dp))
                        ) {
                            OSMMapView(
                                modifier = Modifier.fillMaxSize(),
                                markers = mapMarkers,
                                zoom = 12.0,
                                enableGps = false
                            )
                            Surface(
                                modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                                color = NagarSetuColors.Primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Disease Surveillance", 
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White, 
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }

            // Professional Branding Watermark
            AppWatermark(
                alignment = Alignment.BottomCenter,
                opacity = 0.15f
            )
        }
    }
}

@Composable
fun AQIGauge(value: Int, status: String) {
    val sweepAngle = 240f
    val startAngle = 150f
    val progress = (value.toFloat() / 300f).coerceIn(0.05f, 1f)
    val aqiColor = getAqiColor(value)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        Canvas(modifier = Modifier.size(210.dp)) {
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0f to NagarSetuColors.SuccessGreen,
                    0.3f to NagarSetuColors.WarningOrange,
                    0.6f to NagarSetuColors.SOSRed,
                    1f to Color.Black
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(), 
                fontSize = 80.sp, 
                fontWeight = FontWeight.Black, 
                color = NagarSetuColors.TextPrimary,
                letterSpacing = (-4).sp
            )
            Text(
                text = status.uppercase(), 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Black, 
                color = aqiColor,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun PollutantCard(pollutant: Pollutant, modifier: Modifier = Modifier) {
    val color = try { Color(android.graphics.Color.parseColor(pollutant.colorHex)) } catch (e: Exception) { NagarSetuColors.Accent }
    
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pollutant.name, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = NagarSetuColors.TextPrimary
                )
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pollutant.level.uppercase(), 
                        fontSize = 9.sp, 
                        color = color,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${pollutant.value.toInt()} ${pollutant.unit}",
                fontSize = 22.sp, 
                fontWeight = FontWeight.Black,
                color = NagarSetuColors.TextPrimary
            )
            
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { pollutant.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = Color(0xFFF1F5F9)
            )
            
            Spacer(Modifier.height(14.dp))
            Text(
                text = pollutant.healthImpact,
                fontSize = 11.sp,
                color = NagarSetuColors.TextSecondary,
                lineHeight = 15.sp,
                maxLines = 2,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
