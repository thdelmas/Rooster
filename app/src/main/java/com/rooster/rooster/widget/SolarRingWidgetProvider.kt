package com.rooster.rooster.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rooster.rooster.MainActivity
import com.rooster.rooster.R
import kotlinx.coroutines.runBlocking

/**
 * Widget provider for the Solar Ring Widget.
 * Orchestrates data fetching and rendering, delegating to
 * [SolarRingDataProvider] and [SolarRingRenderer].
 */
class SolarRingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val astronomyData = runBlocking { SolarRingDataProvider.getAstronomyData(context) }
            val nextAlarm = runBlocking { SolarRingDataProvider.getNextAlarm(context) }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            val density = context.resources.displayMetrics.density
            val minDimensionDp = Math.min(minWidthDp, minHeightDp).coerceAtLeast(100)
            val size = (minDimensionDp * density).toInt()

            val bitmap = SolarRingRenderer.generateRingBitmap(context, astronomyData, size, nextAlarm)

            val views = RemoteViews(context.packageName, R.layout.widget_solar_ring)
            views.setImageViewBitmap(R.id.widget_ring_image, bitmap)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_ring_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEnabled(context: Context) {
        // Widget enabled
    }

    override fun onDisabled(context: Context) {
        // Widget disabled
    }
}
