package com.rishabh.gymbros.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private fun ui2BaseColorScheme(): ColorScheme = darkColorScheme(
    primary = Ui2Colors.Primary, onPrimary = Ui2Colors.Background,
    primaryContainer = Ui2Colors.CardRaised, onPrimaryContainer = Ui2Colors.Text,
    secondary = Ui2Colors.Lavender, onSecondary = Ui2Colors.Background,
    secondaryContainer = Ui2Colors.CardRaised, onSecondaryContainer = Ui2Colors.Text,
    tertiary = Ui2Colors.Cyan, onTertiary = Ui2Colors.Background,
    background = Ui2Colors.Background, onBackground = Ui2Colors.Text,
    surface = Ui2Colors.Card, onSurface = Ui2Colors.Text,
    surfaceVariant = Ui2Colors.CardRaised, onSurfaceVariant = Ui2Colors.Muted,
    surfaceTint = Ui2Colors.Primary, outline = Ui2Colors.Dim,
    outlineVariant = Ui2Colors.CardOutline, error = Ui2Colors.Danger
)

/** Scoped to V2 surfaces; the classic Ignite theme remains unchanged. */
@Composable
fun Ui2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ui2BaseColorScheme(),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

/**
 * App-level Material theme switch. In UI 2.0 every Material default (dialog
 * surfaces, text-button and text-field accents, error color) resolves to the
 * UI 2.0 palette instead of the Ignite one. In UI 1.0 the parent scheme is
 * passed through untouched, so classic screens render exactly as before.
 */
@Composable
fun UiVersionMaterialTheme(isUi2: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isUi2) {
            ui2BaseColorScheme().copy(
                surfaceContainerLowest = Ui2Colors.Background,
                surfaceContainerLow = Ui2Colors.Card,
                surfaceContainer = Ui2Colors.Card,
                surfaceContainerHigh = Ui2Colors.CardRaised,
                surfaceContainerHighest = Ui2Colors.CardRaised
            )
        } else {
            MaterialTheme.colorScheme
        },
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
