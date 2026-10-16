package com.nagarsetu.roadwatch.presentation.tracker

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.components.*
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.roadwatch.domain.model.*
import com.nagarsetu.roadwatch.presentation.RoadWatchViewModel
import com.nagarsetu.roadwatch.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackTab(viewModel: RoadWatchViewModel) {
    val reports by viewModel.filteredReports.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val detailReport by viewModel.detailReport.collectAsState()

    // Detail bottom sheet
    detailReport?.let { report ->
        val sla = viewModel.slaFor(report)
        val authority = viewModel.authorityFor(report)
        val contractor = viewModel.contractorFor(report)

        ModalBottomSheet(onDismissRequest = { viewModel.closeDetail() }) {
            LazyColumn(
                Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Report Detail",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                item {
                    RichReportCard(
                        report = report,
                        sla = sla,
                        authority = authority,
                        contractor = contractor,
                        onEscalate = { viewModel.escalateReport(report.id) },
                        onShare = {}
                    )
                }
                item { StatusTimeline(report) }
            }
        }
    }

    // Escalation snackbar
    val escalationMsg by viewModel.escalationEvent.collectAsState(initial = "")
    if (escalationMsg.isNotBlank()) {
        LaunchedEffect(escalationMsg) {
            // Snackbar handled by scaffold host in parent
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Search bar ─────────────────────────────────────────────────────
        OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search by ID, type, description…", color = NagarSetuColors.TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = NagarSetuColors.Accent) },
            trailingIcon = if (filter.searchQuery.isNotBlank()) {
                { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null, Modifier.size(16.dp), tint = NagarSetuColors.TextSecondary) } }
            } else null,
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = NagarSetuColors.TextPrimary,
                unfocusedTextColor = NagarSetuColors.TextPrimary,
                focusedBorderColor = NagarSetuColors.Accent,
                unfocusedBorderColor = NagarSetuColors.SurfaceVariant
            )
        )

        // ── Status filter chips ────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = filter.statusFilter == null && filter.typeFilter == null,
                    onClick = { viewModel.clearFilters() },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NagarSetuColors.Accent.copy(0.15f),
                        selectedLabelColor = NagarSetuColors.Accent
                    )
                )
            }
            items(ReportStatus.entries) { status ->
                val color = when (status) {
                    ReportStatus.RESOLVED -> EmeraldGreen
                    ReportStatus.IN_PROGRESS, ReportStatus.ASSIGNED -> WarnAmber
                    ReportStatus.ESCALATED -> AlertRed
                    else -> PrimaryBlue
                }
                FilterChip(
                    selected = filter.statusFilter == status,
                    onClick = {
                        viewModel.setStatusFilter(if (filter.statusFilter == status) null else status)
                    },
                    label = { Text(status.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(0.15f),
                        selectedLabelColor = color
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Summary stats ──────────────────────────────────────────────────
        val allList by viewModel.allReports.collectAsState()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip("${allList.size} Total", PrimaryBlue)
            StatChip("${allList.count { it.status == ReportStatus.IN_PROGRESS }} Active", WarnAmber)
            StatChip("${allList.count { it.status == ReportStatus.RESOLVED }} Done", EmeraldGreen)
            StatChip("${allList.count { it.status == ReportStatus.ESCALATED }} Escalated", AlertRed)
        }

        Spacer(Modifier.height(6.dp))

        // ── Report list ────────────────────────────────────────────────────
        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No reports found", fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                    Text("Try adjusting your filters", color = NagarSetuColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${reports.size} report${if (reports.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(reports, key = { it.id }) { report ->
                    val sla = viewModel.slaFor(report)
                    ReportListCard(
                        report = report,
                        sla = sla,
                        onClick = { viewModel.openDetail(report.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Status timeline ──────────────────────────────────────────────────────────
@Composable
fun StatusTimeline(report: RoadReport) {
    val allStatuses = ReportStatus.entries
    val currentIndex = allStatuses.indexOf(report.status).coerceAtLeast(0)

    Card(
        Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Status Timeline", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            allStatuses.forEachIndexed { index, status ->
                val isPast = index < currentIndex
                val isCurrent = index == currentIndex
                val color = when {
                    isCurrent -> when (status) {
                        ReportStatus.RESOLVED -> EmeraldGreen
                        ReportStatus.ESCALATED -> AlertRed
                        ReportStatus.REJECTED -> TextSecondary
                        else -> PrimaryBlue
                    }
                    isPast -> EmeraldGreen
                    else -> DividerSoft
                }
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = color.copy(if (isCurrent || isPast) 1f else 0.2f),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isPast)
                                    Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.White)
                                else if (isCurrent)
                                    Icon(Icons.Default.Circle, null, Modifier.size(8.dp), tint = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                        if (index < allStatuses.lastIndex) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                            ) {
                                HorizontalDivider(
                                    Modifier.fillMaxHeight().width(2.dp),
                                    color = if (isPast) EmeraldGreen else DividerSoft
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(bottom = 20.dp)) {
                        Text(
                            status.label,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) color else if (isPast) MaterialTheme.colorScheme.onSurface else TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isCurrent) {
                            Text("Current status", style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
