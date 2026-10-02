package com.rishabh.gymbros.widgets

import android.content.Context
import com.rishabh.gymbros.features.reminders.WaterReminderScheduler
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.settings.model.WaterUnit
import com.rishabh.gymbros.features.settings.model.volumeNumber
import com.rishabh.gymbros.features.settings.model.volumeUnitLabel
import com.rishabh.gymbros.features.water.data.WaterDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal data class WidgetHydrationSnapshot(
    val intakeMl: Int,
    val goalMl: Int,
    val percent: Int,
    val progress: Float,
    val unit: WaterUnit,
    val quickAddMl: Int,
    val nextSipText: String,
    val reminderStatusText: String
) {
    val intakeAndGoalText: String
        get() = "${volumeNumber(intakeMl, unit)} / ${volumeNumber(goalMl, unit)} ${volumeUnitLabel(unit)}"

    /** Pieces for the 2x1 widget, which shows "2,400" big and "/ 3,000" small. */
    val intakeText: String
        get() = volumeNumber(intakeMl, unit)
    val goalText: String
        get() = "/ ${volumeNumber(goalMl, unit)}"
}

internal object WidgetHydrationData {
    fun load(context: Context): WidgetHydrationSnapshot {
        val appContext = context.applicationContext
        val settings = SettingsRepository.getInstance(appContext).currentSettings
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val intake = WaterDatabase(appContext)
            .entriesBetween(start, end)
            .sumOf { it.amountMl.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        val goal = settings.dailyGoalMl.coerceAtLeast(1)
        val rawProgress = intake.toFloat() / goal.toFloat()
        val progress = rawProgress.coerceIn(0f, 1f)
        val percent = (progress * 100f).roundToInt().coerceIn(0, 100)
        val goalReached = intake >= goal

        val nextSip = when {
            goalReached -> "Goal hit"
            !settings.reminderEnabled -> "Off"
            else -> WaterReminderScheduler.nextReminderTime(
                settings = settings,
                now = ZonedDateTime.now(zoneId)
            ).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        }

        val reminderStatus = when {
            goalReached -> "Hydration complete"
            settings.reminderEnabled -> "Reminder"
            else -> "Reminders off"
        }

        return WidgetHydrationSnapshot(
            intakeMl = intake,
            goalMl = goal,
            percent = percent,
            progress = progress,
            unit = settings.unit,
            quickAddMl = settings.widgetButtonMl,
            nextSipText = nextSip,
            reminderStatusText = reminderStatus
        )
    }
}
