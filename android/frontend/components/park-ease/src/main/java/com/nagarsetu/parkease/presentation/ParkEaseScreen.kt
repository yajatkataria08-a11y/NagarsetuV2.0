package com.nagarsetu.parkease.presentation

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.parkease.domain.model.ParkingLot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkEaseScreen(
    viewModel: ParkEaseViewModel = hiltViewModel(),
    locationProvider: LocationProvider? = null,
    onNavigate: (String) -> Unit = {}
) {
    val s = LocalAppStrings.current
    val parkingLots by viewModel.parkingLots.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchText by remember { mutableStateOf("") }
    var selectedLotId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val displayed = if (searchText.isBlank()) parkingLots
                    else parkingLots.filter { it.name.contains(searchText, ignoreCase = true) }

    Scaffold(
        containerColor = NagarSetuColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NAGARSETU",
                        style = MaterialTheme.typography.labelSmall,
                        color = NagarSetuColors.Accent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = s.service.findBookParking,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NagarSetuColors.TextPrimary
                    )
                }

                Surface(
                    color = NagarSetuColors.Accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NagarSetuColors.Accent, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${parkingLots.size} Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = NagarSetuColors.Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(s.service.searchNearMe, color = NagarSetuColors.TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NagarSetuColors.Accent) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface,
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = NagarSetuColors.TextPrimary,
                    unfocusedTextColor = NagarSetuColors.TextPrimary
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 3. Filtered list
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (activeBooking != null) {
                    item {
                        ActiveBookingCard(activeBooking!!, viewModel)
                    }
                }

                items(displayed, key = { it.id }) { lot ->
                    ParkingLotCard(
                        lot = lot,
                        isSelected = selectedLotId == lot.id,
                        onSingleTap = { selectedLotId = lot.id },
                        onViewOnMap = {
                            val route = "route_map/${lot.latitude}/${lot.longitude}/" +
                                "${Uri.encode(lot.name)}/${Uri.encode("₹${lot.ratePerHour.toInt()}/hr · ${lot.availableSlots} slots free")}/PARKING"
                            onNavigate(route)
                        },
                        onBook = { viewModel.holdSlot(lot) }
                    )
                }
            }
        }
    }
}

@Composable
fun ParkingLotCard(
    lot: ParkingLot,
    isSelected: Boolean,
    onSingleTap: () -> Unit,
    onViewOnMap: () -> Unit,
    onBook: () -> Unit
) {
    val statusColor = when (lot.status) {
        "Available"    -> NagarSetuColors.SuccessGreen
        "Limited"      -> NagarSetuColors.WarningOrange
        "Nearly Full"  -> NagarSetuColors.WarningOrange
        else           -> NagarSetuColors.SOSRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSingleTap() }
            .then(
                if (isSelected) Modifier.border(2.dp, NagarSetuColors.Accent, RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalParking, contentDescription = null, tint = statusColor)
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NagarSetuColors.TextPrimary
                    )
                    Text(
                        text = "Bhopal · ${lot.availableSlots} slots free",
                        style = MaterialTheme.typography.labelSmall,
                        color = NagarSetuColors.TextSecondary
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = lot.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${lot.ratePerHour.toInt()}/hr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NagarSetuColors.TextPrimary
                    )
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = NagarSetuColors.TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBook,
                        enabled = lot.availableSlots > 0,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NagarSetuColors.Accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NagarSetuColors.Accent)
                    ) {
                        Text("Hold Slot", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onViewOnMap,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Map", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveBookingCard(booking: com.nagarsetu.parkease.domain.model.ParkingBooking, viewModel: ParkEaseViewModel) {
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val seconds by viewModel.holdSecondsRemaining.collectAsState()
    
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        color = NagarSetuColors.Accent.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "Booking QR",
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Active Booking", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Slot #${booking.slotNumber}", color = Color.White.copy(0.8f))
                Text("Expires in ${seconds / 60}m ${seconds % 60}s", fontSize = 12.sp, color = Color.Yellow)
            }
            IconButton(onClick = { viewModel.cancelBooking() }) {
                Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.White)
            }
        }
    }
}
