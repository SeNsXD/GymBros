package com.rishabh.gymbros.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Phase 1 uses Android's sans-serif fallback so the project contains no bundled font files.
 * The typography metrics mirror the Sora/Manrope mockup closely; exact Sora + Manrope font
 * resources can be wired in later without touching screen code.
 */
private val DisplayFont = FontFamily.SansSerif
private val BodyFont = FontFamily.SansSerif

val GymTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.35).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
