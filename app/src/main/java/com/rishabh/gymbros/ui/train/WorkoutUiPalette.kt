package com.rishabh.gymbros.ui.train

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rishabh.gymbros.ui.theme.GymBrosAccent
import com.rishabh.gymbros.ui.theme.ObsidianSunsetColors
import com.rishabh.gymbros.ui.theme.Ui2Colors

/**
 * Palette bridge for workout drill-down surfaces shared by UI 1 and UI 2.
 * UI 1 keeps the existing Obsidian/Accent colors by default. TrainScreenV2
 * overrides this CompositionLocal so the same feature screens can render in
 * UI 2 without duplicating state or behavior.
 */
internal data class WorkoutUiPalette(
    val Canvas: Color,
    val CanvasDeep: Color,
    val Card: Color,
    val CardHigh: Color,
    val Elevated: Color,
    val ElevatedHigh: Color,
    val Border: Color,
    val Orange: Color,
    val Amber: Color,
    val Gold: Color,
    val SoftOrange: Color,
    val Text: Color,
    val TextStrong: Color,
    val TextSecondary: Color,
    val Muted: Color,
    val Dimmed: Color,
    val OrangeGlow: Color,
    val OrangeTint: Color,
    val CancelAccent: Color,
    val CancelTint: Color,
    val OnAccent: Color,
    val Heat: Brush,
    // Dialog/destructive text. Color.Unspecified keeps the default Material button color (UI 1.0 behavior).
    val DismissText: Color = Color.Unspecified,
    val DangerText: Color = Color.Unspecified,
    val DiscardAccent: Color = Color(0xFFFF8A80),
    val DeleteAccent: Color = Color(0xFFFFAAA2),
    // True only for the UI 2.0 (Arctic) palette. Drill-down screens use it to switch
    // to the multi-color, borderless card treatment. UI 1.0 keeps its classic look.
    val MultiColor: Boolean = false
)

/** Fixed accent colors for the UI 2.0 multi-color workout drill-down screens. */
internal object WorkoutAccents {
    val Mint = Ui2Colors.Primary
    val MintTint = Color(0xFF12332A)
    val Cyan = Ui2Colors.Cyan
    val CyanTint = Color(0xFF10303A)
    val OnCyan = Color(0xFF062A33)
    val Purple = Color(0xFFA78BFA)
    val PurpleTint = Color(0xFF211B3F)
    val OnPurple = Color(0xFF1A1333)
    val Pink = Color(0xFFFF5C8A)
    val PinkTint = Color(0xFF2A1520)
    val DeleteRed = Color(0xFFFF4D4D)
    val DeleteTint = Color(0xFF2A1520)
    val ChipNeutral = Color(0xFF1E2130)
}

/** Returns [multi] on the UI 2.0 palette and [classic] on the UI 1.0 palette. */
internal fun WorkoutUiPalette.pick(multi: Color, classic: Color): Color =
    if (MultiColor) multi else classic

/** Card border that is skipped on the UI 2.0 palette (borderless cards). */
@Composable
internal fun Modifier.workoutBorder(width: Dp, color: Color, shape: Shape): Modifier =
    if (LocalWorkoutUiPalette.current.MultiColor) this else this.border(width, color, shape)

private fun classicWorkoutPalette(): WorkoutUiPalette {
    val accent = ObsidianSunsetColors.Orange
    val secondary = ObsidianSunsetColors.Amber
    return WorkoutUiPalette(
        Canvas = ObsidianSunsetColors.Canvas,
        CanvasDeep = ObsidianSunsetColors.CanvasDeep,
        Card = ObsidianSunsetColors.Card,
        CardHigh = ObsidianSunsetColors.CardHigh,
        Elevated = ObsidianSunsetColors.Elevated,
        ElevatedHigh = ObsidianSunsetColors.ElevatedHigh,
        Border = ObsidianSunsetColors.Border,
        Orange = accent,
        Amber = secondary,
        Gold = ObsidianSunsetColors.Gold,
        SoftOrange = ObsidianSunsetColors.SoftOrange,
        Text = ObsidianSunsetColors.Text,
        TextStrong = ObsidianSunsetColors.TextStrong,
        TextSecondary = ObsidianSunsetColors.TextSecondary,
        Muted = ObsidianSunsetColors.Muted,
        Dimmed = ObsidianSunsetColors.Dimmed,
        OrangeGlow = ObsidianSunsetColors.OrangeGlow,
        OrangeTint = ObsidianSunsetColors.OrangeTint,
        // Preserve the existing UI 1.0 cancel treatment.
        CancelAccent = Color(0xFFFFAAA2),
        CancelTint = Color(0xFF3A2020),
        OnAccent = GymBrosAccent.onAccent,
        Heat = Brush.linearGradient(
            colors = listOf(accent, secondary),
            start = Offset.Zero,
            end = Offset(700f, 700f)
        )
    )
}

internal val Ui2WorkoutPalette = WorkoutUiPalette(
    Canvas = Ui2Colors.Background,
    CanvasDeep = Ui2Colors.Background,
    Card = Ui2Colors.Card,
    CardHigh = Ui2Colors.CardRaised,
    Elevated = Ui2Colors.CardRaised,
    ElevatedHigh = Ui2Colors.QuestEmpty,
    Border = Ui2Colors.CardOutline,
    Orange = Ui2Colors.Primary,
    Amber = Ui2Colors.Cyan,
    Gold = Ui2Colors.Primary,
    SoftOrange = Ui2Colors.Primary.copy(alpha = 0.86f),
    Text = Ui2Colors.Text,
    TextStrong = Ui2Colors.Text,
    TextSecondary = Ui2Colors.Muted,
    Muted = Ui2Colors.Muted,
    Dimmed = Ui2Colors.Dim,
    OrangeGlow = Ui2Colors.Primary.copy(alpha = 0.15f),
    OrangeTint = Ui2Colors.Primary.copy(alpha = 0.12f),
    // UI 2.0 cancel uses the purple side of the new purple/cyan identity,
    // never the legacy orange danger pill.
    CancelAccent = Ui2Colors.Lavender,
    CancelTint = Ui2Colors.Lavender.copy(alpha = 0.16f),
    OnAccent = Ui2Colors.Background,
    Heat = Brush.linearGradient(
        colors = listOf(Ui2Colors.Primary, Ui2Colors.Cyan),
        start = Offset.Zero,
        end = Offset(700f, 700f)
    ),
    DismissText = Ui2Colors.Muted,
    DangerText = Ui2Colors.Danger,
    DiscardAccent = Ui2Colors.Danger,
    DeleteAccent = Ui2Colors.Danger,
    MultiColor = true
)

internal val LocalWorkoutUiPalette = staticCompositionLocalOf { classicWorkoutPalette() }
