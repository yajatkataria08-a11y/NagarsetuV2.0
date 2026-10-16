package com.nagarsetu.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors

enum class ServiceScreen {
    NONE, PARK_EASE, HEALTH_WATCH, CHARGE_UP, GREEN_ROUTE, DRIVE_LEGAL, RAKSHA, ASSISTANT, PREDICTIVE, ROAD_WATCH
}

data class ServiceEntry(
    val screen: ServiceScreen,
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesMenuScreen(onServiceSelected: (ServiceScreen) -> Unit) {
    val s = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }

    val serviceEntries = remember(s) {
        listOf(
            ServiceEntry(ServiceScreen.PARK_EASE,    s.service.serviceParkEase,    s.service.serviceParkEaseDesc,    Icons.Filled.LocalParking,   Color(0xFF1976D2)),
            ServiceEntry(ServiceScreen.HEALTH_WATCH, s.service.serviceHealthWatch, s.service.serviceHealthWatchDesc, Icons.Filled.Favorite,       Color(0xFFE53935)),
            ServiceEntry(ServiceScreen.CHARGE_UP,    s.service.serviceChargeUp,    s.service.serviceChargeUpDesc,    Icons.Filled.EvStation,      Color(0xFF43A047)),
            ServiceEntry(ServiceScreen.GREEN_ROUTE,  s.service.serviceGreenRoute,  s.service.serviceGreenRouteDesc,  Icons.Filled.Terrain,        Color(0xFF2E7D32)),
            ServiceEntry(ServiceScreen.DRIVE_LEGAL,  s.service.serviceDriveLegal,  s.service.serviceDriveLegalDesc,  Icons.Filled.Scale,          Color(0xFF1565C0)),
            ServiceEntry(ServiceScreen.RAKSHA,       "Safety Guide",       "Raksha safety manual",   Icons.Filled.Shield,         Color(0xFFF4511E)),
        )
    }

    val quickActions = remember(s) {
        listOf(
            QuickActionItem(s.service.emergencyContacts, Icons.Filled.PeopleAlt),
            QuickActionItem(s.service.nearbyHospitals,   Icons.Filled.LocalHospital),
            QuickActionItem(s.service.trafficUpdates,    Icons.Filled.Traffic),
            QuickActionItem(s.service.cityAnnouncements, Icons.AutoMirrored.Filled.VolumeUp),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.nav.navServices, fontWeight = FontWeight.Bold, color = NagarSetuColors.Accent) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back if needed, but this is a main tab */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NagarSetuColors.Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(s.common.search, color = NagarSetuColors.TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NagarSetuColors.TextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NagarSetuColors.Surface,
                        unfocusedContainerColor = NagarSetuColors.Surface,
                        focusedBorderColor = NagarSetuColors.Accent.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // All Services Header
            item {
                Text(
                    text = s.service.allServices,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Services Grid (implemented using items in LazyColumn for simple integration)
            val rows = serviceEntries.chunked(3)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowItems.forEach { entry ->
                        ServiceCard(
                            entry = entry,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            onClick = { onServiceSelected(entry.screen) }
                        )
                    }
                    // Fill empty spaces if row has fewer than 3 items
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f).padding(4.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // Quick Actions Header
            item {
                Text(
                    text = s.raksha.rakshaQuickActions,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Quick Actions List
            items(quickActions) { action ->
                QuickActionRow(action)
            }
        }
    }
}

@Composable
fun ServiceCard(entry: ServiceEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = NagarSetuColors.Surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(entry.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.name,
                    tint = entry.color,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // Description removed for compact grid, or kept if space allows. 
            // User requested "Short description" so I'll add it if it looks good.
            Text(
                text = entry.desc,
                style = MaterialTheme.typography.labelSmall,
                color = NagarSetuColors.TextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class QuickActionItem(val title: String, val icon: ImageVector)

@Composable
fun QuickActionRow(item: QuickActionItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { },
        color = NagarSetuColors.Surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = NagarSetuColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NagarSetuColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
