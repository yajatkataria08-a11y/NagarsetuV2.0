package com.nagarsetu.roadwatch.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.roadwatch.domain.model.*

// ─── Severity color helper ────────────────────────────────────────────────────
fun Severity.color(): Color = Color(this.color)

// ─── Rich report card (post-submission) ──────────────────────────────────────
@Composable
fun RichReportCard(
    report: RoadReport,
    sla: SlaInfo,
    authority: AuthorityMapping,
    contractor: Contractor?,
    onEscalate: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Header row ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(report.severity.color(), report.severity.color().copy(0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(report.type.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(report.type.label, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SeverityBadge(report.severity)
                        StatusBadge(report.status)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        report.id,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (report.verifiedByAi) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, null, Modifier.size(12.dp), tint = EmeraldGreen)
                            Spacer(Modifier.width(2.dp))
                            Text("AI Verified", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = DividerSoft)
            Spacer(Modifier.height(12.dp))

            // ── SLA countdown ─────────────────────────────────────────────────
            SlaCountdownRow(sla)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerSoft)
            Spacer(Modifier.height(12.dp))

            // ── Authority escalation path ─────────────────────────────────────
            EscalationPath(report.authorityLevel, authority)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerSoft)
            Spacer(Modifier.height(12.dp))

            // ── Contractor info ────────────────────────────────────────────────
            contractor?.let {
                ContractorRow(it)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerSoft)
                Spacer(Modifier.height(12.dp))
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelMedium)
                }

                val canEscalate = report.authorityLevel != AuthorityLevel.STATE &&
                        report.status != ReportStatus.RESOLVED

                Button(
                    onClick = onEscalate,
                    enabled = canEscalate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Escalate", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (report.escalationCount > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠️ Escalated ${report.escalationCount}× — now at ${report.authorityLevel.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarnAmber,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── SLA countdown row ────────────────────────────────────────────────────────
@Composable
fun SlaCountdownRow(sla: SlaInfo) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (sla.isBreached) Icons.Default.Warning else Icons.Default.Timer,
            null,
            tint = if (sla.isBreached) AlertRed else WarnAmber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "SLA: ${sla.slaDays}d resolution target",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                sla.urgencyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (sla.isBreached) AlertRed else TextSecondary
            )
        }
        if (!sla.isBreached) {
            CountdownChip(
                hours = sla.remainingHours,
                days = sla.remainingDays
            )
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AlertRed.copy(0.12f)
            ) {
                Text(
                    "SLA Breached",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun CountdownChip(hours: Long, days: Long) {
    val text = when {
        days >= 1 -> "${days}d ${hours % 24}h left"
        hours >= 1 -> "${hours}h left"
        else -> "< 1h left"
    }
    val color = when {
        days >= 2 -> EmeraldGreen
        hours >= 12 -> WarnAmber
        else -> AlertRed
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.12f)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ─── Escalation path visualizer ───────────────────────────────────────────────
@Composable
fun EscalationPath(
    current: AuthorityLevel,
    authority: AuthorityMapping
) {
    val levels = AuthorityLevel.entries
    val currentIndex = levels.indexOf(current)

    Column {
        Text("Authority Escalation Path", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            levels.forEachIndexed { index, level ->
                val isActive = index == currentIndex
                val isPassed = index < currentIndex
                val isNext = index == currentIndex + 1

                // Node
                Box(
                    Modifier
                        .size(if (isActive) 36.dp else 28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> Brush.linearGradient(listOf(PrimaryBlue, EmeraldGreen))
                                isPassed -> Brush.linearGradient(listOf(EmeraldGreen, EmeraldGreen))
                                else -> Brush.linearGradient(listOf(DividerSoft, DividerSoft))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.White)
                    } else {
                        Text(
                            "${index + 1}",
                            color = if (isActive) Color.White else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Connector
                if (index < levels.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (isPassed) EmeraldGreen else DividerSoft)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Labels
        Row(Modifier.fillMaxWidth()) {
            levels.forEachIndexed { index, level ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        level.label.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == currentIndex) PrimaryBlue else TextSecondary,
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Current authority detail
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(0.06f))
        ) {
            Row(
                Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(18.dp), tint = PrimaryBlue)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(authority.wardName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(authority.wardOfficerName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Phone, null, Modifier.size(14.dp), tint = EmeraldGreen)
                    Spacer(Modifier.width(4.dp))
                    Text(authority.wardPhone, style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                }
            }
        }
    }
}

// ─── Contractor info row ──────────────────────────────────────────────────────
@Composable
fun ContractorRow(contractor: Contractor) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(WarnAmber.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Construction, null, Modifier.size(20.dp), tint = WarnAmber)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(contractor.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = WarnAmber)
                Text(
                    " ${contractor.rating} • ~${contractor.averageResponseHours}h response",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Icon(Icons.Default.Phone, null, Modifier.size(14.dp), tint = EmeraldGreen)
            Spacer(Modifier.width(4.dp))
            Text(contractor.phone, style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
        }
    }
}

// ─── Severity badge ───────────────────────────────────────────────────────────
@Composable
fun SeverityBadge(severity: Severity) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = severity.color().copy(0.15f)
    ) {
        Text(
            severity.label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = severity.color(),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ─── Status badge ─────────────────────────────────────────────────────────────
@Composable
fun StatusBadge(status: ReportStatus) {
    val color = when (status) {
        ReportStatus.RESOLVED -> EmeraldGreen
        ReportStatus.IN_PROGRESS, ReportStatus.ASSIGNED -> WarnAmber
        ReportStatus.ESCALATED -> AlertRed
        ReportStatus.REJECTED -> TextSecondary
        else -> PrimaryBlue
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(0.12f)) {
        Text(
            status.label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ─── Report list card (tracker) ───────────────────────────────────────────────
@Composable
fun ReportListCard(
    report: RoadReport,
    sla: SlaInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(1.dp),
        border = if (sla.isBreached)
            androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(0.4f))
        else null
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(report.severity.color().copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(report.type.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        report.type.label,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    if (report.verifiedByAi) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.SmartToy, null, Modifier.size(12.dp), tint = EmeraldGreen)
                    }
                }
                Text(
                    report.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SeverityBadge(report.severity)
                    StatusBadge(report.status)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                // SLA indicator
                Text(
                    sla.urgencyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sla.isBreached) AlertRed else TextSecondary,
                    maxLines = 1
                )
                if (report.upvotes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, null, Modifier.size(10.dp), tint = TextSecondary)
                        Text(
                            " ${report.upvotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    Modifier.size(16.dp),
                    tint = TextSecondary
                )
            }
        }
    }
}

// ─── Submission success card ──────────────────────────────────────────────────
@Composable
fun SubmissionSuccessCard(
    report: RoadReport,
    sla: SlaInfo,
    onTrack: () -> Unit,
    onNewReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(0.06f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(0.3f)),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                Modifier.size(56.dp),
                tint = EmeraldGreen
            )
            Spacer(Modifier.height(8.dp))
            Text("Report Submitted!", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                report.id,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "SLA: ${sla.slaDays} day(s) resolution target",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                sla.urgencyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = WarnAmber
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onNewReport, Modifier.weight(1f)) {
                    Text("New Report")
                }
                Button(
                    onClick = onTrack,
                    Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Track")
                }
            }
        }
    }
}

// ─── Report type selector tile ────────────────────────────────────────────────
@Composable
fun ReportTypeTile(
    type: ReportType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.surface
    Card(
        onClick = onClick,
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryBlue.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue)
        else null
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(type.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                type.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ─── Upvote + share row ───────────────────────────────────────────────────────
@Composable
fun UpvoteRow(upvotes: Int, onUpvote: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onUpvote) {
            Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("$upvotes Upvotes")
        }
    }
}
