package com.rishabh.gymbros.features.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rishabh.gymbros.MainActivity
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.settings.model.formatVolume
import com.rishabh.gymbros.features.water.data.WaterDatabase
import com.rishabh.gymbros.widgets.GymBrosWidgetUpdater
import java.time.LocalDate
import java.time.ZoneId

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository.getInstance(context.applicationContext).currentSettings
        if (!settings.reminderEnabled) return

        try {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val totalMl = WaterDatabase(context.applicationContext)
                .entriesBetween(start, end)
                .sumOf { it.amountMl }

            if (totalMl < settings.dailyGoalMl && canPostNotifications(context)) {
                createChannel(context)
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    7302,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val remaining = (settings.dailyGoalMl - totalMl).coerceAtLeast(0)
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_water)
                    .setContentTitle("Time for a sip")
                    .setContentText("${formatVolume(remaining, settings.unit)} left to hit today's goal.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(openAppPendingIntent)
                    .build()

                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        } finally {
            WaterReminderScheduler.scheduleNext(context.applicationContext, settings)
            GymBrosWidgetUpdater.updateAll(context.applicationContext)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Water reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "GymBros hydration reminders"
                }
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "gymbros_water_reminders"
        private const val NOTIFICATION_ID = 7303
    }
}
