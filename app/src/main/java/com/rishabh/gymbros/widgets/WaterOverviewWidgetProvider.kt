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
import com.rishabh.gymbros.features.settings.model.volumeNumber
import com.rishabh.gymbros.features.water.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterOverviewWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_ADD_QUICK || action == ACTION_SUBTRACT_QUICK) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val snapshot = WidgetHydrationData.load(context)
                    val repository = WaterRepository.getInstance(context.applicationContext)
                    if (action == ACTION_ADD_QUICK) {
                        repository.addWaterAndWait(snapshot.quickAddMl)
                    } else {
                        repository.subtractWaterAndWait(snapshot.quickAddMl)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_ADD_QUICK = "com.rishabh.gymbros.widget.ADD_QUICK_WATER"
        private const val ACTION_SUBTRACT_QUICK = "com.rishabh.gymbros.widget.SUBTRACT_QUICK_WATER"
        private const val REQUEST_OPEN_APP = 8100
        private const val REQUEST_ADD_QUICK = 8101
        private const val REQUEST_SUBTRACT_QUICK = 8102

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WaterOverviewWidgetProvider::class.java)
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snapshot = WidgetHydrationData.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_water_overview)
            val quickAmountText = volumeNumber(snapshot.quickAddMl, snapshot.unit)

            views.setTextViewText(R.id.widget_water_value, snapshot.intakeAndGoalText)
            views.setTextViewText(R.id.widget_subtract_quick, "−")
            views.setTextViewText(R.id.widget_add_quick, "+$quickAmountText")
            views.setProgressBar(R.id.widget_water_progress, 100, snapshot.percent, false)
            views.setImageViewBitmap(
                R.id.widget_water_bottle,
                WidgetGraphics.bottleBitmap(context, snapshot.progress)
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            views.setOnClickPendingIntent(
                R.id.widget_subtract_quick,
                subtractQuickPendingIntent(context)
            )
            views.setOnClickPendingIntent(R.id.widget_add_quick, addQuickPendingIntent(context))

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_APP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun addQuickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WaterOverviewWidgetProvider::class.java).apply {
                action = ACTION_ADD_QUICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_ADD_QUICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun subtractQuickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WaterOverviewWidgetProvider::class.java).apply {
                action = ACTION_SUBTRACT_QUICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_SUBTRACT_QUICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
