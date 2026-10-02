package com.rishabh.gymbros.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.rishabh.gymbros.features.settings.model.AccentTheme


object GymBrosAccent {
    var current: AccentTheme = AccentTheme.ORANGE

    val primary: Color get() = when (current) {
        AccentTheme.ORANGE -> Color(0xFFFF7A00)
        AccentTheme.BLUE -> Color(0xFF4D9CFF)
        AccentTheme.LIGHT_GREEN -> Color(0xFF9EDB72)
        AccentTheme.MONOCHROME -> Color(0xFFF2F2F2)
    }
    val secondary: Color get() = when (current) {
        AccentTheme.ORANGE -> Color(0xFFFE9D00)
        AccentTheme.BLUE -> Color(0xFF77B7FF)
        AccentTheme.LIGHT_GREEN -> Color(0xFFB9E895)
        AccentTheme.MONOCHROME -> Color(0xFFC8C8C8)
    }
    val tertiary: Color get() = when (current) {
        AccentTheme.ORANGE -> Color(0xFFF5BF2D)
        AccentTheme.BLUE -> Color(0xFFA9D2FF)
        AccentTheme.LIGHT_GREEN -> Color(0xFFD2F2B7)
        AccentTheme.MONOCHROME -> Color(0xFFA6A6A6)
    }
    val soft: Color get() = when (current) {
        AccentTheme.ORANGE -> Color(0xFFFFB68B)
        AccentTheme.BLUE -> Color(0xFFB9D9FF)
        AccentTheme.LIGHT_GREEN -> Color(0xFFD8F1C5)
        AccentTheme.MONOCHROME -> Color(0xFFDADADA)
    }
    val onAccent: Color get() = Color(0xFF111111)
}

object IgniteColors {
    val Background = Color(0xFF0D0C0B)
    val Surface1 = Color(0xFF141210)
    val Surface2 = Color(0xFF1C1917)
    val Surface3 = Color(0xFF27221E)
    val CardTop = Color(0xFF1E1A16)
    val CardBottom = Color(0xFF16130F)
    val Hairline = Color.White.copy(alpha = 0.08f)

    val Ember: Color get() = GymBrosAccent.primary
    val Gold: Color get() = GymBrosAccent.secondary
    val DeepEmber: Color get() = GymBrosAccent.primary.copy(alpha = 0.78f)
    val TextOnGradient: Color get() = GymBrosAccent.onAccent

    val Text = Color(0xFFF2EDE5)
    val MutedText = Color(0xFF9A9186)
    val Dimmed = Color(0xFF7A7268)
    val SoftText = Color(0xFFB9B0A4)
    val TopGlow: Color get() = GymBrosAccent.primary.copy(alpha = 0.10f)
    val BottleBorder = Color(0xFF3A342E)

    val LiveBorder: Color get() = Ember.copy(alpha = 0.45f)
    val GoldTint: Color get() = Gold.copy(alpha = 0.12f)
    val EmberTint: Color get() = Ember.copy(alpha = 0.12f)

    // Home hydration card redesign tokens.
    val HydrationSurface: Color get() = GymBrosAccent.primary.copy(alpha = 0.055f).compositeOver(Color(0xFF121212))
    val HydrationStroke: Color get() = GymBrosAccent.primary.copy(alpha = 0.16f).compositeOver(Color(0xFF202020))
    val HydrationBorder: Color get() = GymBrosAccent.primary.copy(alpha = 0.18f).compositeOver(Color(0xFF202020))
    val HydrationHi: Color get() = GymBrosAccent.secondary
    val HydrationLo: Color get() = GymBrosAccent.primary
    val HydrationAmber: Color get() = GymBrosAccent.tertiary
    val HydrationText = Color(0xFFF4EDE3)
    val HydrationDim = Color(0xFFA99A86)
    val HydrationFaint = Color(0xFF6F6252)
    val HydrationGoalPill: Color get() = GymBrosAccent.primary.copy(alpha = 0.08f).compositeOver(Color(0xFF171717))
    val HydrationBottleBackground: Color get() = GymBrosAccent.primary.copy(alpha = 0.04f).compositeOver(Color(0xFF111111))
    val HydrationActionText: Color get() = GymBrosAccent.onAccent
}

object IgniteBrushes {
    val Heat: Brush get() = Brush.linearGradient(
        colors = listOf(IgniteColors.Ember, IgniteColors.Gold),
        start = Offset.Zero,
        end = Offset(700f, 700f)
    )

    val Card = Brush.verticalGradient(
        colors = listOf(IgniteColors.CardTop, IgniteColors.CardBottom)
    )

    val LiveCard = Brush.linearGradient(
        colors = listOf(IgniteColors.Ember.copy(alpha = 0.16f).compositeOver(IgniteColors.Surface2), IgniteColors.Surface2),
        start = Offset.Zero,
        end = Offset(600f, 500f)
    )

    val Screen = Brush.verticalGradient(
        colors = listOf(IgniteColors.TopGlow, IgniteColors.Background),
        startY = 0f,
        endY = 900f
    )

    val Water: Brush get() = Brush.verticalGradient(
        colors = listOf(IgniteColors.Gold, IgniteColors.Ember, IgniteColors.DeepEmber)
    )

    val HydrationCard = Brush.linearGradient(
        colors = listOf(
            IgniteColors.HydrationSurface,
            IgniteColors.Surface1,
            IgniteColors.Background
        ),
        start = Offset.Zero,
        end = Offset(900f, 1100f)
    )

    val HydrationAction: Brush get() = Brush.linearGradient(
        colors = listOf(IgniteColors.HydrationHi, IgniteColors.HydrationLo),
        start = Offset.Zero,
        end = Offset(700f, 700f)
    )

    val HydrationWater: Brush get() = Brush.verticalGradient(
        colors = listOf(IgniteColors.HydrationAmber, IgniteColors.HydrationLo)
    )
}

object ObsidianSunsetColors {
    val Canvas = Color(0xFF131313)
    val CanvasDeep = Color(0xFF0E0E0E)
    val Card = Color(0xFF1C1B1B)
    val CardHigh = Color(0xFF201F1F)
    val Elevated = Color(0xFF2A2A2A)
    val ElevatedHigh = Color(0xFF353534)
    val Border = Color(0xFF2A2A2A)

    val Orange: Color get() = GymBrosAccent.primary
    val Amber: Color get() = GymBrosAccent.secondary
    val Gold: Color get() = GymBrosAccent.tertiary
    val SoftOrange: Color get() = GymBrosAccent.soft

    val Text = Color(0xFFE5E2E1)
    val TextStrong = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFC8C8C8)
    val Muted = Color(0xFF9A9A9A)
    val Dimmed = Color(0xFF737373)

    val OrangeGlow: Color get() = Orange.copy(alpha = 0.15f)
    val OrangeTint: Color get() = Orange.copy(alpha = 0.12f)
}

object ObsidianSunsetBrushes {
    val Heat: Brush get() = Brush.linearGradient(
        colors = listOf(ObsidianSunsetColors.Orange, ObsidianSunsetColors.Amber),
        start = Offset.Zero,
        end = Offset(700f, 700f)
    )

    val HeatReverse: Brush get() = Brush.linearGradient(
        colors = listOf(ObsidianSunsetColors.Amber, ObsidianSunsetColors.Orange),
        start = Offset.Zero,
        end = Offset(700f, 700f)
    )
}
