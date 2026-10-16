package com.nagarsetu.predictive.presentation.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.predictive.domain.model.Forecast
import com.nagarsetu.predictive.domain.model.PredictionType
import com.nagarsetu.predictive.domain.model.RiskLevel

/**
 * Animated forecast card with:
 * - Color-coded risk gradient background
 * - Pulsing severity ring for HIGH/SEVERE
 * - Risk probability percentage
 * - ML confidence badge
 */
@Composable
fun ForecastCard(
    forecast: Forecast,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val (color, gradient) = riskVisuals(forecast.riskLevel)
    val icon = predictionIcon(forecast.type)

    // Scale animation on selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Pulse for SEVERE/HIGH
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        0.3f, 0.9f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pa"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .width(108.dp)
    ) {
        // Pulse ring for high/severe
        if (forecast.riskLevel == RiskLevel.SEVERE || forecast.riskLevel == RiskLevel.HIGH) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = pulseAlpha * 0.2f))
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) color else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(color.copy(0.15f), Color.Transparent))
                    )
            ) {
                Column(
                    Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Day label
                    Text(
                        forecast.day,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(Modifier.height(6.dp))

                    // Risk icon in colored circle
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(color.copy(0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = forecast.type.label,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Probability
                    Text(
                        "${(forecast.probability * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )

                    // Risk level chip
                    Box(
                        Modifier
                            .background(color.copy(0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${forecast.riskLevel.emoji} ${forecast.riskLevel.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        forecast.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    // ML confidence
                    Text(
                        "ML: ${(forecast.mlConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary.copy(0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Summary top-risk card used in the overview section.
 */
@Composable
fun TopRiskSummaryCard(forecast: Forecast, modifier: Modifier = Modifier) {
    val (color, _) = riskVisuals(forecast.riskLevel)
    val icon = predictionIcon(forecast.type)

    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(color.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(forecast.type.label, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge)
                Text("${(forecast.probability * 100).toInt()}% — ${forecast.hotspot.take(22)}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            Text(
                forecast.riskLevel.emoji,
                fontSize = 20.sp
            )
        }
    }
}

// ── Visuals helpers ────────────────────────────────────────────────────────────
private fun riskVisuals(level: RiskLevel): Pair<Color, List<Color>> = when (level) {
    RiskLevel.LOW      -> EmeraldGreen to listOf(EmeraldGreen, EcoGreen)
    RiskLevel.MODERATE -> WarnAmber to listOf(WarnAmber, Color(0xFFE65100))
    RiskLevel.HIGH     -> Color(0xFFFF6D00) to listOf(Color(0xFFFF6D00), WarnAmber)
    RiskLevel.SEVERE   -> AlertRed to listOf(AlertRed, Color(0xFFC62828))
}

fun predictionIcon(type: PredictionType): ImageVector = when (type) {
    PredictionType.ACCIDENT -> Icons.Default.CarCrash
    PredictionType.FLOOD    -> Icons.Default.Water
    PredictionType.CRIME    -> Icons.Default.Shield
    PredictionType.HEALTH   -> Icons.Default.LocalHospital
}
