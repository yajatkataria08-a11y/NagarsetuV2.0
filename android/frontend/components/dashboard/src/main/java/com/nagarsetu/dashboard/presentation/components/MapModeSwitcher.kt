package com.nagarsetu.dashboard.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.dashboard.domain.model.MapMode

/**
 * Horizontal mode switcher with flash animation on selection.
 * Each mode has a distinctive icon, color, and label.
 */
@Composable
fun MapModeSwitcher(
    selected: MapMode,
    onSelect: (MapMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapMode.entries.forEach { mode ->
            MapModeTab(
                mode = mode,
                isSelected = selected == mode,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MapModeTab(
    mode: MapMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Flash animation on selection
    val flashAnim = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            flashAnim.animateTo(1f, tween(120))
            flashAnim.animateTo(0.85f, tween(80))
        }
    }

    val scale by animateFloatAsState(
        if (isSelected) 1f else 0.92f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val (icon, tint, gradient) = modeVisuals(mode)

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected)
                    Brush.linearGradient(gradient)
                else
                    Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    ))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.label,
                tint = if (isSelected) Color.White else tint.copy(0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = mode.label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}

private data class ModeVisuals(val icon: ImageVector, val tint: Color, val gradient: List<Color>)

private fun modeVisuals(mode: MapMode): ModeVisuals = when (mode) {
    MapMode.EMERGENCY  -> ModeVisuals(Icons.Default.LocalHospital,  AlertRed,      listOf(AlertRed, Color(0xFFC62828)))
    MapMode.CIVIC      -> ModeVisuals(Icons.Default.AccountBalance,  PrimaryBlue,   listOf(PrimaryBlue, CivicBlue))
    MapMode.ROAD_WATCH -> ModeVisuals(Icons.Default.Warning,         WarnAmber,     listOf(WarnAmber, Color(0xFFE65100)))
    MapMode.AUTHORITY  -> ModeVisuals(Icons.Default.AdminPanelSettings, EmeraldGreen,listOf(EmeraldGreen, EcoGreen))
    MapMode.PARKING     -> ModeVisuals(Icons.Default.LocalParking,  Color(0xFF1976D2), listOf(Color(0xFF1976D2), Color(0xFF1565C0)))
    MapMode.HOSPITALS   -> ModeVisuals(Icons.Default.LocalHospital, AlertRed,      listOf(AlertRed, Color(0xFFC62828)))
    MapMode.EV_CHARGING -> ModeVisuals(Icons.Default.EvStation,     EcoGreen,      listOf(EcoGreen, Color(0xFF1B5E20)))
    MapMode.TRAFFIC     -> ModeVisuals(Icons.Default.Traffic,       Color(0xFF7B1FA2), listOf(Color(0xFF7B1FA2), Color(0xFF4A148C)))
}
