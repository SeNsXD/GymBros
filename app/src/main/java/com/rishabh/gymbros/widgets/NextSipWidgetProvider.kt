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

class NextSipWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val REQUEST_OPEN_APP = 8300

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, NextSipWidgetProvider::class.java)
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snapshot = WidgetHydrationData.load(context)
            val views = RemoteViews(context.packageName, R.layout.widget_next_sip)

            views.setTextViewText(R.id.widget_next_sip_time, snapshot.nextSipText)
            views.setTextViewText(R.id.widget_next_sip_status, snapshot.reminderStatusText)
            views.setOnClickPendingIntent(R.id.widget_next_sip_root, openAppPendingIntent(context))

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
    }
}
