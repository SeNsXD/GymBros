package com.rishabh.gymbros.widgets

import android.content.Context

object GymBrosWidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        WaterOverviewWidgetProvider.updateAll(appContext)
        WaterCompactWidgetProvider.updateAll(appContext)
        WaterProgressWidgetProvider.updateAll(appContext)
        NextSipWidgetProvider.updateAll(appContext)
    }
}
