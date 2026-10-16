package com.nagarsetu.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MapLayerSwitcher(
    activeLayer: MapLayer,
    showTraffic: Boolean,
    showTransit: Boolean,
    onLayerChange: (MapLayer) -> Unit,
    onTrafficToggle: () -> Unit,
    onTransitToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expanded layer options
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Layer tiles
                ALL_MAP_LAYERS.forEach { config ->
                    LayerChip(
                        config = config,
                        isActive = activeLayer == config.layer,
                        onClick = {
                            onLayerChange(config.layer)
                            expanded = false
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.width(160.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Traffic toggle (Google only)
                if (activeLayer != MapLayer.OSM_STANDARD) {
                    ToggleChip(
                        label = "Live Traffic",
                        icon = Icons.Outlined.Traffic,
                        checked = showTraffic,
                        onToggle = onTrafficToggle
                    )
                    ToggleChip(
                        label = "Transit Lines",
                        icon = Icons.Outlined.DirectionsBus,
                        checked = showTransit,
                        onToggle = onTransitToggle
                    )
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                if (expanded) Icons.Outlined.Close else Icons.Outlined.Layers,
                contentDescription = "Switch map layer"
            )
        }
    }
}

@Composable
private fun LayerChip(
    config: MapLayerConfig,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                config.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface
            )
            Text(
                config.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
            }
        }
    }
}
