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
        val ringThickness = 100f
        val edgePadding = 8f // Small padding to prevent clipping
        val radius = (size / 2f) - (ringThickness / 2f) - edgePadding
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Calculate solar noon offset angle
        // Solar noon should be at the top (12 o'clock position = -90 degrees)
        val solarNoonOffset = calculateSolarNoonOffset(astronomyData)
        
        // Create sweep gradient with color stops at each solar event
        val (colors, positions) = createGradientColorStops(astronomyData, solarNoonOffset)
        
        // Create sweep gradient (starts at 0 degrees = 3 o'clock by default)
        val gradient = android.graphics.SweepGradient(
            centerX, centerY,
            colors.toIntArray(),
            positions.toFloatArray()
        )
        
        // Rotate gradient so it starts at -90 degrees (12 o'clock) + solarNoonOffset
        val matrix = android.graphics.Matrix()
        matrix.setRotate(-90f + solarNoonOffset, centerX, centerY)
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
        
        // Draw full circle with gradient
        canvas.drawArc(rect, -90f + solarNoonOffset, 360f, false, paint)
        
        // Draw solar event markers
        if (astronomyData != null) {
            drawSolarEventMarkers(context, canvas, centerX, centerY, radius, astronomyData, solarNoonOffset)
        }
        
        // Draw current time marker
        drawCurrentTimeMarker(context, canvas, centerX, centerY, radius, solarNoonOffset)
        
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
    
    /**
     * Create gradient color stops at each solar event
     * Returns a Pair of (colors list, positions list) for SweepGradient
     * Positions are calculated relative to 0 degrees (3 o'clock), gradient will be rotated
     */
    private fun createGradientColorStops(
        astronomyData: AstronomyDataEntity?,
        solarNoonOffset: Float
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
        
        // Color constants for solar noon (brightest)
        val SOLAR_NOON_BRIGHTEST = android.graphics.Color.parseColor("#FFE8B8") // Brighter than day sky
        val GOLDEN_HOUR_COLOR = android.graphics.Color.parseColor("#FFB86C")
        val SUNRISE_COLOR = android.graphics.Color.parseColor("#FF8E53")
        
        // Add events with their colors
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
        
        // Add intermediate points between sunrise and solar noon for smoother gradient
        if (normalizedData.sunrise > 0 && normalizedData.solarNoon > 0 && normalizedData.sunrise < normalizedData.solarNoon) {
            val sunriseToNoonRange = normalizedData.solarNoon - normalizedData.sunrise
            // Add golden hour point (approximately 1/3 of the way from sunrise to solar noon)
            val goldenHourTime = normalizedData.sunrise + (sunriseToNoonRange * 0.33f).toLong()
            events.add(Pair(goldenHourTime, GOLDEN_HOUR_COLOR))
            // Add day sky point (approximately 2/3 of the way from sunrise to solar noon)
            val daySkyTime = normalizedData.sunrise + (sunriseToNoonRange * 0.67f).toLong()
            val DAY_SKY_COLOR = android.graphics.Color.parseColor("#FFD89C")
            events.add(Pair(daySkyTime, DAY_SKY_COLOR))
        }
        
        if (normalizedData.solarNoon > 0) {
            // Use brightest color for solar noon
            events.add(Pair(normalizedData.solarNoon, SOLAR_NOON_BRIGHTEST))
        }
        
        // Add intermediate points between solar noon and sunset for smoother gradient
        if (normalizedData.solarNoon > 0 && normalizedData.sunset > 0 && normalizedData.solarNoon < normalizedData.sunset) {
            val noonToSunsetRange = normalizedData.sunset - normalizedData.solarNoon
            // Add day sky point (approximately 1/3 of the way from solar noon to sunset)
            val daySkyTime = normalizedData.solarNoon + (noonToSunsetRange * 0.33f).toLong()
            val DAY_SKY_COLOR = android.graphics.Color.parseColor("#FFD89C")
            events.add(Pair(daySkyTime, DAY_SKY_COLOR))
            // Add golden hour point (approximately 2/3 of the way from solar noon to sunset)
            val goldenHourTime = normalizedData.solarNoon + (noonToSunsetRange * 0.67f).toLong()
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
        
        // Create sorted list of events by position
        val eventPositions = events.map { event ->
            Pair(timeToPosition(event.first), event.second)
        }.sortedBy { it.first }
        
        // Build color and position arrays, removing duplicates
        val colors = mutableListOf<Int>()
        val positions = mutableListOf<Float>()
        
        var lastPosition = -1f
        val POSITION_EPSILON = 0.001f // Small threshold for considering positions equal
        
        // Add all event positions (they're already sorted)
        // SweepGradient will automatically handle wrap-around from 1.0 to 0.0
        for ((pos, color) in eventPositions) {
            // Skip if this position is too close to the previous one (duplicate)
            if (lastPosition >= 0 && kotlin.math.abs(pos - lastPosition) < POSITION_EPSILON) {
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
        astronomyData: AstronomyDataEntity,
        solarNoonOffset: Float
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
        
        val events = listOf(
            Pair("🌌", normalizeTime(astronomyData.astroDawn)),
            Pair("🌃", normalizeTime(astronomyData.nauticalDawn)),
            Pair("🌆", normalizeTime(astronomyData.civilDawn)),
            Pair("🌅", normalizeTime(astronomyData.sunrise)),
            Pair("☀️", normalizeTime(astronomyData.solarNoon)),
            Pair("🌇", normalizeTime(astronomyData.sunset)),
            Pair("🌆", normalizeTime(astronomyData.civilDusk)),
            Pair("🌃", normalizeTime(astronomyData.nauticalDusk)),
            Pair("🌌", normalizeTime(astronomyData.astroDusk))
        )
        
        for ((emoji, eventTime) in events) {
            if (eventTime <= 0) continue
            
            // Calculate angle for this event
            val eventCalendar = Calendar.getInstance()
            eventCalendar.timeInMillis = eventTime
            val eventHour = eventCalendar.get(Calendar.HOUR_OF_DAY)
            val eventMinute = eventCalendar.get(Calendar.MINUTE)
            
            // Calculate angle: (hour - 12) * 15 + (minute / 60) * 15 - 90 + solarNoonOffset
            val hourOffset = eventHour - 12
            val minuteOffset = eventMinute / 60f
            val totalHourOffset = hourOffset + minuteOffset
            val angle = totalHourOffset * 15f - 90f + solarNoonOffset
            
            // Convert to radians for drawing
            val angleRad = Math.toRadians(angle.toDouble())
            val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
            val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()
            
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
        solarNoonOffset: Float
    ) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Calculate angle for current time
        val hourOffset = currentHour - 12
        val minuteOffset = currentMinute / 60f
        val totalHourOffset = hourOffset + minuteOffset
        val angle = totalHourOffset * 15f - 90f + solarNoonOffset
        
        // Convert to radians for drawing
        val angleRad = Math.toRadians(angle.toDouble())
        val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
        val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()
        
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

