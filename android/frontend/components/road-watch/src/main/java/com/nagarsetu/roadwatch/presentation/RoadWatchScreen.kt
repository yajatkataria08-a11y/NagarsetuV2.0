package com.nagarsetu.roadwatch.presentation

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.components.NagarSetuScreenBackground
import com.nagarsetu.core.ui.components.NagarSetuTopBar
import com.nagarsetu.core.ui.strings.AppStrings
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.theme.PrimaryBlue
import com.nagarsetu.roadwatch.presentation.heatmap.HeatmapTab
import com.nagarsetu.roadwatch.presentation.report.ReportTab
import com.nagarsetu.roadwatch.presentation.tracker.TrackTab

@Composable
fun RoadWatchScreen(
    viewModel: RoadWatchViewModel = hiltViewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val s = LocalAppStrings.current

    NagarSetuScreenBackground {
        Column(Modifier.fillMaxSize()) {
            // Header
            NagarSetuTopBar(
                title = s.roadWatch.roadWatchTitle,
                subtitle = s.roadWatch.roadWatchSub
            )

            // Tab Switcher
            RoadWatchTabRow(
                s = s,
                selectedTab = activeTab,
                onTabSelected = { viewModel.setTab(it) }
            )

            Spacer(Modifier.height(8.dp))

            // Tab Content
            Box(Modifier.weight(1f)) {
                when (activeTab) {
                    RoadWatchTab.REPORT -> ReportTab(viewModel)
                    RoadWatchTab.TRACK -> TrackTab(viewModel)
                    2 -> HeatmapTab(viewModel) // Heatmap is tab index 2
                    else -> ReportTab(viewModel)
                }

                // Professional Branding Watermark
                AppWatermark(
                    alignment = Alignment.BottomEnd,
                    opacity = 0.12f,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun RoadWatchTabRow(
    s: AppStrings,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Triple(RoadWatchTab.REPORT, s.roadWatch.tabReport, Icons.Default.AddAPhoto),
        Triple(RoadWatchTab.TRACK, s.roadWatch.tabTrack, Icons.Default.Assessment),
        Triple(2, s.roadWatch.tabHeatmap, Icons.Default.Map)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = NagarSetuColors.Surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (index, label, icon) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NagarSetuColors.Accent else Color.Transparent)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else NagarSetuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else NagarSetuColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
