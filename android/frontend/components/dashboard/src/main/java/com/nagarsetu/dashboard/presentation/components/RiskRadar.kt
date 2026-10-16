package com.nagarsetu.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.dashboard.domain.model.AlertType
import com.nagarsetu.dashboard.domain.model.RadarAlert
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated rotating radar SVG-style canvas component.
 * Shows proximity alerts as blips at their compass bearing.
 */
@Composable
fun RiskRadar(
    alerts: List<RadarAlert>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    // Blip pulse
    val blipAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "blip"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f
            val gridColor = EmeraldGreen.copy(alpha = 0.25f)
            val sweepColor = EmeraldGreen.copy(alpha = 0.6f)

            // Background circles
            for (i in 1..4) {
                drawCircle(
                    color = gridColor,
                    radius = maxR * i / 4f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f)
                )
            }

            // Cross-hairs
            drawLine(gridColor, Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.5f)

            // Sweep arc
            rotate(sweepAngle, Offset(cx, cy)) {
                drawArc(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.25f to sweepColor.copy(0.8f),
                        1f to Color.Transparent,
                        center = Offset(cx, cy)
                    ),
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(cx - maxR, cy - maxR),
                    size = androidx.compose.ui.geometry.Size(maxR * 2, maxR * 2)
                )
            }

            // Blips for each alert
            alerts.forEach { alert ->
                val bearingRad = Math.toRadians(alert.bearing.toDouble() - 90)
                // Map distance (max 2km) to radar radius
                val r = (alert.distanceMeters.coerceAtMost(2000) / 2000f) * maxR * 0.9f
                val bx = cx + r * cos(bearingRad).toFloat()
                val by = cy + r * sin(bearingRad).toFloat()
                val blipColor = alertTypeColor(alert.type)
                // Glow ring
                drawCircle(
                    color = blipColor.copy(alpha = blipAlpha * 0.3f),
                    radius = 12f,
                    center = Offset(bx, by)
                )
                // Core blip
                drawCircle(
                    color = blipColor.copy(alpha = blipAlpha),
                    radius = 5f,
                    center = Offset(bx, by)
                )
            }

            // Center dot
            drawCircle(color = EmeraldGreen, radius = 4f, center = Offset(cx, cy))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${alerts.size} proximity alerts",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun alertTypeColor(type: AlertType): Color = when (type) {
    AlertType.HAZARD, AlertType.SLA_BREACH, AlertType.NEW_CRISIS -> AlertRed
    AlertType.TRAFFIC  -> WarnAmber
    AlertType.HEALTH   -> Color(0xFFE91E63)
    AlertType.CIVIC    -> PrimaryBlue
    else               -> EmeraldGreen
}
