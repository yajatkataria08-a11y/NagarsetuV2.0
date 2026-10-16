package com.nagarsetu.drivelegal.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.nagarsetu.drivelegal.domain.model.*

// ─── Fine result card (single violation) ─────────────────────────────────────
@Composable
fun FineCard(
    fine: FineCalculation,
    modifier: Modifier = Modifier,
    onMvActClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(0.06f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(AlertRed, AlertRedDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fine.violation.emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(fine.violation.label, fontWeight = FontWeight.Bold)
                    Text(
                        "Section ${fine.mvActSection} • ${fine.vehicleCategory.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                // Fine badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRed.copy(0.12f)
                ) {
                    Text(
                        fine.formattedTotal,
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.ExtraBold,
                        color = AlertRed,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerSoft)
            Spacer(Modifier.height(10.dp))

            // Breakdown row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FineBreakdownItem("Base Fine", "${fine.currencySymbol}${fine.baseAmount.toLong()}")
                FineBreakdownItem("Vehicle", "×${fine.vehicleMultiplier}")
                FineBreakdownItem("Repeat", "×${fine.offenceMultiplier.toInt()}")
                FineBreakdownItem("Total", fine.formattedTotal, highlight = true)
            }

            // State note
            fine.stateNote?.let { note ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = WarnAmber,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // MV Act link
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onMvActClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(14.dp), tint = PrimaryBlue)
                Spacer(Modifier.width(4.dp))
                Text(
                    "MV Act Section ${fine.mvActSection} — echallan.parivahan.gov.in",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun FineBreakdownItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (highlight) AlertRed else MaterialTheme.colorScheme.onSurface,
            fontSize = if (highlight) 15.sp else 13.sp
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ─── Multi-violation result card ──────────────────────────────────────────────
@Composable
fun MultiViolationCard(result: MultiViolationResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = WarnAmber.copy(0.08f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, null, tint = AlertRed, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Multi-Violation Summary", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${result.currencySymbol}${result.grandTotal.toLong()}",
                    fontWeight = FontWeight.ExtraBold,
                    color = AlertRed,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            result.violations.take(3).forEachIndexed { index, calc ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${index + 1}. ${calc.violation.emoji} ${calc.violation.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        calc.formattedTotal,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertRed
                    )
                }
            }
            if (result.violations.size > 3) {
                Text(
                    "+${result.violations.size - 3} more violations",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Grand Total: ${result.currencySymbol}${result.grandTotal.toLong()}",
                    fontWeight = FontWeight.ExtraBold,
                    color = AlertRed
                )
            }
        }
    }
}

// ─── Violation chip (for calculator multi-select) ────────────────────────────
@Composable
fun ViolationChip(
    violation: ViolationType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                "${violation.emoji} ${violation.label}",
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AlertRed.copy(0.15f),
            selectedLabelColor = AlertRed
        )
    )
}

// ─── Vehicle selector chip ────────────────────────────────────────────────────
@Composable
fun VehicleCategoryChip(
    category: VehicleCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${category.emoji} ${category.label}", style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldGreen.copy(0.18f),
            selectedLabelColor = EmeraldGreen
        )
    )
}

// ─── Chat message bubble ──────────────────────────────────────────────────────
@Composable
fun ChatBubble(message: DriveLegalMessage, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // User bubble (right)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                color = PrimaryBlue,
                shadowElevation = 1.dp
            ) {
                Text(
                    message.userText,
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Bot bubble (left) with optional fine card
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            // Bot avatar
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(EmeraldGreen, PrimaryBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text("⚖️", fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                    color = SurfaceWhite,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        message.botText,
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Inline fine mini-card if detected
                message.fineCalculation?.let { fine ->
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertRed.copy(0.07f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(0.25f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fine.violation.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    fine.violation.label,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Section ${fine.mvActSection}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                fine.formattedTotal,
                                fontWeight = FontWeight.ExtraBold,
                                color = AlertRed
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── OCR result card ──────────────────────────────────────────────────────────
@Composable
fun OcrResultCard(
    result: OcrScanResult,
    onDismiss: () -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuspectFraud) AlertRed.copy(0.08f) else EmeraldGreen.copy(0.06f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (result.isSuspectFraud) Icons.Default.Warning else Icons.Default.CheckCircle,
                    null,
                    tint = if (result.isSuspectFraud) AlertRed else EmeraldGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (result.isSuspectFraud) "⚠️ Possible Fraud Detected" else "✅ Scan Successful",
                    fontWeight = FontWeight.Bold,
                    color = if (result.isSuspectFraud) AlertRed else EmeraldGreen
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = TextSecondary)
                }
            }

            val fraudReason = result.fraudReason
            if (result.isSuspectFraud && fraudReason != null) {
                Spacer(Modifier.height(6.dp))
                Text(fraudReason, color = AlertRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            OcrFieldRow("Vehicle No.", result.vehicleNumber)
            OcrFieldRow("Fine Amount", result.fineAmount)
            OcrFieldRow("Date", result.date)
            OcrFieldRow("MV Section", result.section)
            OcrFieldRow("Authority", result.authority)

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Calculate, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Calculate Fine from Scan")
            }
        }
    }
}

@Composable
private fun OcrFieldRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Country selector chip ────────────────────────────────────────────────────
@Composable
fun CountryChip(
    country: com.nagarsetu.drivelegal.domain.config.DriveLegalConfig.CountryInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                "${country.flag} ${country.name}",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryBlue.copy(0.18f),
            selectedLabelColor = PrimaryBlue
        )
    )
}
