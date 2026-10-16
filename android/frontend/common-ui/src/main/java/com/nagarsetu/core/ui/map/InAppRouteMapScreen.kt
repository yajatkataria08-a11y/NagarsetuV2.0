package com.nagarsetu.core.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straight
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material.icons.outlined.UTurnLeft
import androidx.compose.material.icons.outlined.UTurnRight
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppRouteMapScreen(
    args: RouteMapArgs,
    onBack: () -> Unit,
    viewModel: RouteMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speedStateKph by viewModel.currentSpeedKph.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val route = uiState.routeInfo
    val arrivalTime = remember(route) {
        route?.let { r ->
            val arrivalMillis = System.currentTimeMillis() + (r.durationSeconds * 1000).toLong()
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(arrivalMillis))
        }
    }

    // Layer state
    var activeLayer by remember { mutableStateOf(MapLayer.OSM_STANDARD) }
    var showTraffic by remember { mutableStateOf(false) }
    var showTransit by remember { mutableStateOf(false) }

    var showNavChoiceSheet by remember { mutableStateOf(false) }
    var inAppNavActive by remember { mutableStateOf(false) }
    var liveUserLocation by remember { mutableStateOf(uiState.userLocation) }
    var currentSpeedKph by remember { mutableStateOf(0f) }

    val voiceCoach = remember { VoiceNavigationCoach(context) }
    var voiceMuted by remember { mutableStateOf(false) }

    // Shutdown TTS when the screen leaves composition
    DisposableEffect(Unit) {
        onDispose { voiceCoach.shutdown() }
    }

    // Sync mute state
    LaunchedEffect(voiceMuted) {
        voiceCoach.setMuted(voiceMuted)
    }

    LaunchedEffect(args) {
        viewModel.loadRoute(args.destinationLat, args.destinationLng)
    }

    LaunchedEffect(inAppNavActive) {
        if (inAppNavActive) {
            val steps = uiState.routeInfo?.steps ?: emptyList()
            // Announce start
            voiceCoach.announceStart(steps, uiState.routeInfo?.distanceText ?: "")
            
            viewModel.startNavigationTracking { geoPoint, speedKph ->
                liveUserLocation = geoPoint
                currentSpeedKph  = speedKph
                // Feed location to voice coach
                voiceCoach.updateLocation(geoPoint, steps)
            }
        } else {
            // Navigation stopped
            if (voiceMuted.not()) voiceCoach.announceStop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Unified Map (OSM or Google depending on activeLayer) ──────────
        UnifiedMapView(
            modifier = Modifier.fillMaxSize(),
            activeLayer = activeLayer,
            routePoints = uiState.routeInfo?.polylinePoints ?: emptyList(),
            userLocation = if (inAppNavActive) liveUserLocation else uiState.userLocation,
            destination = GeoPoint(args.destinationLat, args.destinationLng),
            destinationName = args.destinationName,
            destinationType = args.iconType,
            showTrafficLayer = showTraffic,
            showTransitLayer = showTransit,
            isNavigating = inAppNavActive
        )

        // ── Top Bar (Back button + Station name) ──────────────────────────
        if (!inAppNavActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = args.destinationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (args.destinationSubtitle.isNotBlank()) {
                        Text(
                            text = args.destinationSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Navigation instruction banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Navigating to ${args.destinationName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            uiState.routeInfo?.let { "${it.distanceText} · ${it.etaText}" +
                                if (arrivalTime != null) " · Arrive $arrivalTime" else "" } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        if (route?.nextTurnInstruction.orEmpty().isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        route!!.nextTurnInstruction.contains("left", ignoreCase = true) -> Icons.Outlined.TurnLeft
                                        route.nextTurnInstruction.contains("right", ignoreCase = true) -> Icons.Outlined.TurnRight
                                        else -> Icons.Outlined.Straight
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${route.nextTurnDistanceText} · ${route.nextTurnInstruction}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming turn instruction card (shown below the top bar while navigating)
            val steps = uiState.routeInfo?.steps
            val nextStep = steps?.getOrNull(1)   // step[0] = current leg, step[1] = next turn
            if (nextStep != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 72.dp)    // just below the navigation bar
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Direction icon
                        val turnIcon = when {
                            nextStep.maneuverModifier.contains("left") &&
                            nextStep.maneuverModifier.contains("sharp")  -> Icons.Outlined.UTurnLeft
                            nextStep.maneuverModifier.contains("left")   -> Icons.Outlined.TurnLeft
                            nextStep.maneuverModifier.contains("right") &&
                            nextStep.maneuverModifier.contains("sharp")  -> Icons.Outlined.UTurnRight
                            nextStep.maneuverModifier.contains("right")  -> Icons.Outlined.TurnRight
                            nextStep.maneuverType == "roundabout"        -> Icons.Outlined.RotateRight
                            nextStep.maneuverType == "arrive"            -> Icons.Outlined.Place
                            else                                         -> Icons.Outlined.Straight
                        }
                        Icon(
                            imageVector = turnIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "In ${steps[0].distanceText}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = nextStep.instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ── Layer Switcher FAB (top-right) ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 12.dp)  // below top bar
        ) {
            MapLayerSwitcher(
                activeLayer = activeLayer,
                showTraffic = showTraffic,
                showTransit = showTransit,
                onLayerChange = { activeLayer = it },
                onTrafficToggle = { showTraffic = !showTraffic },
                onTransitToggle = { showTransit = !showTransit }
            )
        }

        // ── Active layer badge (bottom-left corner of map) ────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 160.dp),  // above bottom card
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (activeLayer == MapLayer.OSM_STANDARD)
                                Color(0xFF4CAF50) else Color(0xFF1A73E8),
                            CircleShape
                        )
                )
                Text(
                    text = when (activeLayer) {
                        MapLayer.OSM_STANDARD    -> "OpenStreetMap"
                        MapLayer.GOOGLE_NORMAL   -> "Google Maps"
                        MapLayer.GOOGLE_SATELLITE-> "Satellite"
                        MapLayer.GOOGLE_HYBRID   -> "Hybrid"
                        MapLayer.GOOGLE_TERRAIN  -> "Terrain"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // ── Mute Button ───────────────────────────────────────────────────
        if (inAppNavActive) {
            FloatingActionButton(
                onClick = { voiceMuted = !voiceMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 140.dp)
                    .size(44.dp),
                shape = CircleShape,
                containerColor = if (voiceMuted)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (voiceMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    contentDescription = if (voiceMuted) "Unmute voice" else "Mute voice",
                    tint = if (voiceMuted)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Loading overlay ───────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Fetching route...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Bottom Card: Distance + ETA + Navigate button ─────────────────
        if (route != null) {
            if (!inAppNavActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // Distance + ETA row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RouteStatChip(
                                icon = Icons.Outlined.Directions,
                                label = "Distance",
                                value = route.distanceText
                            )
                            RouteStatChip(
                                icon = Icons.Outlined.Schedule,
                                label = "ETA",
                                value = route.etaText
                            )
                            RouteStatChip(
                                icon = Icons.Outlined.ElectricCar,
                                label = "Mode",
                                value = "Driving"
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Navigate button (opens navigation choice sheet)
                        Button(
                            onClick = { showNavChoiceSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Navigation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Turn-by-Turn Navigation")
                        }
                    }
                }
            } else {
                // In-app nav bottom strip
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                route.distanceText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                route.etaText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (arrivalTime != null) {
                                Text(
                                    "Arrive $arrivalTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentSpeedKph.toInt()} km/h",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSpeedKph > 60f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { inAppNavActive = false },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }

        if (showNavChoiceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNavChoiceSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Choose Navigation Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Navigate in-app with OSM or open Google Maps for full voice guidance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))

                    // Option A — In-App Navigation
                    OutlinedButton(
                        onClick = {
                            showNavChoiceSheet = false
                            inAppNavActive = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Navigate in App", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Uses OSM map · No internet required after load",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option B — Google Maps
                    Button(
                        onClick = {
                            showNavChoiceSheet = false
                            val uri = Uri.parse(
                                "google.navigation:q=${args.destinationLat},${args.destinationLng}&mode=d"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            val fallback = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://maps.google.com/maps?daddr=${args.destinationLat},${args.destinationLng}")
                            )
                            try { context.startActivity(intent) }
                            catch (e: Exception) { context.startActivity(fallback) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Navigation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Open Google Maps", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Voice-guided turn-by-turn · Opens external app",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    TextButton(onClick = { showNavChoiceSheet = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStatChip(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
