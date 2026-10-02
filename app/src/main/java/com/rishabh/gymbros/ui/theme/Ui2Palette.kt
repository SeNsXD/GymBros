package com.rishabh.gymbros.ui.theme

import androidx.compose.ui.graphics.Color

/** Visual tokens for the optional GymBros 2.0 interface. */
object Ui2Colors {
    val Background = Color(0xFF0A0B10)
    val Card = Color(0xFF14161F)
    val CardRaised = Color(0xFF191C28)
    val CardOutline = Color(0xFF222635)
    val Text = Color(0xFFF6F4F1)
    val Muted = Color(0xFF7F8497)
    val Dim = Color(0xFF5A6074)
    val Primary = Color(0xFF38E89F)
    // Compatibility alias: all existing UI 2.0 accent consumers share Primary.
    val Lime = Primary
    val Cyan = Color(0xFF4DE1FF)
    val Water = Color(0xFF1FB6D9)
    val Lavender = Color(0xFF9B8CFF)
    val QuestEmpty = Color(0xFF262A40)
    // Rose red (no orange hue) so destructive actions never read as the UI 1.0 orange accent.
    val Danger = Color(0xFFFF5C7A)
}
