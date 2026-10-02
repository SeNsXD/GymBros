package com.rishabh.gymbros.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rishabh.gymbros.MainActivity
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.water.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Compact 2x1 hydration widget: live bottle, intake, progress bar + quick subtract/add pills. */
class WaterCompactWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_ADD || action == ACTION_SUBTRACT) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val snapshot = WidgetHydrationData.load(context)
                    val repository = WaterRepository.getInstance(context.applicationContext)
                    if (action == ACTION_ADD) repository.addWaterAndWait(snapshot.quickAddMl)
                    else repository.subtractWaterAndWait(snapshot.quickAddMl)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_ADD = "com.rishabh.gymbros.widget.COMPACT_ADD_WATER"
        private const val ACTION_SUBTRACT = "com.rishabh.gymbros.widget.COMPACT_SUBTRACT_WATER"
        private const val REQUEST_OPEN = 8300
        private const val REQUEST_ADD = 8301
        private const val REQUEST_SUBTRACT = 8302

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, WaterCompactWidgetProvider::class.java))
                .forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val snapshot = WidgetHydrationData.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_water_compact)
            views.setImageViewBitmap(
                R.id.widget_compact_bottle,
                WidgetGraphics.miniBottleBitmap(context, snapshot.progress)
            )
            views.setTextViewText(R.id.widget_compact_intake, snapshot.intakeText)
            views.setTextViewText(R.id.widget_compact_goal, snapshot.goalText)
            views.setProgressBar(R.id.widget_compact_progress, 100, snapshot.percent, false)
            views.setTextViewText(R.id.widget_compact_add, "+${snapshot.quickAddMl}")
            views.setOnClickPendingIntent(R.id.widget_compact_root, openApp(context))
            views.setOnClickPendingIntent(R.id.widget_compact_add, actionIntent(context, ACTION_ADD, REQUEST_ADD))
            views.setOnClickPendingIntent(R.id.widget_compact_subtract, actionIntent(context, ACTION_SUBTRACT, REQUEST_SUBTRACT))
            manager.updateAppWidget(id, views)
        }

        private fun openApp(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, REQUEST_OPEN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, WaterCompactWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
