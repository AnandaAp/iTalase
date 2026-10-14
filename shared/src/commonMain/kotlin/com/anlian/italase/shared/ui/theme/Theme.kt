package com.anlian.italase.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// --- Warung design tokens (single source of truth) ---
// Cream paper background + deep warung green + marigold accent.
// Body text on cream and white text on primary both pass WCAG AA.

val WarungGreen = Color(0xFF1D6B3C)
val WarungGreenDark = Color(0xFF0E3D20)
val WarungGreenSoft = Color(0xFFDCEFE1)
val Marigold = Color(0xFFE8930C)
val MarigoldSoft = Color(0xFFFDEBC8)
val MarigoldInk = Color(0xFF5C3800)
val Paper = Color(0xFFFAF6EE)
val CardWhite = Color(0xFFFFFDF7)
val SandLow = Color(0xFFF3EDDE)
val Sand = Color(0xFFEAE1CC)
val InkSoft = Color(0xFF6B6250)

internal val LightColors = lightColorScheme(
    primary = WarungGreen,
    onPrimary = Color.White,
    primaryContainer = WarungGreenSoft,
    onPrimaryContainer = WarungGreenDark,
    secondary = Marigold,
    onSecondary = Color.White,
    secondaryContainer = MarigoldSoft,
    onSecondaryContainer = MarigoldInk,
    tertiary = Color(0xFF0E7C86),
    background = Paper,
    onBackground = Color(0xFF1C1B17),
    surface = CardWhite,
    onSurface = Color(0xFF1C1B17),
    surfaceVariant = SandLow,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SandLow,
    surfaceContainer = Sand,
    surfaceContainerHigh = Color(0xFFE2D7BE),
    outlineVariant = Color(0xFFD8CFB6),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD89A),
    onPrimary = Color(0xFF06351A),
    primaryContainer = WarungGreenDark,
    onPrimaryContainer = Color(0xFFDCEFE1),
    secondary = Marigold,
    secondaryContainer = Color(0xFF4A2E00),
    onSecondaryContainer = MarigoldSoft,
    background = Color(0xFF14120C),
    surface = Color(0xFF1E1B14),
    surfaceContainerLow = Color(0xFF242117),
    surfaceContainer = Color(0xFF2B271C),
    surfaceContainerHigh = Color(0xFF333022),
)

private val WarungShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ITalaseTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme(dark),
        shapes = WarungShapes,
        content = content,
    )
}

// Brand scheme on iOS / older Android, Material You dynamic scheme on Android 12+.
@Composable
expect fun AppColorScheme(dark: Boolean): ColorScheme
