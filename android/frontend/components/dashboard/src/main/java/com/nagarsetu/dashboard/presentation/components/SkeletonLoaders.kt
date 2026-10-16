package com.nagarsetu.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.DividerSoft

/** Shimmer skeleton placeholder for cards while data is loading. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val shimmerColors = listOf(
        DividerSoft.copy(alpha = 0.6f),
        DividerSoft.copy(alpha = 0.2f),
        DividerSoft.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/** Full dashboard skeleton shown during initial load. */
@Composable
fun DashboardSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Hero banner skeleton
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(100.dp),
            RoundedCornerShape(20.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Mode switcher skeleton
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                ShimmerBox(Modifier.weight(1f).height(64.dp), RoundedCornerShape(14.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        // Map skeleton
        ShimmerBox(
            Modifier.fillMaxWidth().height(200.dp),
            RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Card skeletons
        repeat(3) {
            ShimmerBox(
                Modifier.fillMaxWidth().height(72.dp).padding(vertical = 4.dp),
                RoundedCornerShape(14.dp)
            )
        }
    }
}
