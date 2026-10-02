package com.rishabh.gymbros.features.settings.model

import java.util.Locale

const val DEFAULT_DAILY_GOAL_ML = 3_700
val DEFAULT_QUICK_ADD_PRESETS = listOf(250, 500, 750)
val DEFAULT_QUICK_ADD_LABELS = listOf("Glass", "Bottle", "Gym jug")

enum class WaterUnit {
    ML,
    FL_OZ
}

enum class WeightUnit {
    KG,
    LB
}

enum class HeightUnit {
    FT,
    CM
}

enum class AccentTheme {
    ORANGE,
    BLUE,
    LIGHT_GREEN,
    MONOCHROME
}

enum class UiVersion {
    V1,
    V2
}

data class AppSettings(
    val userName: String = "",
    val profileImageUri: String = "",
    val ageYears: Int? = null,
    val weightKg: Float? = null,
    val heightCm: Int? = null,
    val dailyGoalMl: Int = DEFAULT_DAILY_GOAL_ML,
    val quickAddPresets: List<Int> = DEFAULT_QUICK_ADD_PRESETS,
    val quickAddLabels: List<String> = DEFAULT_QUICK_ADD_LABELS,
    val widgetButtonMl: Int = 250,
    val reminderEnabled: Boolean = false,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 20,
    val reminderIntervalHours: Int = 2,
    val unit: WaterUnit = WaterUnit.ML,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.FT,
    val accentTheme: AccentTheme = AccentTheme.ORANGE,
    val uiVersion: UiVersion = UiVersion.V2
)

fun formatVolume(amountMl: Int, unit: WaterUnit): String {
    return when (unit) {
        WaterUnit.ML -> String.format(Locale.US, "%,d ml", amountMl)
        WaterUnit.FL_OZ -> String.format(Locale.US, "%.1f fl oz", amountMl / 29.5735)
    }
}

fun volumeNumber(amountMl: Int, unit: WaterUnit): String {
    return when (unit) {
        WaterUnit.ML -> String.format(Locale.US, "%,d", amountMl)
        WaterUnit.FL_OZ -> String.format(Locale.US, "%.1f", amountMl / 29.5735)
    }
}

fun volumeUnitLabel(unit: WaterUnit): String = when (unit) {
    WaterUnit.ML -> "ml"
    WaterUnit.FL_OZ -> "fl oz"
}
