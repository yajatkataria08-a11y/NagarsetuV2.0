package com.nagarsetu.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.dashboard.domain.model.*

/**
 * Citizen complaint tracker card — shows recent complaint with status chip.
 */
@Composable
fun ComplaintTrackerCard(complaint: CitizenComplaint, modifier: Modifier = Modifier) {
    val (statusColor, statusLabel) = when (complaint.status) {
        ComplaintStatus.PENDING     -> WarnAmber to "Pending"
        ComplaintStatus.IN_PROGRESS -> PrimaryBlue to "In Progress"
        ComplaintStatus.RESOLVED    -> EmeraldGreen to "Resolved"
        ComplaintStatus.REJECTED    -> AlertRed to "Rejected"
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Category icon
            Box(
                Modifier
                    .size(44.dp)
                    .background(statusColor.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(complaint.category),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    complaint.category,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    complaint.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    complaint.wardName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(0.7f)
                )
            }

            AssistChip(
                onClick = {},
                label = { Text(statusLabel, style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = statusColor.copy(0.15f),
                    labelColor = statusColor
                )
            )
        }
    }
}

/**
 * Budget transparency card with gradient bar.
 */
@Composable
fun BudgetCard(item: BudgetTransparencyItem, modifier: Modifier = Modifier) {
    val utilization = if (item.allocated > 0) item.spent.toFloat() / item.allocated else 0f
    val barColor = when {
        utilization >= 0.9f -> WarnAmber
        utilization >= 0.6f -> PrimaryBlue
        else                -> EmeraldGreen
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.category, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${item.projects} projects",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "₹${item.spent / 100_000}L spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = barColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "of ₹${item.allocated / 100_000}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(4.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(barColor.copy(0.1f), RoundedCornerShape(50))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(utilization.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(barColor.copy(0.8f), barColor)),
                            RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

private fun categoryIcon(category: String) = when (category.lowercase()) {
    "pothole", "road"    -> Icons.Default.Report
    "streetlight"        -> Icons.Default.Lightbulb
    "garbage", "waste"   -> Icons.Default.Delete
    "water", "water supply" -> Icons.Default.WaterDrop
    "drain"              -> Icons.Default.WaterDrop
    else                 -> Icons.Default.Build
}
