package com.nagarsetu.predictive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.predictive.domain.model.HazardPrediction
import com.nagarsetu.predictive.domain.model.RiskLevel

/**
 * Detailed hazard prediction card showing:
 * - Area, risk type, score gauge
 * - Contributing risk factors as bullet chips
 * - Recommendation text
 * - Day label
 */
@Composable
fun HazardPredictionCard(
    prediction: HazardPrediction,
    modifier: Modifier = Modifier
) {
    val riskColor = when (prediction.riskLevel) {
        RiskLevel.SEVERE   -> AlertRed
        RiskLevel.HIGH     -> Color(0xFFFF6D00)
        RiskLevel.MODERATE -> WarnAmber
        RiskLevel.LOW      -> EmeraldGreen
    }

    val animScore by animateFloatAsState(
        targetValue = prediction.riskScore / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        prediction.area,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${prediction.type.emoji} ${prediction.type.label}  •  ${prediction.dayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Score ring
                Box(
                    Modifier
                        .size(52.dp)
                        .background(riskColor.copy(0.12f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animScore },
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 4.dp,
                        color = riskColor,
                        trackColor = riskColor.copy(0.1f)
                    )
                    Text(
                        "${prediction.riskScore}",
                        fontWeight = FontWeight.ExtraBold,
                        color = riskColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Risk factors ───────────────────────────────────────────────
            Text(
                "Contributing factors:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            prediction.factors.forEach { factor ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(riskColor, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        factor,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Recommendation ─────────────────────────────────────────────
            Surface(
                color = riskColor.copy(0.07f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Recommend,
                        null,
                        tint = riskColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        prediction.recommendation,
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor
                    )
                }
            }
        }
    }
}
