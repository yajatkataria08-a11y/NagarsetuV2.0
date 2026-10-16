package com.nagarsetu.raksha.presentation.livetrack

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.theme.AlertRed
import com.nagarsetu.core.ui.theme.EmeraldGreen
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.theme.PrimaryBlue
import com.nagarsetu.raksha.presentation.RakshaViewModel

@Composable
fun LiveTrackScreen(
    onBack: () -> Unit = {},
    viewModel: RakshaViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val isSharingLocation by viewModel.isSharingLocation.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val sosTriggered by viewModel.sosTriggered.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()

    var shareUrl by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Tap below to share your live location") }

    // Generate link dynamically when sharing is toggled ON
    LaunchedEffect(isSharingLocation, userLocation) {
        if (isSharingLocation) {
            shareUrl = "https://www.google.com/maps/search/?api=1&query=${userLocation.latitude},${userLocation.longitude}"
            statusText = "🟢 Tracking — Mom & Dad notified"
        } else {
            shareUrl = null
            statusText = "Tap below to share your live location"
        }
    }

    val snackState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackState.showSnackbar(it)
            snackMsg = null
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost   = { SnackbarHost(snackState) }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NagarSetuColors.Background)
        ) {
            // ── Map ────────────────────────────────────────────────────────
            OSMMapView(
                modifier = Modifier.fillMaxSize(),
                locationProvider = viewModel.locationProvider,
                centerLat = userLocation.latitude,
                centerLng = userLocation.longitude,
                markers  = listOf(
                    MapMarker(userLocation.latitude + 0.0015, userLocation.longitude + 0.002, "Mom", "Tracking Active"),
                    MapMarker(userLocation.latitude - 0.0025, userLocation.longitude - 0.001, "Dad", "Tracking Active")
                ),
                zoom = 15.5
            )

            // ── PANIC overlay banner ──────────────────────────────────────
            AnimatedVisibility(
                visible = sosTriggered,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
            ) {
                Card(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = AlertRed)
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("🚨 PANIC ALERT SENT", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                            Text("Emergency contacts notified • Help requested", color = Color.White.copy(0.85f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── Header bar ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = Color.Black.copy(0.45f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(8.dp)
                                .background(if (isSharingLocation) EmeraldGreen else Color.Gray, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isSharingLocation) "LIVE SHARING" else "OFFLINE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Bottom Panel ────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface.copy(0.97f))
            ) {
                Column(Modifier.padding(20.dp)) {

                    // ── Sharing toggle row ──────────────────────────────────
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Family Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(statusText, color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked         = isSharingLocation,
                            onCheckedChange = { _ ->
                                viewModel.toggleLocationSharing()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldGreen,
                                checkedTrackColor = EmeraldGreen.copy(0.35f)
                            )
                        )
                    }

                    // ── Share URL + action buttons (visible when sharing) ────
                    AnimatedVisibility(visible = shareUrl != null, enter = expandVertically(tween(220)), exit = shrinkVertically()) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = NagarSetuColors.SurfaceVariant)
                            Spacer(Modifier.height(12.dp))

                            // URL display
                            Surface(
                                color  = NagarSetuColors.Background,
                                shape  = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier          = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        shareUrl ?: "",
                                        modifier  = Modifier.weight(1f),
                                        color     = NagarSetuColors.TextSecondary,
                                        style     = MaterialTheme.typography.labelSmall,
                                        maxLines  = 1,
                                        overflow  = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Action buttons row: Copy + WhatsApp
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Copy link
                                OutlinedButton(
                                    onClick  = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("NagarSetu Live Link", shareUrl))
                                        snackMsg = "📋 Link copied to clipboard"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy Link", fontSize = 12.sp)
                                }

                                // WhatsApp share
                                Button(
                                    onClick = {
                                        val url = shareUrl ?: return@Button
                                        val msg = "I'm sharing my live location via NagarSetu Raksha. Track me: $url"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?text=${Uri.encode(msg)}"))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            })
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = NagarSetuColors.SurfaceVariant)
                    Spacer(Modifier.height(14.dp))

                    // ── Tracking contacts status ─────────────────────────────
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val momContact = trustedContacts.getOrNull(0)
                        val dadContact = trustedContacts.getOrNull(1)

                        TrackingContact(
                            name = momContact?.name ?: "Mom",
                            active = isSharingLocation,
                            onClick = {
                                momContact?.let { contact ->
                                    val locUrl = "https://maps.google.com/?q=${userLocation.latitude},${userLocation.longitude}"
                                    val msg = "Hi, I’m sharing my live location from NagarSetu.\nTrack me here:\n$locUrl"
                                    val phone = contact.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                    val whatsappUrl = "https://wa.me/$phone?text=${Uri.encode(msg)}"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                    } catch (e: Exception) {
                                        // Fallback
                                        context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        })
                                    }
                                } ?: run { snackMsg = "No contact saved for Mom" }
                            }
                        )

                        TrackingContact(
                            name = dadContact?.name ?: "Dad",
                            active = isSharingLocation,
                            onClick = {
                                dadContact?.let { contact ->
                                    val locUrl = "https://maps.google.com/?q=${userLocation.latitude},${userLocation.longitude}"
                                    val msg = "Hi, I’m sharing my live location from NagarSetu.\nTrack me here:\n$locUrl"
                                    val phone = contact.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                    val whatsappUrl = "https://wa.me/$phone?text=${Uri.encode(msg)}"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                    } catch (e: Exception) {
                                        // Fallback
                                        context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        })
                                    }
                                } ?: run { snackMsg = "No contact saved for Dad" }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── PANIC ALERT button ─────────────────────────────────
                    Button(
                        onClick  = {
                            viewModel.triggerSosTap()
                            if (!isSharingLocation) viewModel.toggleLocationSharing()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = "Trigger panic alert SOS" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sosTriggered) AlertRed.copy(0.7f) else AlertRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (sosTriggered) "PANIC ALERT SENT" else "TRIGGER PANIC ALERT",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingContact(name: String, active: Boolean, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(NagarSetuColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = if (active) NagarSetuColors.Accent else Color.Gray
                )
            }
            if (active) {
                Box(
                    Modifier
                        .size(14.dp)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(EmeraldGreen, CircleShape))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NagarSetuColors.TextPrimary)
        Text(
            if (active) "tracking" else "offline",
            fontSize = 10.sp,
            color    = if (active) EmeraldGreen else NagarSetuColors.TextSecondary
        )
    }
}
