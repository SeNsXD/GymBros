package com.rishabh.gymbros.features.workout.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rishabh.gymbros.MainActivity
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.workout.data.WorkoutRepository

object WorkoutActiveNotification {
    const val EXTRA_OPEN_WORKOUT = "open_workout"
    private const val CHANNEL_ID = "gymbros_active_workout_lockscreen_v2"
    private const val NOTIFICATION_ID = 7410
    private const val REST_FINISH_REQUEST_CODE = 7415
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private const val REST_PROGRESS_PREFS = "gymbros_rest_notification_progress"
    private const val KEY_PROGRESS_BASELINE_MS = "progress_baseline_ms"

    private fun progressBaselineMs(context: Context, suggestedTotalMs: Long): Long {
        val prefs = context.getSharedPreferences(REST_PROGRESS_PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getLong(KEY_PROGRESS_BASELINE_MS, 0L)
        if (saved > 0L) return saved
        val baseline = suggestedTotalMs.coerceAtLeast(1_000L)
        prefs.edit().putLong(KEY_PROGRESS_BASELINE_MS, baseline).apply()
        return baseline
    }

    private fun clearProgressBaseline(context: Context) {
        context.getSharedPreferences(REST_PROGRESS_PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_PROGRESS_BASELINE_MS).apply()
    }

    fun show(context: Context) {
        val appContext = context.applicationContext
        if (!canPostNotifications(appContext)) return
        createChannel(appContext)
        val state = WorkoutRepository.getInstance(appContext).state.value

        val openWorkoutIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_WORKOUT, true)
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            7411,
            openWorkoutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, requestCode: Int) = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            Intent(appContext, WorkoutNotificationActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_workout_ui2)
            .setColor(0xFF4DE1FF.toInt())
            .setContentTitle("Workout in progress")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentIntent)

        val restEnd = state.restEndsAt
        val now = System.currentTimeMillis()
        if (restEnd != null && restEnd > now) {
            val remainingMs = (restEnd - now).coerceAtLeast(0L)
            val chronometerBase = SystemClock.elapsedRealtime() + remainingMs
            val baselineMs = progressBaselineMs(appContext, state.restDurationSeconds.coerceAtLeast(1) * 1000L)
            val progress = ((remainingMs.coerceAtMost(baselineMs) * 1000L) / baselineMs).toInt()

            val minusIntent = actionIntent(WorkoutNotificationActionReceiver.ACTION_MINUS_15, 7412)
            val plusIntent = actionIntent(WorkoutNotificationActionReceiver.ACTION_PLUS_30, 7413)
            val skipIntent = actionIntent(WorkoutNotificationActionReceiver.ACTION_SKIP, 7414)

            val collapsedView = RemoteViews(appContext.packageName, R.layout.notification_rest_timer_collapsed).apply {
                setChronometer(R.id.rest_timer_chronometer, chronometerBase, null, true)
                setChronometerCountDown(R.id.rest_timer_chronometer, true)
            }

            val expandedView = RemoteViews(appContext.packageName, R.layout.notification_rest_timer).apply {
                setChronometer(R.id.rest_timer_chronometer, chronometerBase, null, true)
                setChronometerCountDown(R.id.rest_timer_chronometer, true)
                setProgressBar(R.id.rest_timer_progress, 1000, progress, false)
                setOnClickPendingIntent(R.id.rest_timer_minus, minusIntent)
                setOnClickPendingIntent(R.id.rest_timer_plus, plusIntent)
                setOnClickPendingIntent(R.id.rest_timer_skip, skipIntent)
            }

            scheduleRestFinish(appContext, restEnd)

            builder
                .setContentTitle("REST TIMER")
                .setContentText("Rest timer running")
                .setWhen(restEnd)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            cancelRestFinish(appContext)
            clearProgressBaseline(appContext)
            builder.setContentText("Tap to return to your workout.")
        }

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())

        if (restEnd != null && restEnd > now) {
            startProgressUpdates(appContext)
        } else {
            stopProgressUpdates()
        }
    }

    private fun startProgressUpdates(context: Context) {
        // RemoteViews ProgressBar does not animate itself as time passes. Refresh only
        // the notification once per second so it stays synchronized with the Chronometer.
        progressRunnable?.let(progressHandler::removeCallbacks)
        progressRunnable = object : Runnable {
            override fun run() {
                val state = WorkoutRepository.getInstance(context).state.value
                val restEnd = state.restEndsAt
                val now = System.currentTimeMillis()
                if (restEnd == null || restEnd <= now) {
                    clearProgressBaseline(context)
                    progressRunnable = null
                    return
                }

                val remainingMs = (restEnd - now).coerceAtLeast(0L)
                val baselineMs = progressBaselineMs(context, state.restDurationSeconds.coerceAtLeast(1) * 1000L)
                val progress = ((remainingMs.coerceAtMost(baselineMs) * 1000L) / baselineMs).toInt()
                val chronometerBase = SystemClock.elapsedRealtime() + remainingMs

                val openWorkoutIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_OPEN_WORKOUT, true)
                }
                val contentIntent = PendingIntent.getActivity(
                    context, 7411, openWorkoutIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                fun actionIntent(action: String, requestCode: Int) = PendingIntent.getBroadcast(
                    context, requestCode,
                    Intent(context, WorkoutNotificationActionReceiver::class.java).setAction(action),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val collapsedView = RemoteViews(context.packageName, R.layout.notification_rest_timer_collapsed).apply {
                    setChronometer(R.id.rest_timer_chronometer, chronometerBase, null, true)
                    setChronometerCountDown(R.id.rest_timer_chronometer, true)
                }
                val expandedView = RemoteViews(context.packageName, R.layout.notification_rest_timer).apply {
                    setChronometer(R.id.rest_timer_chronometer, chronometerBase, null, true)
                    setChronometerCountDown(R.id.rest_timer_chronometer, true)
                    setProgressBar(R.id.rest_timer_progress, 1000, progress, false)
                    setOnClickPendingIntent(R.id.rest_timer_minus, actionIntent(WorkoutNotificationActionReceiver.ACTION_MINUS_15, 7412))
                    setOnClickPendingIntent(R.id.rest_timer_plus, actionIntent(WorkoutNotificationActionReceiver.ACTION_PLUS_30, 7413))
                    setOnClickPendingIntent(R.id.rest_timer_skip, actionIntent(WorkoutNotificationActionReceiver.ACTION_SKIP, 7414))
                }

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_workout_ui2)
                    .setColor(0xFF4DE1FF.toInt())
                    .setContentTitle("REST TIMER")
                    .setContentText("Rest timer running")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setContentIntent(contentIntent)
                    .setWhen(restEnd)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .build()

                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                progressHandler.postDelayed(this, 1000L)
            }
        }.also { progressHandler.postDelayed(it, 1000L) }
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let(progressHandler::removeCallbacks)
        progressRunnable = null
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        cancelRestFinish(appContext)
        stopProgressUpdates()
        clearProgressBaseline(appContext)
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    private fun restFinishIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REST_FINISH_REQUEST_CODE,
        Intent(context, WorkoutNotificationActionReceiver::class.java)
            .setAction(WorkoutNotificationActionReceiver.ACTION_REST_FINISHED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun scheduleRestFinish(context: Context, restEnd: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = restFinishIntent(context)
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restEnd, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restEnd, pendingIntent)
        }
    }

    private fun cancelRestFinish(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(restFinishIntent(context))
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout timer", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows the active workout and rest timer controls"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
        )
    }
}
