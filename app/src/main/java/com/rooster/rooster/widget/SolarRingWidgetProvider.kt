package com.rooster.rooster.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.rooster.rooster.MainActivity
import com.rooster.rooster.R
import com.rooster.rooster.data.local.AlarmDatabase
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
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
            
            // Get widget size to ensure ring fits properly
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            // Convert from dp to pixels using device density
            val displayMetrics = context.resources.displayMetrics
            val density = displayMetrics.density
            
            // Use minimum dimension to ensure ring is always circular (1:1)
            // When widget is not square, ring will be sized to smallest side
            val minDimensionDp = Math.min(minWidthDp, minHeightDp).coerceAtLeast(100)
            // Calculate size in pixels - use density to convert dp to pixels
            val size = (minDimensionDp * density).toInt()
            
            // Generate square bitmap with calculated size
            val bitmap = generateRingBitmap(context, astronomyData, size)
            
            // Create RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_solar_ring)
            views.setImageViewBitmap(R.id.widget_ring_image, bitmap)
            
            // Set click intent to open the app
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_ring_image, pendingIntent)
            
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
        astronomyData: AstronomyDataEntity?,
        size: Int = 512 // Square size based on minimum dimension
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Calculate radius for 1:1 rendering with minimal padding to prevent clipping
        // ringThickness/2 accounts for half the stroke width on each side
        // Small padding ensures the ring doesn't get clipped at edges
        val ringThickness = 67f // Reduced by a third (was 100f)
        val edgePadding = 8f // Small padding to prevent clipping
        val radius = (size / 2f) - (ringThickness / 2f) - edgePadding
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Create sweep gradient with color stops at each solar event
        // Positions are calculated relative to solar noon (0.0 = solar noon)
        val (colors, positions) = createGradientColorStops(astronomyData)
        
        // Create sweep gradient (starts at 0 degrees = 3 o'clock by default)
        val gradient = android.graphics.SweepGradient(
            centerX, centerY,
            colors.toIntArray(),
            positions.toFloatArray()
        )
        
        // Rotate gradient so solar noon (position 0.0) is at -90 degrees (12 o'clock position)
        val matrix = android.graphics.Matrix()
        matrix.setRotate(-90f, centerX, centerY)
        gradient.setLocalMatrix(matrix)
        
        // Draw the ring with gradient (single arc, not segments)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            style = Paint.Style.STROKE
            strokeWidth = ringThickness
            strokeCap = Paint.Cap.ROUND
        }
        
        // Create arc rectangle
        val rect = android.graphics.RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        // Draw full circle with gradient (start at -90 degrees = 12 o'clock)
        canvas.drawArc(rect, -90f, 360f, false, paint)
        
        // Draw solar event markers
        if (astronomyData != null) {
            drawSolarEventMarkers(context, canvas, centerX, centerY, radius, ringThickness, astronomyData)
        }
        
        // Draw current time marker
        drawCurrentTimeMarker(context, canvas, centerX, centerY, radius, ringThickness, astronomyData)
        
        // Get current time and date
        val calendar = Calendar.getInstance()
        
        // Format date as "Sun, Dec 28"
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val dateText = dateFormat.format(calendar.time)
        
        // Format time as "21:43"
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(calendar.time)
        
        // Draw date in the center (top)
        val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
        
        // Draw time below the date (larger)
        val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Calculate text positions
        // Date text - positioned in upper center area
        val dateTextY = centerY - 20f - (dateTextPaint.descent() + dateTextPaint.ascent()) / 2
        canvas.drawText(dateText, centerX, dateTextY, dateTextPaint)
        
        // Time text - positioned below date
        val timeTextY = centerY + 40f - (timeTextPaint.descent() + timeTextPaint.ascent()) / 2
        canvas.drawText(timeText, centerX, timeTextY, timeTextPaint)
        
        return bitmap
    }
    
    /**
     * Create gradient color stops at each solar event
     * Returns a Pair of (colors list, positions list) for SweepGradient
     * Positions are calculated relative to solar noon (0.0 = solar noon)
     */
    private fun createGradientColorStops(
        astronomyData: AstronomyDataEntity?
    ): Pair<List<Int>, List<Float>> {
        if (astronomyData == null) {
            // Default: single night color for full circle
            val nightColor = android.graphics.Color.parseColor("#2A2A3A")
            return Pair(listOf(nightColor, nightColor), listOf(0f, 1f))
        }
        
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Normalize astronomy data times to today
        fun normalizeTime(originalTime: Long): Long {
            if (originalTime <= 0) return 0
            calendar.timeInMillis = originalTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        
        // Create list of solar events with their times and colors
        val events = mutableListOf<Pair<Long, Int>>()
        
        // Add all solar events
        val normalizedData = astronomyData.copy(
            sunrise = normalizeTime(astronomyData.sunrise),
            sunset = normalizeTime(astronomyData.sunset),
            solarNoon = normalizeTime(astronomyData.solarNoon),
            civilDawn = normalizeTime(astronomyData.civilDawn),
            civilDusk = normalizeTime(astronomyData.civilDusk),
            nauticalDawn = normalizeTime(astronomyData.nauticalDawn),
            nauticalDusk = normalizeTime(astronomyData.nauticalDusk),
            astroDawn = normalizeTime(astronomyData.astroDawn),
            astroDusk = normalizeTime(astronomyData.astroDusk)
        )
        
        // Color constants - ensure smooth progression to brightest at solar noon
        val SOLAR_NOON_BRIGHTEST = android.graphics.Color.parseColor("#FFE8B8") // Brightest - solar noon
        val DAY_SKY_COLOR = android.graphics.Color.parseColor("#FFD89C") // Day sky
        val GOLDEN_HOUR_COLOR = android.graphics.Color.parseColor("#FFB86C") // Golden hour
        val SUNRISE_COLOR = android.graphics.Color.parseColor("#FF8E53") // Sunrise
        
        // Helper function to interpolate colors (defined first so it can be used by other functions)
        fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
            val clampedFactor = factor.coerceIn(0f, 1f)
            val a1 = android.graphics.Color.alpha(color1)
            val r1 = android.graphics.Color.red(color1)
            val g1 = android.graphics.Color.green(color1)
            val b1 = android.graphics.Color.blue(color1)
            val a2 = android.graphics.Color.alpha(color2)
            val r2 = android.graphics.Color.red(color2)
            val g2 = android.graphics.Color.green(color2)
            val b2 = android.graphics.Color.blue(color2)
            val a = (a1 + (a2 - a1) * clampedFactor).toInt()
            val r = (r1 + (r2 - r1) * clampedFactor).toInt()
            val g = (g1 + (g2 - g1) * clampedFactor).toInt()
            val b = (b1 + (b2 - b1) * clampedFactor).toInt()
            return android.graphics.Color.argb(a, r, g, b)
        }
        
        // Add events with their colors - in chronological order
        if (normalizedData.astroDawn > 0) {
            events.add(Pair(normalizedData.astroDawn, SolarColorCalculator.getColorForTime(normalizedData.astroDawn, normalizedData)))
        }
        if (normalizedData.nauticalDawn > 0) {
            events.add(Pair(normalizedData.nauticalDawn, SolarColorCalculator.getColorForTime(normalizedData.nauticalDawn, normalizedData)))
        }
        if (normalizedData.civilDawn > 0) {
            events.add(Pair(normalizedData.civilDawn, SolarColorCalculator.getColorForTime(normalizedData.civilDawn, normalizedData)))
        }
        if (normalizedData.sunrise > 0) {
            events.add(Pair(normalizedData.sunrise, SUNRISE_COLOR))
        }
        
        // Calculate the color just before/after solar noon once to ensure perfect match on both sides
        val nearNoonColor = if (normalizedData.solarNoon > 0) {
            interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.97f)
        } else {
            DAY_SKY_COLOR
        }
        
        // Add smooth gradient between sunrise and solar noon with multiple intermediate stops
        if (normalizedData.sunrise > 0 && normalizedData.solarNoon > 0 && normalizedData.sunrise < normalizedData.solarNoon) {
            // Add multiple intermediate stops for very smooth transition
            // Sunrise -> Golden Hour -> Day Sky -> Brighter Day Sky -> Solar Noon
            val sunriseToNoonRange = normalizedData.solarNoon - normalizedData.sunrise
            
            // Golden hour point (~20% of the way)
            val goldenHourTime = normalizedData.sunrise + (sunriseToNoonRange * 0.2f).toLong()
            events.add(Pair(goldenHourTime, GOLDEN_HOUR_COLOR))
            
            // Day sky point (~40% of the way)
            val daySkyTime1 = normalizedData.sunrise + (sunriseToNoonRange * 0.4f).toLong()
            events.add(Pair(daySkyTime1, DAY_SKY_COLOR))
            
            // Brighter day sky point (~60% of the way)
            val daySkyTime2 = normalizedData.sunrise + (sunriseToNoonRange * 0.6f).toLong()
            val brighterDaySky1 = interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.3f)
            events.add(Pair(daySkyTime2, brighterDaySky1))
            
            // Even brighter point (~80% of the way) - transitioning to solar noon
            val daySkyTime3 = normalizedData.sunrise + (sunriseToNoonRange * 0.8f).toLong()
            val brighterDaySky2 = interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.6f)
            events.add(Pair(daySkyTime3, brighterDaySky2))
            
            // Add point just before solar noon using the pre-calculated color
            val nearNoonTimeBefore = normalizedData.solarNoon - (sunriseToNoonRange * 0.02f).toLong()
            events.add(Pair(nearNoonTimeBefore, nearNoonColor))
        }
        
        // Solar noon - MUST be the brightest and at exact position
        if (normalizedData.solarNoon > 0) {
            events.add(Pair(normalizedData.solarNoon, SOLAR_NOON_BRIGHTEST))
        }
        
        // Add smooth gradient between solar noon and sunset with multiple intermediate stops
        if (normalizedData.solarNoon > 0 && normalizedData.sunset > 0 && normalizedData.solarNoon < normalizedData.sunset) {
            val noonToSunsetRange = normalizedData.sunset - normalizedData.solarNoon
            
            // Add point just after solar noon using the EXACT SAME pre-calculated color
            val nearNoonTimeAfter = normalizedData.solarNoon + (noonToSunsetRange * 0.02f).toLong()
            events.add(Pair(nearNoonTimeAfter, nearNoonColor))
            
            // Brighter day sky point (~20% of the way)
            val daySkyTime1 = normalizedData.solarNoon + (noonToSunsetRange * 0.2f).toLong()
            val brighterDaySky1 = interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.6f)
            events.add(Pair(daySkyTime1, brighterDaySky1))
            
            // Brighter day sky point (~40% of the way)
            val daySkyTime2 = normalizedData.solarNoon + (noonToSunsetRange * 0.4f).toLong()
            val brighterDaySky2 = interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.3f)
            events.add(Pair(daySkyTime2, brighterDaySky2))
            
            // Day sky point (~60% of the way)
            val daySkyTime3 = normalizedData.solarNoon + (noonToSunsetRange * 0.6f).toLong()
            events.add(Pair(daySkyTime3, DAY_SKY_COLOR))
            
            // Golden hour point (~80% of the way)
            val goldenHourTime = normalizedData.solarNoon + (noonToSunsetRange * 0.8f).toLong()
            events.add(Pair(goldenHourTime, GOLDEN_HOUR_COLOR))
        }
        
        if (normalizedData.sunset > 0) {
            events.add(Pair(normalizedData.sunset, SUNRISE_COLOR))
        }
        if (normalizedData.civilDusk > 0) {
            events.add(Pair(normalizedData.civilDusk, SolarColorCalculator.getColorForTime(normalizedData.civilDusk, normalizedData)))
        }
        if (normalizedData.nauticalDusk > 0) {
            events.add(Pair(normalizedData.nauticalDusk, SolarColorCalculator.getColorForTime(normalizedData.nauticalDusk, normalizedData)))
        }
        if (normalizedData.astroDusk > 0) {
            events.add(Pair(normalizedData.astroDusk, SolarColorCalculator.getColorForTime(normalizedData.astroDusk, normalizedData)))
        }
        
        val nightColor = android.graphics.Color.parseColor("#2A2A3A")
        
        // Convert times to angles and then to positions (0.0 to 1.0)
        // Positions are calculated relative to solar noon (which will be at position 0.0)
        // The gradient will be rotated so position 0.0 aligns with -90 + solarNoonOffset (top)
        fun timeToPosition(time: Long): Float {
            // Calculate time difference from solar noon in milliseconds
            val solarNoonTime = normalizedData.solarNoon
            if (solarNoonTime <= 0) {
                // Fallback: calculate relative to hour 12:00
                calendar.timeInMillis = time
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val hourOffset = hour - 12
                val minuteOffset = minute / 60f
                val angleFromSolarNoon = (hourOffset + minuteOffset) * 15f
                var position = angleFromSolarNoon / 360f
                while (position < 0) position += 1f
                while (position >= 1) position -= 1f
                return position
            }
            
            // Calculate time difference from solar noon
            var timeDiff = time - solarNoonTime
            
            // Handle wrap-around (times before solar noon in the same day)
            // If time is before solar noon, it should be negative, which is correct
            // But we need to handle the case where time is from the previous day
            val dayInMillis = 24 * 60 * 60 * 1000L
            if (timeDiff > dayInMillis / 2) {
                timeDiff -= dayInMillis
            } else if (timeDiff < -dayInMillis / 2) {
                timeDiff += dayInMillis
            }
            
            // Convert time difference to angle (each hour = 15 degrees)
            val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
            val angleFromSolarNoon = hoursFromSolarNoon * 15f
            
            // Convert to position (0.0 to 1.0), where 0.0 is solar noon
            var position = angleFromSolarNoon / 360f
            
            // Normalize to 0-1 range
            while (position < 0) position += 1f
            while (position >= 1) position -= 1f
            
            return position
        }
        
        // Add midnight (00:00) to ensure full circle coverage
        val midnight = todayStart
        events.add(Pair(midnight, nightColor))
        
        // Create list of events with positions, ensuring solar noon is exactly at 0.0
        val eventPositions = mutableListOf<Pair<Float, Int>>()
        var solarNoonColor: Int? = null
        
        for (event in events) {
            // If this is solar noon, force position to exactly 0.0
            if (event.first == normalizedData.solarNoon && normalizedData.solarNoon > 0) {
                solarNoonColor = event.second
                eventPositions.add(Pair(0.0f, event.second)) // Force to exactly 0.0
            } else {
                val pos = timeToPosition(event.first)
                eventPositions.add(Pair(pos, event.second))
            }
        }
        
        // Sort by position
        val sortedEvents = eventPositions.sortedBy { it.first }
        
        // Build color and position arrays, removing duplicates but preserving solar noon
        val colors = mutableListOf<Int>()
        val positions = mutableListOf<Float>()
        
        var lastPosition = -1f
        val POSITION_EPSILON = 0.0001f // Smaller threshold for considering positions equal
        
        // Add all event positions (they're already sorted)
        // SweepGradient will automatically handle wrap-around from 1.0 to 0.0
        for ((pos, color) in sortedEvents) {
            // Always include solar noon (position 0.0) - never skip it
            val isSolarNoon = (pos == 0.0f && solarNoonColor != null && color == solarNoonColor)
            
            // Skip if this position is too close to the previous one (duplicate), but not if it's solar noon
            if (!isSolarNoon && lastPosition >= 0 && kotlin.math.abs(pos - lastPosition) < POSITION_EPSILON) {
                // If we have a duplicate, prefer the brighter color (higher RGB values)
                val lastColor = colors.last()
                val lastBrightness = android.graphics.Color.red(lastColor) + android.graphics.Color.green(lastColor) + android.graphics.Color.blue(lastColor)
                val currentBrightness = android.graphics.Color.red(color) + android.graphics.Color.green(color) + android.graphics.Color.blue(color)
                if (currentBrightness > lastBrightness) {
                    // Replace with brighter color
                    colors[colors.size - 1] = color
                }
                continue
            }
            
            colors.add(color)
            positions.add(pos)
            lastPosition = pos
        }
        
        // Ensure solar noon (0.0) is at the start of the arrays for proper gradient alignment
        if (solarNoonColor != null) {
            val solarNoonIndex = positions.indexOfFirst { it == 0.0f }
            if (solarNoonIndex > 0) {
                // Move solar noon to the beginning
                val color = colors.removeAt(solarNoonIndex)
                val pos = positions.removeAt(solarNoonIndex)
                colors.add(0, color)
                positions.add(0, pos)
            } else if (solarNoonIndex < 0) {
                // Solar noon not found, add it at the beginning
                colors.add(0, solarNoonColor)
                positions.add(0, 0.0f)
            }
        }
        
        // Ensure we have at least 2 points for a valid gradient
        if (colors.size < 2) {
            colors.clear()
            positions.clear()
            colors.add(nightColor)
            positions.add(0f)
            colors.add(nightColor)
            positions.add(1f)
        }
        
        return Pair(colors, positions)
    }
    
    /**
     * Draw markers for solar events on the ring
     */
    private fun drawSolarEventMarkers(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        ringThickness: Float,
        astronomyData: AstronomyDataEntity
    ) {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Normalize solar event times to today
        fun normalizeTime(originalTime: Long): Long {
            calendar.timeInMillis = originalTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        
        // Normalize solar noon to today
        val normalizedSolarNoon = if (astronomyData.solarNoon > 0) {
            normalizeTime(astronomyData.solarNoon)
        } else {
            0L
        }
        
        val events = listOf(
            Pair("🌌", normalizeTime(astronomyData.astroDawn)),
            Pair("🌃", normalizeTime(astronomyData.nauticalDawn)),
            Pair("🌆", normalizeTime(astronomyData.civilDawn)),
            Pair("🌅", normalizeTime(astronomyData.sunrise)),
            Pair("☀️", normalizedSolarNoon),
            Pair("🌇", normalizeTime(astronomyData.sunset)),
            Pair("🌆", normalizeTime(astronomyData.civilDusk)),
            Pair("🌃", normalizeTime(astronomyData.nauticalDusk)),
            Pair("🌌", normalizeTime(astronomyData.astroDusk))
        )
        
        for ((emoji, eventTime) in events) {
            if (eventTime <= 0) continue
            
            // Calculate angle relative to solar noon (not 12:00)
            // If no solar noon data, fall back to 12:00
            val angle = if (normalizedSolarNoon > 0) {
                // Calculate time difference from solar noon
                var timeDiff = eventTime - normalizedSolarNoon
                
                // Handle wrap-around (times before solar noon in the same day)
                val dayInMillis = 24 * 60 * 60 * 1000L
                if (timeDiff > dayInMillis / 2) {
                    timeDiff -= dayInMillis
                } else if (timeDiff < -dayInMillis / 2) {
                    timeDiff += dayInMillis
                }
                
                // Convert time difference to angle (each hour = 15 degrees)
                val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
                val angleFromSolarNoon = hoursFromSolarNoon * 15f
                
                // Start from -90 degrees (12 o'clock position) and add the angle offset
                -90f + angleFromSolarNoon
            } else {
                // Fallback: calculate relative to 12:00
                val eventCalendar = Calendar.getInstance()
                eventCalendar.timeInMillis = eventTime
                val eventHour = eventCalendar.get(Calendar.HOUR_OF_DAY)
                val eventMinute = eventCalendar.get(Calendar.MINUTE)
                val hourOffset = eventHour - 12
                val minuteOffset = eventMinute / 60f
                val totalHourOffset = hourOffset + minuteOffset
                totalHourOffset * 15f - 90f
            }
            
            // Convert to radians for drawing
            // Position markers at the inner edge of the ring (radius - ringThickness/2)
            // and slightly more inside for better visibility
            val innerRadius = radius - (ringThickness / 2f) - 8f
            val angleRad = Math.toRadians(angle.toDouble())
            val markerX = centerX + (innerRadius * Math.cos(angleRad)).toFloat()
            val markerY = centerY + (innerRadius * Math.sin(angleRad)).toFloat()
            
            // Draw marker circle
            val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.md_theme_dark_background)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(markerX, markerY, 12f, markerPaint)
            
            // Draw emoji
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(emoji, markerX, markerY + 6f, emojiPaint)
        }
    }
    
    /**
     * Draw marker for current time on the ring
     */
    private fun drawCurrentTimeMarker(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        ringThickness: Float,
        astronomyData: AstronomyDataEntity?
    ) {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        // Calculate angle relative to solar noon (not 12:00)
        val angle = if (astronomyData != null && astronomyData.solarNoon > 0) {
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            // Normalize solar noon to today
            calendar.timeInMillis = astronomyData.solarNoon
            val solarNoonHour = calendar.get(Calendar.HOUR_OF_DAY)
            val solarNoonMinute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, solarNoonHour)
            calendar.set(Calendar.MINUTE, solarNoonMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val normalizedSolarNoon = calendar.timeInMillis
            
            // Calculate time difference from solar noon
            var timeDiff = currentTime - normalizedSolarNoon
            
            // Handle wrap-around (times before solar noon in the same day)
            val dayInMillis = 24 * 60 * 60 * 1000L
            if (timeDiff > dayInMillis / 2) {
                timeDiff -= dayInMillis
            } else if (timeDiff < -dayInMillis / 2) {
                timeDiff += dayInMillis
            }
            
            // Convert time difference to angle (each hour = 15 degrees)
            val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
            val angleFromSolarNoon = hoursFromSolarNoon * 15f
            
            // Start from -90 degrees (12 o'clock position) and add the angle offset
            -90f + angleFromSolarNoon
        } else {
            // Fallback: calculate relative to 12:00
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val hourOffset = currentHour - 12
            val minuteOffset = currentMinute / 60f
            val totalHourOffset = hourOffset + minuteOffset
            totalHourOffset * 15f - 90f
        }
        
        // Convert to radians for drawing
        // Position current time marker at the inner edge of the ring (radius - ringThickness/2)
        // and slightly more inside for better visibility
        val innerRadius = radius - (ringThickness / 2f) - 8f
        val angleRad = Math.toRadians(angle.toDouble())
        val markerX = centerX + (innerRadius * Math.cos(angleRad)).toFloat()
        val markerY = centerY + (innerRadius * Math.sin(angleRad)).toFloat()
        
        // Draw current time marker (larger, more prominent)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.accent_coral)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(markerX, markerY, 16f, markerPaint)
        
        // Draw white border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.white)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(markerX, markerY, 16f, borderPaint)
    }
    
    override fun onEnabled(context: Context) {
        // Widget enabled
    }
    
    override fun onDisabled(context: Context) {
        // Widget disabled
    }
}

