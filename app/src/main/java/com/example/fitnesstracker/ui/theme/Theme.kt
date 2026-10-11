package com.example.fitnesstracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Zentrale Farben der App ("Forge").
 * Ersetzt die privaten Farben aus MainActivity.kt.
 */
object ForgeColors {
    val Background = Color(0xFF15171B)
    val Card = Color(0xFF23262C)
    val CardElevated = Color(0xFF2B2F36)
    val Accent = Color(0xFFD0D4DC)
    val SecondaryText = Color(0xFF9DA3AE)
    val Outline = Color(0xFF3A3F47)
    val ChartLine = Color(0xFF1E6BFF)
    val Danger = Color(0xFFE5645F)
}

private val ForgeDarkColorScheme = darkColorScheme(
    primary = ForgeColors.Accent,
    onPrimary = ForgeColors.Background,
    primaryContainer = ForgeColors.CardElevated,
    onPrimaryContainer = ForgeColors.Accent,
    secondary = ForgeColors.SecondaryText,
    onSecondary = ForgeColors.Background,
    background = ForgeColors.Background,
    onBackground = ForgeColors.Accent,
    surface = ForgeColors.Card,
    onSurface = ForgeColors.Accent,
    surfaceVariant = ForgeColors.CardElevated,
    onSurfaceVariant = ForgeColors.SecondaryText,
    surfaceContainer = ForgeColors.Card,
    surfaceContainerHigh = ForgeColors.CardElevated,
    surfaceTint = Color.Transparent,
    outline = ForgeColors.Outline,
    outlineVariant = ForgeColors.Outline,
    error = ForgeColors.Danger,
    onError = ForgeColors.Background
)

private val ForgeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

/**
 * Immer dunkel, keine Dynamic Colors (die erzeugen die lila Buttons).
 * Falls dein Projekt schon eine Typography in Type.kt hat, bleibt sie erhalten.
 */
@Composable
fun FitnessTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForgeDarkColorScheme,
        shapes = ForgeShapes,
        typography = Typography,
        content = content
    )
}
