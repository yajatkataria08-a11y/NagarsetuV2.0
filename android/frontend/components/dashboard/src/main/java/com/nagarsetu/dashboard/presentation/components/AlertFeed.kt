package com.nagarsetu.dashboard.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.dashboard.domain.model.AlertSeverity
import com.nagarsetu.dashboard.domain.model.AlertType
import com.nagarsetu.dashboard.domain.model.DashboardAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Animated alert feed item with severity-coded left border and icon.
 * New alerts slide in from the top via AnimatedContent.
 */
@Composable
fun AlertFeedItem(
    alert: DashboardAlert,
    modifier: Modifier = Modifier
) {
    val borderColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertRed
        AlertSeverity.WARNING  -> WarnAmber
        AlertSeverity.INFO     -> PrimaryBlue
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Severity border
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon badge
                Box(
                    Modifier
                        .size(36.dp)
                        .background(borderColor.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = alertIcon(alert.type),
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatTime(alert.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                    Text(
                        "Ward: ${alert.ward}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Crisis level banner displayed at top of dashboard.
 * Pulses red/amber for elevated states.
 */
@Composable
fun CrisisBanner(
    level: com.nagarsetu.dashboard.domain.model.CrisisLevel,
    modifier: Modifier = Modifier
) {
    val (color, label, icon) = when (level) {
        com.nagarsetu.dashboard.domain.model.CrisisLevel.NORMAL   -> Triple(EcoGreen,    "All systems normal — Bhopal is safe", Icons.Default.CheckCircle)
        com.nagarsetu.dashboard.domain.model.CrisisLevel.ELEVATED -> Triple(WarnAmber,   "Elevated: Monitor activity in city", Icons.Default.Warning)
        com.nagarsetu.dashboard.domain.model.CrisisLevel.CRITICAL -> Triple(Color(0xFFFF6D00), "Critical: Multiple incidents active", Icons.Default.PriorityHigh)
        com.nagarsetu.dashboard.domain.model.CrisisLevel.EMERGENCY-> Triple(AlertRed,    "EMERGENCY: Crisis protocol active!", Icons.Default.NotificationsActive)
    }

    val pulseAnim = rememberInfiniteTransition(label = "crisis")
    val alpha by pulseAnim.animateFloat(
        0.7f, 1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = color.copy(alpha = if (level == com.nagarsetu.dashboard.domain.model.CrisisLevel.NORMAL) 0.1f else alpha * 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    level.label,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(0.8f)
                )
            }
        }
    }
}

private fun alertIcon(type: AlertType): ImageVector = when (type) {
    AlertType.CIVIC          -> Icons.Default.AccountBalance
    AlertType.TRAFFIC        -> Icons.Default.Traffic
    AlertType.HEALTH         -> Icons.Default.LocalHospital
    AlertType.GEOFENCE_ENTRY -> Icons.Default.LocationOn
    AlertType.SLA_BREACH     -> Icons.Default.Schedule
    AlertType.NEW_CRISIS     -> Icons.Default.NotificationsActive
    AlertType.HAZARD         -> Icons.Default.Warning
}

private fun formatTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000  -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        else           -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
