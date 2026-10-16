package com.nagarsetu.raksha.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.NagarSetuScreenBackground
import com.nagarsetu.core.ui.components.NagarSetuTopBar
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.AlertRed
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.raksha.presentation.fakecall.FakeCallScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RakshaScreen(
    viewModel: RakshaViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLiveTrack: () -> Unit = {},
    onNavigateToEmergencyGuide: () -> Unit = {},
    permissionsState: Any? = null
) {
    val s = LocalAppStrings.current
    val sosTriggered by viewModel.sosTriggered.collectAsState()
    val isSosLoading by viewModel.isSosLoading.collectAsState()
    var showFakeCall by remember { mutableStateOf(false) }

    if (showFakeCall) {
        FakeCallScreen(onDismiss = { showFakeCall = false })
        return
    }

    NagarSetuScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NagarSetuTopBar(
                title = "Emergency SOS & Safety",
                subtitle = if (sosTriggered) "Help is on the way!" else "Press and hold for assistance"
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SimpleSosButton(
                        isActive = sosTriggered,
                        onTrigger = { viewModel.triggerSosTap() },
                        onCancel = { viewModel.clearSos() }
                    )
                    
                    Spacer(Modifier.height(28.dp))
                    
                    if (isSosLoading) {
                        CircularProgressIndicator(color = AlertRed)
                        Text("Transmitting beacon to Command Center...", modifier = Modifier.padding(top = 8.dp), color = AlertRed)
                    } else if (sosTriggered) {
                        Text("🚨 SOS BEACON ACTIVE", fontSize = 22.sp, fontWeight = FontWeight.Black, color = AlertRed)
                        Text("Live GPS & Audio streamed to Command Grid", color = NagarSetuColors.TextSecondary, fontSize = 12.sp)
                    } else {
                        Text("HOLD BUTTON TO ACTIVATE SOS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextSecondary)
                    }

                    Spacer(Modifier.height(36.dp))

                    // Tactical Safety Actions (Inspired by Raksha)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Fake Call Deterrent
                        OutlinedButton(
                            onClick = { showFakeCall = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fake Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Live Location Link
                        OutlinedButton(
                            onClick = onNavigateToLiveTrack,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Live Track", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Emergency Guide
                        OutlinedButton(
                            onClick = onNavigateToEmergencyGuide,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleSosButton(
    isActive: Boolean,
    onTrigger: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    val infinite = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(if (isActive) AlertRed else Color(0xFFB91C1C))
            .pointerInput(isActive) {
                detectTapGestures(
                    onPress = {
                        if (isActive) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancel()
                        } else {
                            isHolding = true
                            holdProgress = 0f
                            val job = scope.launch {
                                while (holdProgress < 1f) {
                                    delay(40)
                                    holdProgress += 0.04f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTrigger()
                                isHolding = false
                            }
                            tryAwaitRelease()
                            job.cancel()
                            isHolding = false
                            holdProgress = 0f
                        }
                    }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isActive) Icons.Default.Cancel else Icons.Default.Emergency,
                contentDescription = "SOS",
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isActive) "CANCEL" else "SOS",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
