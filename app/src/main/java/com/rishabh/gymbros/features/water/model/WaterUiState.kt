package com.rishabh.gymbros.features.water.model

const val DEFAULT_DAILY_GOAL_ML = 3_700

data class DailyWaterIntake(
    val epochDay: Long,
    val intakeMl: Int
)

data class WaterUiState(
    val intakeMl: Int = 0,
    val lifetimeIntakeMl: Int = 0,
    val goalMl: Int = DEFAULT_DAILY_GOAL_ML,
    val progress: Float = 0f,
    val progressPercent: Int = 0,
    val goalReached: Boolean = false,
    val streakDays: Int = 0,
    val todayEntries: List<WaterEntry> = emptyList(),
    val weeklyIntake: List<DailyWaterIntake> = emptyList(),
    val historyIntake: List<DailyWaterIntake> = emptyList(),
    val isLoading: Boolean = true
)
