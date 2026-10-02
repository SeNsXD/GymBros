package com.rishabh.gymbros.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private fun igniteColorScheme() = darkColorScheme(
    primary = IgniteColors.Ember,
    onPrimary = IgniteColors.TextOnGradient,
    primaryContainer = IgniteColors.Surface2,
    onPrimaryContainer = IgniteColors.Text,
    secondary = IgniteColors.Gold,
    onSecondary = IgniteColors.TextOnGradient,
    background = IgniteColors.Background,
    onBackground = IgniteColors.Text,
    surface = IgniteColors.Surface1,
    onSurface = IgniteColors.Text,
    surfaceVariant = IgniteColors.Surface2,
    onSurfaceVariant = IgniteColors.MutedText,
    outline = IgniteColors.Hairline
)

val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GymBrosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = igniteColorScheme(),
        typography = GymTypography,
        shapes = GymShapes,
        content = content
    )
}
