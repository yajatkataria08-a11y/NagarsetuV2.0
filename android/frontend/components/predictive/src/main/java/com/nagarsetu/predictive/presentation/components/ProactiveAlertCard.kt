package com.nagarsetu.predictive.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.AlertRed
import com.nagarsetu.core.ui.theme.EcoGreen
import com.nagarsetu.core.ui.theme.EmeraldGreen
import com.nagarsetu.core.ui.theme.TextSecondary
import com.nagarsetu.core.ui.theme.WarnAmber
import com.nagarsetu.predictive.domain.model.ProactiveAlert
import com.nagarsetu.predictive.domain.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Proactive AI alert card with gradient banner matching risk severity.
 * Severe alerts pulse their border for visual urgency.
 */
@Composable
fun ProactiveAlertCard(
    alert: ProactiveAlert,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val (mainColor, gradient) = when (alert.riskLevel) {
        RiskLevel.SEVERE   -> AlertRed       to listOf(AlertRed, Color(0xFFC62828))
        RiskLevel.HIGH     -> Color(0xFFFF6D00) to listOf(Color(0xFFFF6D00), WarnAmber)
        RiskLevel.MODERATE -> WarnAmber      to listOf(WarnAmber, Color(0xFFF9A825))
        RiskLevel.LOW      -> EmeraldGreen   to listOf(EmeraldGreen, EcoGreen)
    }

    val title = if (s.profile.langHindi == "हिन्दी") alert.titleHi.takeIf { it.isNotBlank() } ?: alert.title else alert.title
    val description = if (s.profile.langHindi == "हिन्दी") alert.descriptionHi.takeIf { it.isNotBlank() } ?: alert.description else alert.description
    val area = if (s.profile.langHindi == "हिन्दी") alert.areaHi.takeIf { it.isNotBlank() } ?: alert.area else alert.area
    val recommendation = if (s.profile.langHindi == "हिन्दी") alert.recommendationHi.takeIf { it.isNotBlank() } ?: alert.recommendation else alert.recommendation

    val riskLabel = when(alert.riskLevel) {
        RiskLevel.LOW -> s.predictive.riskLow
        RiskLevel.MODERATE -> s.predictive.riskModerate
        RiskLevel.HIGH -> s.predictive.riskHigh
        RiskLevel.SEVERE -> s.predictive.riskSevere
    }

    // Pulsing border for SEVERE
    val pulse = rememberInfiniteTransition(label = "alert_pulse")
    val pulseAlpha by pulse.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pa"
    )
    val borderAlpha = if (alert.riskLevel == RiskLevel.SEVERE) pulseAlpha else 0.6f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (alert.riskLevel == RiskLevel.SEVERE) 1.5.dp else 0.dp,
            color = mainColor.copy(borderAlpha)
        )
    ) {
        Column {
            // Gradient top bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(gradient))
            )

            Column(Modifier.padding(14.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(mainColor.copy(0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = predictionIcon(alert.type),
                            contentDescription = null,
                            tint = mainColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            area,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    // Risk badge
                    Box(
                        Modifier
                            .background(mainColor.copy(0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${alert.riskLevel.emoji} $riskLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = mainColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Description
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                )

                Spacer(Modifier.height(10.dp))

                // Recommendation chip
                Surface(
                    color = mainColor.copy(0.08f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            null,
                            tint = mainColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            recommendation,
                            style = MaterialTheme.typography.labelSmall,
                            color = mainColor
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Timestamp
                Text(
                    formatAlertTime(alert.timestamp, s),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(0.6f)
                )
            }
        }
    }
}

private fun formatAlertTime(ts: Long, s: com.nagarsetu.core.ui.strings.AppStrings): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 3_600_000  -> {
            val mins = diff / 60_000
            if (s.profile.langHindi == "हिन्दी") "$mins मिनट पहले" else "$mins m ago"
        }
        diff < 86_400_000 -> {
            val hrs = diff / 3_600_000
            if (s.profile.langHindi == "हिन्दी") "$hrs घंटे पहले" else "$hrs h ago"
        }
        else              -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))
    }
}
