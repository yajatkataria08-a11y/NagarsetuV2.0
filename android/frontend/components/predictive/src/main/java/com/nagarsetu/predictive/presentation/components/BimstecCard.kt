package com.nagarsetu.predictive.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.predictive.domain.model.BimstecCityData

/**
 * BIMSTEC city comparison card showing accident blackspots,
 * flood risk areas, crime hotspots, and health risk index.
 * Visual bar charts for quick at-a-glance comparison.
 */
@Composable
fun BimstecCityCard(
    city: BimstecCityData,
    modifier: Modifier = Modifier
) {
    val (qualityColor, qualityBg) = when (city.dataQuality) {
        "Live"       -> EmeraldGreen to EmeraldGreen.copy(0.12f)
        "Historical" -> PrimaryBlue to PrimaryBlue.copy(0.12f)
        else         -> WarnAmber to WarnAmber.copy(0.12f)
    }

    // Max values for relative bar sizing (normalise to visual scale)
    val maxAccident = 50; val maxFlood = 25; val maxCrime = 35; val maxHealth = 100

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(city.flag, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        city.city,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        city.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                // Data quality badge
                Box(
                    Modifier
                        .background(qualityBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        city.dataQuality,
                        style = MaterialTheme.typography.labelSmall,
                        color = qualityColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // ── Metric bars ────────────────────────────────────────────────
            BimstecMetricBar(
                label = "💥 Accident Blackspots",
                value = city.accidentBlackspots,
                max = maxAccident,
                color = AlertRed
            )
            Spacer(Modifier.height(6.dp))
            BimstecMetricBar(
                label = "🌊 Flood Risk Areas",
                value = city.floodRiskAreas,
                max = maxFlood,
                color = PrimaryBlue
            )
            Spacer(Modifier.height(6.dp))
            BimstecMetricBar(
                label = "🔒 Crime Hotspots",
                value = city.crimeHotspots,
                max = maxCrime,
                color = WarnAmber
            )
            Spacer(Modifier.height(6.dp))
            BimstecMetricBar(
                label = "🏥 Health Risk Index",
                value = city.healthRiskIndex,
                max = maxHealth,
                color = Color(0xFFAD1457)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Last updated: ${city.lastUpdated}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(0.6f)
            )
        }
    }
}

@Composable
private fun BimstecMetricBar(
    label: String,
    value: Int,
    max: Int,
    color: Color
) {
    val fraction = (value.toFloat() / max).coerceIn(0f, 1f)
    val animatedFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction,
        animationSpec = androidx.compose.animation.core.tween(900,
            easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "bar"
    )

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(0.7f), color)),
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Compact BIMSTEC comparison summary — horizontal scroll row of stat chips.
 */
@Composable
fun BimstecCompactRow(cities: List<BimstecCityData>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.take(4).forEach { city ->
            BimstecMiniChip(city = city, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BimstecMiniChip(city: BimstecCityData, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(
            Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(city.flag, fontSize = 20.sp)
            Text(city.city, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${city.accidentBlackspots} 💥",
                style = MaterialTheme.typography.labelSmall, color = AlertRed)
        }
    }
}
