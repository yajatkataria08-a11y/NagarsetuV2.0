package com.nagarsetu.greenroute.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.greenroute.domain.model.*

@Composable
fun GreenRouteScreen(viewModel: GreenRouteViewModel = hiltViewModel()) {
    var destination by remember { mutableStateOf("") }
    val routeOptions by viewModel.routeOptions.collectAsState()
    val nearbyStations by viewModel.nearbyStations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var selectedRoute by remember { mutableStateOf<RouteOption?>(null) }
    val s = LocalAppStrings.current

    LaunchedEffect(routeOptions) {
        if (selectedRoute == null && routeOptions.isNotEmpty()) {
            selectedRoute = routeOptions.first()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NagarSetuColors.Background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("NAGARSETU", fontSize = 10.sp, color = NagarSetuColors.Accent,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(s.service.serviceGreenRoute, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = NagarSetuColors.TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NagarSetuColors.SuccessGreen)
                } else {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(NagarSetuColors.SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Park, contentDescription = null,
                            tint = NagarSetuColors.SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            // Destination input
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(NagarSetuColors.Surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null,
                    tint = NagarSetuColors.TextSecondary, modifier = Modifier.padding(start = 12.dp))
                TextField(
                    value = destination,
                    onValueChange = { destination = it },
                    placeholder = { Text(s.common.search,
                        color = NagarSetuColors.TextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = NagarSetuColors.TextPrimary,
                        unfocusedTextColor = NagarSetuColors.TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true
                )
                Box(
                    modifier = Modifier.padding(8.dp).size(36.dp)
                        .clip(RoundedCornerShape(8.dp)).background(NagarSetuColors.SuccessGreen)
                        .clickable { viewModel.findRoutes(destination) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // REAL MAP IMPLEMENTATION
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp)).background(NagarSetuColors.Surface)
                    .border(1.dp, NagarSetuColors.Accent.copy(0.1f), RoundedCornerShape(14.dp)),
            ) {
                OSMMapView(
                    modifier = Modifier.fillMaxSize(),
                    markers = nearbyStations.map { station ->
                        MapMarker(
                            lat = station.latitude,
                            lng = station.longitude,
                            title = station.name,
                            snippet = "${station.mode} - Arrival in ${station.nextArrivalMin}m",
                            tintColor = when(station.mode) {
                                TransitMode.BUS -> android.graphics.Color.parseColor("#4FC3F7")
                                TransitMode.CYCLE -> android.graphics.Color.parseColor("#4CAF50")
                                TransitMode.AUTO -> android.graphics.Color.parseColor("#FFB74D")
                                TransitMode.WALK -> android.graphics.Color.parseColor("#81C784")
                            }
                        )
                    } + (selectedRoute?.let { 
                        listOf(MapMarker(
                            lat = it.destLat, 
                            lng = it.destLng, 
                            title = "Destination", 
                            snippet = "Your destination",
                            iconRes = com.nagarsetu.core.R.drawable.ic_marker_destination_pin
                        ))
                    } ?: emptyList()),
                    enableGps = true,
                    locationProvider = viewModel.locationProvider
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Text(s.service.greenRouteTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = NagarSetuColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        if (routeOptions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No routes found. Try searching for a destination.", color = NagarSetuColors.TextSecondary)
                }
            }
        }

        items(routeOptions) { route ->
            RouteCard(
                route = route,
                selected = selectedRoute?.id == route.id,
                onClick = { selectedRoute = route }
            )
        }

        // Live arrivals
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.service.arrivalIn, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.TextPrimary)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NagarSetuColors.SOSRed.copy(0.1f)).padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", color = NagarSetuColors.SOSRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        items(nearbyStations) { station ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp)).background(NagarSetuColors.Surface).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(station.mode.toColor().copy(0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(station.mode.toIcon(), contentDescription = null,
                            tint = station.mode.toColor(), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(station.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = NagarSetuColors.TextPrimary)
                        Text(station.routes.joinToString(", "), fontSize = 11.sp, color = NagarSetuColors.TextSecondary)
                    }
                }
                Text("${station.nextArrivalMin} ${s.service.minSuffix}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.SuccessGreen)
            }
        }
    }

    // Professional Branding Watermark
    AppWatermark(
        alignment = Alignment.BottomEnd,
        opacity = 0.12f,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun RouteCard(route: RouteOption, selected: Boolean, onClick: () -> Unit) {
    val s = LocalAppStrings.current
    val color = route.mode.toColor()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(0.15f) else NagarSetuColors.Surface)
            .border(if (selected) 1.dp else 0.dp, color, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(color.copy(0.2f)), contentAlignment = Alignment.Center) {
                Icon(route.mode.toIcon(), contentDescription = null,
                    tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(route.mode.toLabel(), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${route.durationMin} ${s.service.minSuffix}", fontSize = 12.sp, color = NagarSetuColors.TextSecondary)
                    if (route.cost > 0)
                        Text("₹${route.cost.toInt()}", fontSize = 12.sp, color = NagarSetuColors.TextSecondary)
                    else
                        Text(s.service.free, fontSize = 12.sp, color = NagarSetuColors.SuccessGreen)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${route.carbonSavedKg}${if(s.profile.langHindi=="हिन्दी") "किग्रा" else "kg"} CO₂", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = NagarSetuColors.SuccessGreen)
            Text(s.service.ecoSaved, fontSize = 10.sp, color = NagarSetuColors.TextSecondary)
            Text(route.crowdedness.toLabel(), fontSize = 10.sp,
                color = when (route.crowdedness) {
                    Crowdedness.LOW      -> NagarSetuColors.SuccessGreen
                    Crowdedness.MODERATE -> NagarSetuColors.WarningOrange
                    else       -> NagarSetuColors.SOSRed
                })
        }
    }
}

@Composable
fun TransitMode.toIcon(): ImageVector = when(this) {
    TransitMode.BUS -> Icons.Filled.DirectionsBus
    TransitMode.CYCLE -> Icons.Filled.PedalBike
    TransitMode.AUTO -> Icons.Filled.ElectricRickshaw
    TransitMode.WALK -> Icons.Filled.DirectionsWalk
}

@Composable
fun TransitMode.toColor(): Color = when(this) {
    TransitMode.BUS -> Color(0xFF4FC3F7)
    TransitMode.CYCLE -> NagarSetuColors.SuccessGreen
    TransitMode.AUTO -> NagarSetuColors.WarningOrange
    TransitMode.WALK -> Color(0xFF81C784)
}

@Composable
fun TransitMode.toLabel(): String {
    val s = LocalAppStrings.current
    return when(this) {
        TransitMode.BUS -> s.service.bus
        TransitMode.CYCLE -> s.service.cycle
        TransitMode.AUTO -> s.service.auto
        TransitMode.WALK -> "Walk" 
    }
}

@Composable
fun Crowdedness.toLabel(): String {
    val s = LocalAppStrings.current
    return when(this) {
        Crowdedness.LOW -> s.service.low
        Crowdedness.MODERATE -> s.service.moderate
        Crowdedness.HIGH -> s.service.high
        Crowdedness.FULL -> "Full"
    }
}
