package com.nagarsetu.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors

@Composable
fun DashboardScreen(
    onEmergencyFab: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onReportIssue: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    userName: String? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
    locationProvider: com.nagarsetu.core.utils.LocationProvider? = null
) {
    val s = LocalAppStrings.current
    val weather by viewModel.weather.collectAsState()
    val aqi by viewModel.aqi.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val allFeatures = listOf(
        FeatureItem(s.roadWatch.roadWatchTitle, Icons.Default.AddRoad, NagarSetuColors.Primary, "road_watch"),
        FeatureItem(s.service.predictiveTitle, Icons.Default.Psychology, NagarSetuColors.Accent, "predictive"),
        FeatureItem(s.service.chargeUpTitle, Icons.Default.ElectricBolt, NagarSetuColors.WarningOrange, "charge_up"),
        FeatureItem(s.service.parkEaseTitle, Icons.Default.LocalParking, NagarSetuColors.Accent, "park_ease"),
        FeatureItem(s.service.greenRouteTitle, Icons.Default.Park, NagarSetuColors.SuccessGreen, "green_route"),
        FeatureItem(s.service.healthWatchTitle, Icons.Default.LocalHospital, NagarSetuColors.SOSRed, "health_watch")
    )

    val filteredFeatures = remember(searchQuery) {
        if (searchQuery.isBlank()) allFeatures
        else allFeatures.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NagarSetuColors.Background)
            .statusBarsPadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenAssistant,
                    containerColor = NagarSetuColors.Accent,
                    contentColor = NagarSetuColors.Background,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                // 1. Header: Greeting & Status Pill
                HomeHeader(
                    name = userName ?: "User",
                    temp = weather?.temp?.toInt() ?: 32,
                    aqi = aqi?.aqi ?: 45
                )

                // 2. Functional Search Hub
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeSearchBar(
                        placeholder = s.common.search + " city services...",
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )

                    LazyRow(
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tags = listOf("Parking", "AI", "Charging", "Roads", "Hospital")
                        items(tags) { tag ->
                            Surface(
                                onClick = {
                                    searchQuery = when(tag) {
                                        "Parking"  -> "Park"
                                        "Charging" -> "Charge"
                                        "Roads"    -> "Road"
                                        "Hospital" -> "Health"
                                        else       -> tag
                                    }
                                },
                                shape = CircleShape,
                                color = NagarSetuColors.Accent.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.Accent.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NagarSetuColors.Accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Services Section
                Text(
                    text = s.nav.navServices,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NagarSetuColors.TextPrimary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
                )

                // 4. Feature Grid
                if (filteredFeatures.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No services found for '$searchQuery'", color = NagarSetuColors.TextSecondary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFeatures) { feature ->
                            ServiceCard(feature) { onNavigate(feature.route) }
                        }
                    }
                }
            }

            // Professional Branding Watermark
            AppWatermark(
                alignment = Alignment.BottomCenter,
                opacity = 0.12f,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun HomeHeader(name: String, temp: Int, aqi: Int) {
    val s = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NagarSetuColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = NagarSetuColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if(s.profile.langHindi == "हिन्दी") "नमस्ते," else "Namaste,",
                    style = MaterialTheme.typography.labelSmall,
                    color = NagarSetuColors.TextSecondary
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NagarSetuColors.TextPrimary
                )
            }
        }

        // Weather | AQI Pill
        Surface(
            color = Color.White,
            shape = CircleShape,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.SurfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$temp°C | AQI $aqi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.TextPrimary
                )
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = NagarSetuColors.TextSecondary, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = NagarSetuColors.Accent) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, null, tint = NagarSetuColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = NagarSetuColors.TextPrimary,
            unfocusedTextColor = NagarSetuColors.TextPrimary,
            cursorColor = NagarSetuColors.Accent
        )
    )
}

@Composable
fun ServiceCard(feature: FeatureItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.SurfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(feature.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = feature.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NagarSetuColors.TextPrimary
            )
        }
    }
}

data class FeatureItem(val name: String, val icon: ImageVector, val color: Color, val route: String)
