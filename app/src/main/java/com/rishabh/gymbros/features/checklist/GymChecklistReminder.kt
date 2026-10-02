package com.rishabh.gymbros.features.checklist

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rishabh.gymbros.MainActivity
import com.rishabh.gymbros.R
import java.time.*

object GymChecklistScheduler {
    private const val REQUEST = 8461
    fun reschedule(context: Context) { cancel(context); scheduleNext(context) }
    fun scheduleNext(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val s = GymChecklistStore(context).settings(); if (!s.enabled || s.days.isEmpty()) return
        var candidate: ZonedDateTime? = null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val c = date.atTime(s.hour, s.minute).atZone(now.zone)
            if (s.days.contains(c.dayOfWeek.value) && c.isAfter(now)) { candidate = c; break }
        }
        candidate ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = candidate.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending(context))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending(context))
        }
    }
    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pending(context))
    private fun pending(context: Context) = PendingIntent.getBroadcast(context, REQUEST, Intent(context, GymChecklistReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class GymChecklistReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = GymChecklistStore(context); val s = store.settings()
        try {
            if (!s.enabled) return
            val items = store.items(); val unchecked = items.filterNot { it.checked }
            if (canNotify(context)) {
                createChannel(context)
                val open = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP; putExtra(EXTRA_OPEN_CHECKLIST, true) }
                val pi = PendingIntent.getActivity(context, 8462, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val body = if (unchecked.isEmpty()) "Your bag is ready — ${items.size}/${items.size} items packed." else "${items.size-unchecked.size}/${items.size} packed • ${unchecked.take(3).joinToString { it.name }}${if (unchecked.size > 3) " +${unchecked.size-3} more" else ""}"
                val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification_water).setContentTitle("Gym time soon 🏋️").setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).setContentIntent(pi).build()
                NotificationManagerCompat.from(context).notify(8463, n)
            }
        } finally { GymChecklistScheduler.scheduleNext(context.applicationContext) }
    }
    private fun canNotify(c: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private fun createChannel(c: Context) { if (Build.VERSION.SDK_INT >= 26) c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Gym checklist reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Reminds you to check your gym bag before gym time" }) }
    companion object { const val EXTRA_OPEN_CHECKLIST = "open_gym_checklist"; private const val CHANNEL = "gymbros_gym_checklist" }
}
