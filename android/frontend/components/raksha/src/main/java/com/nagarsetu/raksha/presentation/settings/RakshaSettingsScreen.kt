package com.nagarsetu.raksha.presentation.settings

/**
 * RakshaSettingsScreen.kt
 *
 * Ported from Raksha/SafePath's SettingsFragment into Jetpack Compose.
 * Added to NagarSetu Raksha module with full Hilt/ViewModel integration.
 *
 * Features (from Raksha):
 *  • Emergency contact management
 *  • Voice navigation toggle (VoiceCoach)
 *  • Stick-to-main-roads routing preference
 *  • WearOS watch status + ping
 *  • Developer tools (simulate SOS, refresh hazards, clear cache)
 *
 * Connected to NagarSetu via:
 *  • MainActivity composable("raksha_settings")
 *  • RakshaScreen settings gear icon
 */

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.theme.AlertRed
import com.nagarsetu.core.ui.theme.EmeraldGreen
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.theme.PrimaryBlue
import com.nagarsetu.core.ui.theme.WarnAmber
import com.nagarsetu.raksha.presentation.RakshaViewModel

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RakshaSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: RakshaViewModel = hiltViewModel()
) {
    val watchConnected by viewModel.isWatchConnected.collectAsState()
    val heartRate      by viewModel.heartRate.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()

    // ── Local preferences (persisted by ViewModel in real build) ─────────────
    var emergencyContact by remember(trustedContacts) { 
        mutableStateOf(trustedContacts.firstOrNull { it.isEmergencyContact }?.phoneNumber ?: "") 
    }
    var stickToMainRoads by remember { mutableStateOf(true) }
    var voiceEnabled     by remember { mutableStateOf(true) }
    var verboseLogging   by remember { mutableStateOf(false) }

    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackState   = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackState.showSnackbar(it)
            snackMessage = null
        }
    }

    Scaffold(
        containerColor = NagarSetuColors.Background,
        snackbarHost   = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Raksha Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Personalise safety preferences", color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NagarSetuColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Emergency Contact ────────────────────────────────────────────
            item(key = "contact_header") { SectionHeader("Emergency Contact") }
            item(key = "contact_field") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value         = emergencyContact,
                            onValueChange = { emergencyContact = it },
                            label         = { Text("Phone number") },
                            placeholder   = { Text("+91 98765 43210") },
                            leadingIcon   = { Icon(Icons.Default.Phone, null, tint = AlertRed) },
                            trailingIcon  = {
                                IconButton(onClick = {
                                    viewModel.saveTrustedContact(emergencyContact)
                                    snackMessage = "Emergency contact updated!"
                                }) {
                                    Icon(Icons.Default.Save, "Save", tint = NagarSetuColors.Accent)
                                }
                            },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AlertRed,
                                unfocusedBorderColor = NagarSetuColors.SurfaceVariant
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This number receives SOS SMS alerts and is shown during live tracking.",
                            color = NagarSetuColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // ── Navigation Preferences ────────────────────────────────────────
            item(key = "nav_header") { SectionHeader("Navigation Preferences") }
            item(key = "main_roads_toggle") {
                SettingsToggle(
                    icon    = Icons.Default.Route,
                    label   = "Stick to main roads",
                    desc    = "Prefer well-lit main roads for safe routing (avoids shortcuts)",
                    color   = PrimaryBlue,
                    checked = stickToMainRoads,
                    onToggle = { stickToMainRoads = it }
                )
            }
            item(key = "voice_toggle") {
                SettingsToggle(
                    icon    = Icons.Default.RecordVoiceOver,
                    label   = "Voice navigation",
                    desc    = "Spoken route instructions using Android TTS (VoiceCoach)",
                    color   = EmeraldGreen,
                    checked = voiceEnabled,
                    onToggle = { voiceEnabled = it }
                )
            }

            // ── WearOS Watch ──────────────────────────────────────────────────
            item(key = "watch_header") { SectionHeader("WearOS Smartwatch") }
            item(key = "watch_card") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (watchConnected) EmeraldGreen.copy(alpha = 0.08f) else NagarSetuColors.Surface
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = "Watch status",
                            tint               = if (watchConnected) EmeraldGreen else NagarSetuColors.TextSecondary,
                            modifier           = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (watchConnected) "Watch Connected" else "No Watch Found",
                                fontWeight = FontWeight.SemiBold,
                                color      = if (watchConnected) EmeraldGreen else NagarSetuColors.TextPrimary
                            )
                            Text(
                                if (watchConnected)
                                    "Crown 3× = SOS" + if (heartRate > 0) "  •  ♥ $heartRate bpm" else ""
                                else
                                    "Open Wear OS companion app",
                                color = NagarSetuColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (watchConnected) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.simulateWatchSos()
                                    snackMessage = "📡 Ping sent to watch"
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldGreen.copy(alpha = 0.14f))
                            ) {
                                Text("Ping", color = EmeraldGreen, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ── Developer Tools ────────────────────────────────────────────────
            item(key = "dev_header") { SectionHeader("Developer Tools") }
            item(key = "dev_buttons") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        DevToolButton(
                            icon    = Icons.Default.BugReport,
                            label   = "Simulate SOS",
                            color   = AlertRed,
                            onClick = {
                                viewModel.triggerSosTap()
                                snackMessage = "🚨 SOS simulated"
                            }
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = NagarSetuColors.SurfaceVariant)
                        DevToolButton(
                            icon    = Icons.Default.Refresh,
                            label   = "Refresh Hazard Zones",
                            color   = WarnAmber,
                            onClick = { snackMessage = "⚠️ Hazard zones refreshed (demo)" }
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = NagarSetuColors.SurfaceVariant)
                        DevToolButton(
                            icon    = Icons.Default.DeleteSweep,
                            label   = "Clear Crime Cache",
                            color   = PrimaryBlue,
                            onClick = { snackMessage = "🗑️ Crime dataset reloaded" }
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = NagarSetuColors.SurfaceVariant)
                        SettingsToggle(
                            icon    = Icons.Default.Code,
                            label   = "Verbose logging",
                            desc    = "Print extra debug info to Logcat",
                            color   = NagarSetuColors.TextSecondary,
                            checked = verboseLogging,
                            onToggle = { verboseLogging = it },
                            embedded = true
                        )
                    }
                }
            }

            // ── App version footer ─────────────────────────────────────────────
            item(key = "footer") {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NagarSetu Raksha", color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("Features integrated from SafePath Indore (Raksha)", color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Shared helper composables ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = NagarSetuColors.TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggle(
    icon:     ImageVector,
    label:    String,
    desc:     String,
    color:    androidx.compose.ui.graphics.Color,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
    embedded: Boolean = false
) {
    val wrapper: @Composable (@Composable () -> Unit) -> Unit = if (embedded) {
        { content -> content() }
    } else {
        { content ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
            ) { content() }
        }
    }
    wrapper {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(if (embedded) 4.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(desc, color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.30f)
                )
            )
        }
    }
}

@Composable
private fun DevToolButton(
    icon:    ImageVector,
    label:   String,
    color:   androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
        FilledTonalButton(
            onClick = onClick,
            colors  = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.12f))
        ) {
            Text("Run", color = color, fontSize = 12.sp)
        }
    }
}
