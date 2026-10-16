/**
 * Theme.kt — Consolidated Dark-Indigo Design System
 *
 * Single source of truth for every colour, typography, and shape token
 * used across all NagarSetu modules.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  MERGE PROVENANCE                                                       │
 * │  • NagarSetuColors object + dark scheme ──→ BestMerge (superior)       │
 * │  • Semantic aliases (AlertRed, Emerald…)  ──→ Color.kt both projects   │
 * │  • Constant coordinates                   ──→ BestMerge Color.kt       │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
package com.nagarsetu.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp


// ══════════════════════════════════════════════════════════════════════════════
// Colour tokens
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Primary NagarSetu palette.
 *
 * Access from Composable code via [NagarSetuColors.Accent] etc., or via
 * the top-level aliases below (e.g. [AlertRed], [EmeraldGreen]) which are
 * imported in individual screen files.
 */
object NagarSetuColors {
    // Backgrounds / surfaces
    val Background     = Color(0xFFF8FAFC)   // Refined clean background
    val Surface        = Color(0xFFFFFFFF)   // Pure white surfaces
    val SurfaceVariant = Color(0xFFF1F5F9)   // Slate-tinted surface variant

    // Theme Colors
    val Primary        = Color(0xFF1A1A3E)   // Deep Navy Blue
    val Accent         = Color(0xFF4B6EAF)   // Slate Blue
    val ElectricBlue   = Color(0xFF00D4FF)   // Brand highlight (from icon)

    // Semantic
    val SOSRed         = Color(0xFFE53935)
    val SuccessGreen   = Color(0xFF10B981)   // Emerald Green
    val WarningOrange  = Color(0xFFF59E0B)   // Amber Orange

    // Typography (High Contrast)
    val TextPrimary    = Color(0xFF0F172A)   // Deep Slate (near black)
    val TextSecondary  = Color(0xFF475569)   // Dark Muted Slate (readable)
    val TextDisabled   = Color(0xFF94A3B8)   // Light Slate
    
    // Legacy support
    val DarkText       = TextPrimary
    val LightText      = Color(0xFFF8FAFC)
}

/**
 * Evaluates the luminance of a color to return the optimal high-contrast text color.
 * Fail-safe implementation for dynamic backgrounds.
 */
fun Color.contentColor(): Color = if (this.luminance() > 0.45f) NagarSetuColors.DarkText else NagarSetuColors.LightText

// ══════════════════════════════════════════════════════════════════════════════
// Material3 colour scheme
// ══════════════════════════════════════════════════════════════════════════════

private val AppColorScheme = lightColorScheme(
    primary          = NagarSetuColors.Primary,
    onPrimary        = NagarSetuColors.LightText,
    secondary        = NagarSetuColors.Accent,
    onSecondary      = NagarSetuColors.LightText,
    tertiary         = NagarSetuColors.ElectricBlue,
    background       = NagarSetuColors.Background,
    surface          = NagarSetuColors.Surface,
    onBackground     = NagarSetuColors.DarkText,
    onSurface        = NagarSetuColors.DarkText,
    error            = NagarSetuColors.SOSRed,
    onError          = Color.White,
    surfaceVariant   = NagarSetuColors.SurfaceVariant,
    onSurfaceVariant = NagarSetuColors.TextSecondary,
)


// ══════════════════════════════════════════════════════════════════════════════
// Typography
// ══════════════════════════════════════════════════════════════════════════════

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,      letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleLarge    = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium   = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleSmall    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,    lineHeight = 20.sp),
    bodyMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal,    lineHeight = 18.sp),
    bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,    letterSpacing = 0.5.sp),
)


// ══════════════════════════════════════════════════════════════════════════════
// Shapes (used via CardShape, PillShape in DesignSystem.kt)
// ══════════════════════════════════════════════════════════════════════════════


// ══════════════════════════════════════════════════════════════════════════════
// Theme composable
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Root theme wrapper. Applies the dark-indigo Material3 scheme and the
 * NagarSetu typography scale to the entire composition tree.
 *
 * Usage in [MainActivity]:
 * ```
 * NagarSetuTheme { AppShell() }
 * ```
 */
@Composable
fun NagarSetuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
