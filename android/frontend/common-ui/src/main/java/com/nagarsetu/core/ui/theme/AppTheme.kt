package com.nagarsetu.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme(val id: String, val displayName: String) {
    CIVIC_LIGHT("civic_light", "Civic Light"),
    CIVIC_DARK("civic_dark", "Civic Dark"),
    ECO_GREEN("eco_green", "Eco Green"),
    SUNSET("sunset", "Sunset Amber"),
    HIGH_CONTRAST("high_contrast", "High Contrast"),
    ROYAL_PURPLE("royal_purple", "Royal Purple");

    companion object {
        fun fromId(id: String?): AppTheme =
            entries.find { it.id == id } ?: CIVIC_LIGHT
    }
}

fun AppTheme.colorScheme(): ColorScheme = when (this) {
    AppTheme.CIVIC_LIGHT -> lightColorScheme(
        primary = PrimaryBlue,
        onPrimary = Color.White,
        secondary = EmeraldGreen,
        tertiary = WarnAmber,
        background = MintBackground,
        onBackground = TextPrimary,
        surface = SurfaceWhite,
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFFEEF2F6),
        onSurfaceVariant = TextSecondary,
        error = AlertRed
    )
    AppTheme.CIVIC_DARK -> darkColorScheme(
        primary = CivicBlueLight,
        onPrimary = Color.White,
        secondary = EcoGreen,
        background = NeutralSurface,
        onBackground = OnSurface,
        surface = Color(0xFF2C2C2E),
        onSurface = OnSurface,
        error = AlertRed
    )
    AppTheme.ECO_GREEN -> lightColorScheme(
        primary = Color(0xFF1B5E20),
        onPrimary = Color.White,
        secondary = EmeraldGreen,
        tertiary = Color(0xFF81C784),
        background = Color(0xFFE8F5E9),
        onBackground = Color(0xFF1B4332),
        surface = Color(0xFFF1F8E9),
        onSurface = Color(0xFF1B4332),
        error = AlertRed
    )
    AppTheme.SUNSET -> lightColorScheme(
        primary = Color(0xFFE65100),
        onPrimary = Color.White,
        secondary = Color(0xFFFF8F00),
        tertiary = Color(0xFFFFB74D),
        background = Color(0xFFFFF8E1),
        onBackground = Color(0xFF4E342E),
        surface = Color(0xFFFFFBF0),
        onSurface = Color(0xFF4E342E),
        error = Color(0xFFC62828)
    )
    AppTheme.HIGH_CONTRAST -> lightColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        secondary = Color(0xFF0000FF),
        background = Color.White,
        onBackground = Color.Black,
        surface = Color(0xFFF0F0F0),
        onSurface = Color.Black,
        error = Color(0xFFB00020)
    )
    AppTheme.ROYAL_PURPLE -> lightColorScheme(
        primary = Color(0xFF4A148C),
        onPrimary = Color.White,
        secondary = Color(0xFF7B1FA2),
        tertiary = Color(0xFFCE93D8),
        background = Color(0xFFF3E5F5),
        onBackground = Color(0xFF311B92),
        surface = Color(0xFFFAF5FC),
        onSurface = Color(0xFF311B92),
        error = AlertRed
    )
}

fun AppTheme.backgroundColor(): Color = when (this) {
    AppTheme.CIVIC_DARK -> NeutralSurface
    AppTheme.ECO_GREEN -> Color(0xFFE8F5E9)
    AppTheme.SUNSET -> Color(0xFFFFF8E1)
    AppTheme.HIGH_CONTRAST -> Color.White
    AppTheme.ROYAL_PURPLE -> Color(0xFFF3E5F5)
    else -> MintBackground
}
