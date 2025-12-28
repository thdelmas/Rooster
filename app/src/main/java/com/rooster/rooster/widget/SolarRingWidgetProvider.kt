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
        
        // Ensure ring fits completely within bounds with padding
        // ringThickness/2 accounts for half the stroke width on each side
        val padding = 15f // Padding from edges to ensure full visibility
        val ringThickness = 35f
        val radius = (size / 2f) - (ringThickness / 2f) - padding
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Get colors for all 24 hours
        val hourColors = SolarColorCalculator.getColorsFor24Hours(astronomyData)
        
        // Calculate solar noon offset angle
        // Solar noon should be at the top (12 o'clock position = -90 degrees)
        val solarNoonOffset = calculateSolarNoonOffset(astronomyData)
        
        // Draw the ring segment by segment (24 segments for 24 hours)
        // Each segment represents 1 hour (15 degrees)
        val segmentAngle = 360f / 24f
        
        for (hour in 0..23) {
            // Convert hour to angle: solar noon should be at top
            // Start with hour 12 at top, then apply solar noon offset
            // Clock position: 12 o'clock = -90 degrees, 3 o'clock = 0, 6 o'clock = 90, 9 o'clock = 180
            // Hour 12 should be at top: (hour - 12) * segmentAngle - 90
            // Then offset by solar noon position
            val baseAngle = (hour - 12) * segmentAngle - 90f + solarNoonOffset
            
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
        
        // Get current time
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Draw current hour in the center (larger, top)
        val hourTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val hourText = String.format(Locale.getDefault(), "%02d", currentHour)
        
        // Draw current minutes below the hour (smaller)
        val minuteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val minuteText = String.format(Locale.getDefault(), "%02d", currentMinute)
        
        // Calculate text positions
        // Hour text - positioned in upper center area
        val hourTextY = centerY - 20f - (hourTextPaint.descent() + hourTextPaint.ascent()) / 2
        canvas.drawText(hourText, centerX, hourTextY, hourTextPaint)
        
        // Minute text - positioned below hour
        val minuteTextY = centerY + 40f - (minuteTextPaint.descent() + minuteTextPaint.ascent()) / 2
        canvas.drawText(minuteText, centerX, minuteTextY, minuteTextPaint)
        
        return bitmap
    }
    
    /**
     * Calculate the offset angle to align solar noon at the top (12 o'clock position)
     * Returns the angle in degrees to rotate the ring
     */
    private fun calculateSolarNoonOffset(astronomyData: AstronomyDataEntity?): Float {
        if (astronomyData == null || astronomyData.solarNoon <= 0) {
            return 0f // No offset if no solar noon data
        }
        
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Get solar noon time for today
        calendar.timeInMillis = astronomyData.solarNoon
        val solarNoonHour = calendar.get(Calendar.HOUR_OF_DAY)
        val solarNoonMinute = calendar.get(Calendar.MINUTE)
        
        // Normalize solar noon to today
        calendar.timeInMillis = todayStart
        calendar.set(Calendar.HOUR_OF_DAY, solarNoonHour)
        calendar.set(Calendar.MINUTE, solarNoonMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val solarNoonToday = calendar.timeInMillis
        
        // Calculate the offset from hour 12 (midday)
        // Each hour is 15 degrees (360 / 24)
        // Each minute is 0.25 degrees (15 / 60)
        val hourOffset = solarNoonHour - 12
        val minuteOffset = solarNoonMinute / 60f
        val totalHourOffset = hourOffset + minuteOffset
        
        // Convert to angle offset (negative because we want to rotate counter-clockwise if solar noon is after 12:00)
        val angleOffset = -totalHourOffset * 15f
        
        return angleOffset
    }
    
    override fun onEnabled(context: Context) {
        // Widget enabled
    }
    
    override fun onDisabled(context: Context) {
        // Widget disabled
    }
}

