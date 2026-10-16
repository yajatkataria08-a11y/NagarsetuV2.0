package com.nagarsetu.chargeup.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nagarsetu.chargeup.domain.model.*
import com.nagarsetu.core.ui.map.InAppRouteMapScreen
import com.nagarsetu.core.ui.map.RouteDestinationType
import com.nagarsetu.core.ui.map.RouteMapArgs
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.utils.NavigationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChargeUpScreen(
    viewModel: ChargeUpViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    // REAL BACKEND DATA
    val stations by viewModel.stations.collectAsState()
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var searchQuery by remember { mutableStateOf("") }

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NAGARSETU", fontSize = 10.sp, color = NagarSetuColors.Accent,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(s.service.serviceChargeUp, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = NagarSetuColors.TextPrimary)
                }
                // Active count badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NagarSetuColors.Surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(NagarSetuColors.SuccessGreen, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("${stations.size} ${s.common.status}", fontSize = 12.sp, color = NagarSetuColors.TextPrimary)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(s.common.search, color = NagarSetuColors.TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NagarSetuColors.Accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                    focusedTextColor = NagarSetuColors.TextPrimary,
                    unfocusedTextColor = NagarSetuColors.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Station list
            val displayed = if (searchQuery.isBlank()) stations 
                           else stations.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayed) { station ->
                    StationCard(
                        station = station,
                        onViewOnMap = {
                            val route = "route_map/${station.latitude}/${station.longitude}/" +
                                    "${Uri.encode(station.name)}/${Uri.encode("${station.powerKw} kW · ₹${station.costPerKwh}/${s.service.perUnit}")}/EV_STATION"
                            onNavigate(route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StationCard(station: ChargingStation, onViewOnMap: () -> Unit) {
    val s = LocalAppStrings.current
    val statusColor = when (station.status) {
        StationStatus.AVAILABLE         -> NagarSetuColors.SuccessGreen
        StationStatus.OCCUPIED          -> NagarSetuColors.WarningOrange
        StationStatus.UNDER_MAINTENANCE -> NagarSetuColors.SOSRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NagarSetuColors.Surface)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ElectricBolt, null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(station.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                    Text("${s.generic.bhopal} · ${station.powerKw} kW", fontSize = 11.sp, color = NagarSetuColors.TextSecondary)
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val statusLabel = if(s.profile.langHindi=="हिन्दी") {
                    when(station.status) {
                        StationStatus.AVAILABLE -> "उपलब्ध"
                        StationStatus.OCCUPIED -> "व्यस्त"
                        StationStatus.UNDER_MAINTENANCE -> "रखरखाव"
                    }
                } else station.status.name
                Text(statusLabel, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("₹${station.costPerKwh}/${s.service.perUnit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                Text(s.service.price, fontSize = 11.sp, color = NagarSetuColors.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onViewOnMap,
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.generic.viewOnMap, fontSize = 12.sp)
                }
            }
        }
    }
}
