package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BlitzFireColorScheme = darkColorScheme(
    primary = BlazeOrange,
    onPrimary = ObsidianBg,
    primaryContainer = BlazeAmberDark,
    onPrimaryContainer = BlazeGold,
    secondary = DiamondCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = DiamondDeep,
    onSecondaryContainer = DiamondCyan,
    tertiary = MythicCrimson,
    onTertiary = TextPrimary,
    tertiaryContainer = MythicDeep,
    onTertiaryContainer = RarityMythic,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianBorder,
    error = DangerRed,
    onError = TextPrimary
)

val BlitzFireShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BlitzFireTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlitzFireColorScheme,
        typography = BlitzFireTypography,
        shapes = BlitzFireShapes,
        content = content
    )
}
