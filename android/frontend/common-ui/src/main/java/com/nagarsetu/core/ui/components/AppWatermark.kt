package com.nagarsetu.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.NagarSetuColors

/**
 * Professional App Watermark/Branding Component
 * Designed for branding, screenshots, demos, and hackathon presentations.
 */
@Composable
fun AppWatermark(
    modifier: Modifier = Modifier,
    text: String = "NAGARSETU",
    subtext: String = "AI Civic Intelligence",
    alignment: Alignment = Alignment.BottomEnd,
    opacity: Float = 0.12f
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 8.dp), // Extra padding for bottom alignment
            color = Color.Transparent
        ) {
            Column(
                horizontalAlignment = if (alignment == Alignment.BottomCenter) Alignment.CenterHorizontally else Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NagarSetuColors.Accent.copy(alpha = opacity)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = NagarSetuColors.TextPrimary.copy(alpha = opacity)
                    )
                }
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = NagarSetuColors.TextSecondary.copy(alpha = opacity)
                )
            }
        }
    }
}

/**
 * Reusable Glassmorphism style watermark for Map overlays
 */
@Composable
fun MapBrandedOverlay(
    modifier: Modifier = Modifier,
    opacity: Float = 0.15f
) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationCity,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = NagarSetuColors.Accent.copy(alpha = opacity + 0.1f)
            )
            Text(
                text = "NagarSetu Smart City",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = NagarSetuColors.TextPrimary.copy(alpha = opacity + 0.1f)
            )
        }
    }
}
