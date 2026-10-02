package com.rishabh.gymbros.features.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rishabh.gymbros.features.settings.model.AppSettings
import java.time.LocalDate
import java.time.ZonedDateTime

object WaterReminderScheduler {
    private const val REQUEST_CODE = 7301

    fun reschedule(context: Context, settings: AppSettings) {
        cancel(context)
        if (settings.reminderEnabled) {
            scheduleNext(context, settings)
        }
    }

    fun scheduleNext(
        context: Context,
        settings: AppSettings,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        if (!settings.reminderEnabled) return

        val trigger = nextReminderTime(settings, now)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            reminderPendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(reminderPendingIntent(context))
    }

    internal fun nextReminderTime(settings: AppSettings, now: ZonedDateTime): ZonedDateTime {
        fun firstCandidate(date: LocalDate): ZonedDateTime = date
            .atTime(settings.reminderStartHour, 0)
            .atZone(now.zone)

        var candidate = firstCandidate(now.toLocalDate())
        val endToday = now.toLocalDate()
            .atTime(settings.reminderEndHour, 0)
            .atZone(now.zone)

        while (!candidate.isAfter(now) && !candidate.isAfter(endToday)) {
            candidate = candidate.plusHours(settings.reminderIntervalHours.toLong())
        }

        if (!candidate.isAfter(endToday)) return candidate

        return firstCandidate(now.toLocalDate().plusDays(1))
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
