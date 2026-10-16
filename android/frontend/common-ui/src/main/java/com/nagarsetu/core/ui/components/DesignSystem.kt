package com.nagarsetu.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.*

@Composable
fun NagarSetuScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NagarSetuColors.Background,
        contentColor = NagarSetuColors.TextPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NagarSetuColors.Background,
                            NagarSetuColors.SurfaceVariant
                        )
                    )
                ),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ModernTopBar(
    title: String,
    subtitle: String,
    onEmergencyClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onThemeClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NagarSetuColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = NagarSetuColors.TextSecondary
                )
            }

            IconButton(
                onClick = onThemeClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
            ) {
                Icon(Icons.Default.Palette, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onAssistantClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onEmergencyClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(AlertRed.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.GppMaybe, null, tint = AlertRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun NagarSetuTopBar(
    title: String,
    subtitle: String = BHOPAL_LOCATION_LABEL,
    showLocation: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLocation) {
            Icon(Icons.Default.LocationOn, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = NagarSetuColors.TextSecondary, maxLines = 1)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NagarSetuColors.TextSecondary)
                }
            }
        }
        trailing()
    }
}

@Composable
fun HeroGradientBanner(
    headline: String,
    subline: String = "Building a Smarter & Safer Bhopal Together",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(PrimaryBlue, CivicBlueLight, Color(0xFF5C6BC0))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(subline, color = Color.White.copy(0.9f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(headline, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeatureModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
            }
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun QuickCategoryCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(88.dp)
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                maxLines = 2,
                color = NagarSetuColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CityProgressCard(total: Int, pending: Int, resolved: Int) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn(Icons.Default.Search, "Total", total)
            StatColumn(Icons.Default.Schedule, "Pending", pending)
            StatColumn(Icons.Default.CheckCircle, "Resolved", resolved)
        }
    }
}

@Composable
private fun StatColumn(icon: ImageVector, label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(22.dp))
        Text("$value", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NagarSetuColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = NagarSetuColors.TextSecondary)
    }
}

@Composable
fun ServiceGridCard(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SosGlowButton(onClick: () -> Unit, sizeDp: Int = 200, label: String = "SOS") {
    val infinite = rememberInfiniteTransition(label = "glow")
    val glow by infinite.animateFloat(
        0.35f, 0.85f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size((sizeDp + 40).dp)
                    .background(AlertRed.copy(alpha = glow * 0.35f), CircleShape)
            )
            Box(
                Modifier
                    .size(sizeDp.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(AlertRed, AlertRedDark)))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Tap in case of emergency", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(label, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun LocationReportCard(
    placeName: String,
    address: String,
    onEdit: () -> Unit = {}
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Report, null, tint = AlertRed)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(placeName, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(address, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            OutlinedButton(onClick = onEdit, colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnAmber)) {
                Text("Edit", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SendReportButton(onClick: () -> Unit, enabled: Boolean = true, text: String = "SEND REPORT") {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .height(56.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(containerColor = AlertRed, disabledContainerColor = AlertRed.copy(0.4f))
    ) {
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, color = EmeraldGreen) }
        }
    }
}

@Composable
fun SearchDestinationField(
    placeholder: String = "Enter destination in Bhopal",
    value: String = "",
    onValueChange: (String) -> Unit = {},
    onSearch: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(placeholder, color = NagarSetuColors.TextSecondary) },
        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = NagarSetuColors.Accent) },
        trailingIcon = {
            IconButton(onClick = onSearch) { Icon(Icons.Default.Search, null, tint = NagarSetuColors.Accent) }
        },
        shape = RoundedCornerShape(50),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NagarSetuColors.Accent,
            unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
            focusedTextColor = NagarSetuColors.TextPrimary,
            unfocusedTextColor = NagarSetuColors.TextPrimary
        )
    )
}

@Composable
fun ChargingHeroBanner(message: String = "Charge your EV easily across Bhopal") {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldGreen)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(message, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Live slots & pricing", color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ElectricBolt, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun CivicReportHeader(onReportClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(PrimaryBlue, CivicBlue)))
            .padding(bottom = 24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(0.9f))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("NagarSetu — Bhopal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Let's fix the city together", color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onReportClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryBlue)
            ) {
                Text("Report an Issue", fontWeight = FontWeight.Bold)
            }
        }
    }
}
