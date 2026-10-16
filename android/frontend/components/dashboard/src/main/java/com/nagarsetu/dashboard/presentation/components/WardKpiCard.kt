package com.nagarsetu.dashboard.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.dashboard.domain.model.WardAuthority

/**
 * Ward-level KPI card with:
 * - Resolution rate progress bar
 * - Budget utilization bar
 * - Pulsing SLA breach indicator
 * - Direct helpline call button
 */
@Composable
fun WardKpiCard(
    ward: WardAuthority,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasSlaBreaches = ward.slaBreaches > 0

    // Pulsing animation for SLA breaches
    val pulseTransition = rememberInfiniteTransition(label = "sla_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSlaBreaches && ward.slaBreaches >= 20)
                AlertRed.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Header row ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ward.wardName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        ward.authorityName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // SLA breach badge with pulse
                if (hasSlaBreaches) {
                    Box(
                        Modifier
                            .background(
                                AlertRed.copy(alpha = if (ward.slaBreaches >= 20) pulseAlpha * 0.9f else 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${ward.slaBreaches} SLA",
                            color = if (ward.slaBreaches >= 20) Color.White else AlertRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Call button
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ward.helpline}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call ${ward.wardName} helpline",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiMiniStat("Total", "${ward.complaintCount}", TextSecondary)
                KpiMiniStat("Resolved", "${ward.resolvedCount}", EmeraldGreen)
                KpiMiniStat("Pending", "${ward.pendingComplaints}", WarnAmber)
            }

            Spacer(Modifier.height(10.dp))

            // ── Resolution rate bar ───────────────────────────────────────────
            KpiProgressBar(
                label = "Resolution Rate",
                value = ward.resolutionRate,
                color = when {
                    ward.resolutionRate >= 0.85f -> EmeraldGreen
                    ward.resolutionRate >= 0.65f -> WarnAmber
                    else                          -> AlertRed
                },
                suffix = "${(ward.resolutionRate * 100).toInt()}%"
            )

            Spacer(Modifier.height(6.dp))

            // ── Budget utilization bar ────────────────────────────────────────
            KpiProgressBar(
                label = "Budget Utilized",
                value = ward.budgetUtilization,
                color = when {
                    ward.budgetUtilization >= 0.9f -> WarnAmber
                    ward.budgetUtilization >= 0.6f -> PrimaryBlue
                    else                            -> EmeraldGreen
                },
                suffix = "₹${ward.budgetSpent / 100_000}L / ₹${ward.budgetSanctioned / 100_000}L"
            )
        }
    }
}

@Composable
private fun KpiMiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun KpiProgressBar(
    label: String,
    value: Float,
    color: Color,
    suffix: String
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(suffix, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { animatedValue },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
