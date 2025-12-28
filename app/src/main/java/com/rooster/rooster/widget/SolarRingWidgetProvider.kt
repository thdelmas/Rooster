package com.rooster.rooster.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import com.rooster.rooster.data.local.AlarmDatabase
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Widget provider for the Solar Ring Widget
 * Displays hours in a ring with gradient colors representing solar events
 */
class SolarRingWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
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
            // Get astronomy data from database or SharedPreferences (using runBlocking for suspend)
            val astronomyData = runBlocking {
                getAstronomyData(context)
            }
            
            // Generate bitmap
            val bitmap = generateRingBitmap(context, astronomyData)
            
            // Create RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_solar_ring)
            views.setImageViewBitmap(R.id.widget_ring_image, bitmap)
            
            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun getAstronomyData(context: Context): AstronomyDataEntity? {
        return try {
            // Try to get from Room database
            val database = androidx.room.Room.databaseBuilder(
                context,
                com.rooster.rooster.data.local.AlarmDatabase::class.java,
                com.rooster.rooster.data.local.AlarmDatabase.DATABASE_NAME
            )
                .addMigrations(
                    com.rooster.rooster.data.local.AlarmDatabase.MIGRATION_1_2,
                    com.rooster.rooster.data.local.AlarmDatabase.MIGRATION_2_3,
                    com.rooster.rooster.data.local.AlarmDatabase.MIGRATION_3_4,
                    com.rooster.rooster.data.local.AlarmDatabase.MIGRATION_4_5,
                    com.rooster.rooster.data.local.AlarmDatabase.MIGRATION_5_6
                )
                .build()
            
            val astronomyData = database.astronomyDao().getAstronomyData()
            database.close()
            
            if (astronomyData != null) {
                return astronomyData
            }
            
            // Fallback to SharedPreferences
            val sharedPreferences = context.getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
            val sunrise = sharedPreferences.getLong("sunrise", 0)
            val sunset = sharedPreferences.getLong("sunset", 0)
            
            if (sunrise > 0 && sunset > 0) {
                AstronomyDataEntity(
                    id = 1,
                    latitude = sharedPreferences.getFloat("latitude", 0f),
                    longitude = sharedPreferences.getFloat("longitude", 0f),
                    sunrise = sunrise,
                    sunset = sunset,
                    solarNoon = sharedPreferences.getLong("solarNoon", 0),
                    civilDawn = sharedPreferences.getLong("civilDawn", 0),
                    civilDusk = sharedPreferences.getLong("civilDusk", 0),
                    nauticalDawn = sharedPreferences.getLong("nauticalDawn", 0),
                    nauticalDusk = sharedPreferences.getLong("nauticalDusk", 0),
                    astroDawn = sharedPreferences.getLong("astroDawn", 0),
                    astroDusk = sharedPreferences.getLong("astroDusk", 0),
                    lastUpdated = System.currentTimeMillis(),
                    dayLength = sunset - sunrise
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun generateRingBitmap(
        context: Context,
        astronomyData: AstronomyDataEntity?
    ): Bitmap {
        val size = 512 // Widget bitmap size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 50f
        val ringThickness = 50f
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Get colors for all 24 hours
        val hourColors = SolarColorCalculator.getColorsFor24Hours(astronomyData)
        
        // Draw the ring segment by segment (24 segments for 24 hours)
        // Each segment represents 1 hour (15 degrees)
        val segmentAngle = 360f / 24f
        
        for (hour in 0..23) {
            // Convert hour to angle: solar noon (12) is at top
            // We want hour 12 at top (0 degrees in standard position = 12 o'clock)
            // Clock position: 12 o'clock = -90 degrees, 3 o'clock = 0, 6 o'clock = 90, 9 o'clock = 180
            // Hour 12 should be at top: (hour - 12) * segmentAngle - 90
            val baseAngle = (hour - 12) * segmentAngle - 90f
            
            // Draw arc segment with gradient color
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = hourColors[hour]
                style = Paint.Style.STROKE
                strokeWidth = ringThickness
                strokeCap = Paint.Cap.ROUND
            }
            
            // Create arc
            val rect = android.graphics.RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )
            
            canvas.drawArc(rect, baseAngle, segmentAngle, false, paint)
        }
        
        // Draw current hour in the center
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Get current hour
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hourText = String.format(Locale.getDefault(), "%02d", currentHour)
        
        // Calculate text position (center of canvas)
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(hourText, centerX, textY, textPaint)
        
        return bitmap
    }
    
    override fun onEnabled(context: Context) {
        // Widget enabled
    }
    
    override fun onDisabled(context: Context) {
        // Widget disabled
    }
}

