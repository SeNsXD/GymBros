package com.rishabh.gymbros.features.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.widgets.GymBrosWidgetUpdater
import com.rishabh.gymbros.features.checklist.GymChecklistScheduler

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val settings = SettingsRepository.getInstance(appContext).currentSettings
        WaterReminderScheduler.reschedule(appContext, settings)
        GymChecklistScheduler.reschedule(appContext)
        GymBrosWidgetUpdater.updateAll(appContext)
    }
}
