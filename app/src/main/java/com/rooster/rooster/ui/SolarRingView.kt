package com.rooster.rooster.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.widget.SolarColorCalculator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom view that displays a solar ring with gradient colors representing solar events
 * Reusable version of the solar ring from SolarRingWidgetProvider
 */
class SolarRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var astronomyData: AstronomyDataEntity? = null
    private var showDateAndTime: Boolean = true
    private var showCurrentTimeMarker: Boolean = true
    private var showSolarEventMarkers: Boolean = true
    
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    init {
        // Load default colors
        setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
    }
    
    /**
     * Set astronomy data for the ring
     */
    fun setAstronomyData(data: AstronomyDataEntity?) {
        astronomyData = data
        invalidate()
    }
    
    /**
     * Configure what to display
     */
    fun configure(
        showDateAndTime: Boolean = this.showDateAndTime,
        showCurrentTimeMarker: Boolean = this.showCurrentTimeMarker,
        showSolarEventMarkers: Boolean = this.showSolarEventMarkers
    ) {
        this.showDateAndTime = showDateAndTime
        this.showCurrentTimeMarker = showCurrentTimeMarker
        this.showSolarEventMarkers = showSolarEventMarkers
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = minOf(width, height)
        if (size <= 0) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Calculate radius
        val ringThickness = (size * 67f / 512f).coerceAtLeast(20f).coerceAtMost(100f)
        val edgePadding = 8f
        val radius = (size / 2f) - (ringThickness / 2f) - edgePadding
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Create and draw gradient ring
        val (colors, positions) = createGradientColorStops(astronomyData)
        val gradient = android.graphics.SweepGradient(
            centerX, centerY,
            colors.toIntArray(),
            positions.toFloatArray()
        )
        
        // Rotate gradient so solar noon is at 12 o'clock
        val matrix = android.graphics.Matrix()
        matrix.setRotate(-90f, centerX, centerY)
        gradient.setLocalMatrix(matrix)
        
        ringPaint.apply {
            shader = gradient
            strokeWidth = ringThickness
        }
        
        val rect = android.graphics.RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        canvas.drawArc(rect, -90f, 360f, false, ringPaint)
        
        // Draw solar event markers
        if (showSolarEventMarkers && astronomyData != null) {
            drawSolarEventMarkers(canvas, centerX, centerY, radius, ringThickness, astronomyData!!)
        }
        
        // Draw current time marker
        if (showCurrentTimeMarker) {
            drawCurrentTimeMarker(canvas, centerX, centerY, radius, ringThickness, astronomyData)
        }
        
        // Draw date and time in center
        if (showDateAndTime) {
            drawCenterText(canvas, centerX, centerY)
        }
    }
    
    /**
     * Create gradient color stops at each solar event
     * This is extracted from SolarRingWidgetProvider
     */
    private fun createGradientColorStops(
        astronomyData: AstronomyDataEntity?
    ): Pair<List<Int>, List<Float>> {
        if (astronomyData == null) {
            val nightColor = Color.parseColor("#2A2A3A")
            return Pair(listOf(nightColor, nightColor), listOf(0f, 1f))
        }
        
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
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
        
        val SOLAR_NOON_BRIGHTEST = Color.parseColor("#FFE8B8")
        val DAY_SKY_COLOR = Color.parseColor("#FFD89C")
        val GOLDEN_HOUR_COLOR = Color.parseColor("#FFB86C")
        val SUNRISE_COLOR = Color.parseColor("#FF8E53")
        val nightColor = Color.parseColor("#2A2A3A")
        
        fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
            val clampedFactor = factor.coerceIn(0f, 1f)
            val a1 = Color.alpha(color1)
            val r1 = Color.red(color1)
            val g1 = Color.green(color1)
            val b1 = Color.blue(color1)
            val a2 = Color.alpha(color2)
            val r2 = Color.red(color2)
            val g2 = Color.green(color2)
            val b2 = Color.blue(color2)
            val a = (a1 + (a2 - a1) * clampedFactor).toInt()
            val r = (r1 + (r2 - r1) * clampedFactor).toInt()
            val g = (g1 + (g2 - g1) * clampedFactor).toInt()
            val b = (b1 + (b2 - b1) * clampedFactor).toInt()
            return Color.argb(a, r, g, b)
        }
        
        val events = mutableListOf<Pair<Long, Int>>()
        
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
        
        val nearNoonColor = if (normalizedData.solarNoon > 0) {
            interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.97f)
        } else {
            DAY_SKY_COLOR
        }
        
        if (normalizedData.sunrise > 0 && normalizedData.solarNoon > 0 && normalizedData.sunrise < normalizedData.solarNoon) {
            val sunriseToNoonRange = normalizedData.solarNoon - normalizedData.sunrise
            
            val goldenHourTime = normalizedData.sunrise + (sunriseToNoonRange * 0.2f).toLong()
            events.add(Pair(goldenHourTime, GOLDEN_HOUR_COLOR))
            
            val daySkyTime1 = normalizedData.sunrise + (sunriseToNoonRange * 0.4f).toLong()
            events.add(Pair(daySkyTime1, DAY_SKY_COLOR))
            
            val daySkyTime2 = normalizedData.sunrise + (sunriseToNoonRange * 0.6f).toLong()
            val brighterDaySky1 = interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.3f)
            events.add(Pair(daySkyTime2, brighterDaySky1))
            
            val daySkyTime3 = normalizedData.sunrise + (sunriseToNoonRange * 0.8f).toLong()
            val brighterDaySky2 = interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.6f)
            events.add(Pair(daySkyTime3, brighterDaySky2))
            
            val nearNoonTimeBefore = normalizedData.solarNoon - (sunriseToNoonRange * 0.02f).toLong()
            events.add(Pair(nearNoonTimeBefore, nearNoonColor))
        }
        
        if (normalizedData.solarNoon > 0) {
            events.add(Pair(normalizedData.solarNoon, SOLAR_NOON_BRIGHTEST))
        }
        
        if (normalizedData.solarNoon > 0 && normalizedData.sunset > 0 && normalizedData.solarNoon < normalizedData.sunset) {
            val noonToSunsetRange = normalizedData.sunset - normalizedData.solarNoon
            
            val nearNoonTimeAfter = normalizedData.solarNoon + (noonToSunsetRange * 0.02f).toLong()
            events.add(Pair(nearNoonTimeAfter, nearNoonColor))
            
            val daySkyTime1 = normalizedData.solarNoon + (noonToSunsetRange * 0.2f).toLong()
            val brighterDaySky1 = interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.6f)
            events.add(Pair(daySkyTime1, brighterDaySky1))
            
            val daySkyTime2 = normalizedData.solarNoon + (noonToSunsetRange * 0.4f).toLong()
            val brighterDaySky2 = interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.3f)
            events.add(Pair(daySkyTime2, brighterDaySky2))
            
            val daySkyTime3 = normalizedData.solarNoon + (noonToSunsetRange * 0.6f).toLong()
            events.add(Pair(daySkyTime3, DAY_SKY_COLOR))
            
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
        
        events.add(Pair(todayStart, nightColor))
        
        fun timeToPosition(time: Long): Float {
            val solarNoonTime = normalizedData.solarNoon
            if (solarNoonTime <= 0) {
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
            
            var timeDiff = time - solarNoonTime
            val dayInMillis = 24 * 60 * 60 * 1000L
            if (timeDiff > dayInMillis / 2) {
                timeDiff -= dayInMillis
            } else if (timeDiff < -dayInMillis / 2) {
                timeDiff += dayInMillis
            }
            
            val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
            val angleFromSolarNoon = hoursFromSolarNoon * 15f
            var position = angleFromSolarNoon / 360f
            while (position < 0) position += 1f
            while (position >= 1) position -= 1f
            return position
        }
        
        val eventPositions = mutableListOf<Pair<Float, Int>>()
        var solarNoonColor: Int? = null
        
        for (event in events) {
            if (event.first == normalizedData.solarNoon && normalizedData.solarNoon > 0) {
                solarNoonColor = event.second
                eventPositions.add(Pair(0.0f, event.second))
            } else {
                val pos = timeToPosition(event.first)
                eventPositions.add(Pair(pos, event.second))
            }
        }
        
        val sortedEvents = eventPositions.sortedBy { it.first }
        val colors = mutableListOf<Int>()
        val positions = mutableListOf<Float>()
        
        var lastPosition = -1f
        val POSITION_EPSILON = 0.0001f
        
        for ((pos, color) in sortedEvents) {
            val isSolarNoon = (pos == 0.0f && solarNoonColor != null && color == solarNoonColor)
            
            if (!isSolarNoon && lastPosition >= 0 && kotlin.math.abs(pos - lastPosition) < POSITION_EPSILON) {
                val lastColor = colors.last()
                val lastBrightness = Color.red(lastColor) + Color.green(lastColor) + Color.blue(lastColor)
                val currentBrightness = Color.red(color) + Color.green(color) + Color.blue(color)
                if (currentBrightness > lastBrightness) {
                    colors[colors.size - 1] = color
                }
                continue
            }
            
            colors.add(color)
            positions.add(pos)
            lastPosition = pos
        }
        
        if (solarNoonColor != null) {
            val solarNoonIndex = positions.indexOfFirst { it == 0.0f }
            if (solarNoonIndex > 0) {
                val color = colors.removeAt(solarNoonIndex)
                val pos = positions.removeAt(solarNoonIndex)
                colors.add(0, color)
                positions.add(0, pos)
            } else if (solarNoonIndex < 0) {
                colors.add(0, solarNoonColor)
                positions.add(0, 0.0f)
            }
        }
        
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
            
            val angle = if (normalizedSolarNoon > 0) {
                var timeDiff = eventTime - normalizedSolarNoon
                val dayInMillis = 24 * 60 * 60 * 1000L
                if (timeDiff > dayInMillis / 2) {
                    timeDiff -= dayInMillis
                } else if (timeDiff < -dayInMillis / 2) {
                    timeDiff += dayInMillis
                }
                val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
                val angleFromSolarNoon = hoursFromSolarNoon * 15f
                -90f + angleFromSolarNoon
            } else {
                val eventCalendar = Calendar.getInstance()
                eventCalendar.timeInMillis = eventTime
                val eventHour = eventCalendar.get(Calendar.HOUR_OF_DAY)
                val eventMinute = eventCalendar.get(Calendar.MINUTE)
                val hourOffset = eventHour - 12
                val minuteOffset = eventMinute / 60f
                val totalHourOffset = hourOffset + minuteOffset
                totalHourOffset * 15f - 90f
            }
            
            val angleRad = Math.toRadians(angle.toDouble())
            val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
            val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()
            
            markerPaint.color = ContextCompat.getColor(context, R.color.md_theme_dark_background)
            canvas.drawCircle(markerX, markerY, 20f, markerPaint)
            
            textPaint.apply {
                textSize = 32f
                color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            }
            canvas.drawText(emoji, markerX, markerY + 10f, textPaint)
        }
    }
    
    /**
     * Draw marker for current time on the ring
     */
    private fun drawCurrentTimeMarker(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        ringThickness: Float,
        astronomyData: AstronomyDataEntity?
    ) {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        val angle = if (astronomyData != null && astronomyData.solarNoon > 0) {
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            calendar.timeInMillis = astronomyData.solarNoon
            val solarNoonHour = calendar.get(Calendar.HOUR_OF_DAY)
            val solarNoonMinute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, solarNoonHour)
            calendar.set(Calendar.MINUTE, solarNoonMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val normalizedSolarNoon = calendar.timeInMillis
            
            var timeDiff = currentTime - normalizedSolarNoon
            val dayInMillis = 24 * 60 * 60 * 1000L
            if (timeDiff > dayInMillis / 2) {
                timeDiff -= dayInMillis
            } else if (timeDiff < -dayInMillis / 2) {
                timeDiff += dayInMillis
            }
            
            val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
            val angleFromSolarNoon = hoursFromSolarNoon * 15f
            -90f + angleFromSolarNoon
        } else {
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val hourOffset = currentHour - 12
            val minuteOffset = currentMinute / 60f
            val totalHourOffset = hourOffset + minuteOffset
            totalHourOffset * 15f - 90f
        }
        
        val angleRad = Math.toRadians(angle.toDouble())
        val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
        val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()
        
        markerPaint.color = ContextCompat.getColor(context, R.color.accent_coral)
        canvas.drawCircle(markerX, markerY, 16f, markerPaint)
        
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.white)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(markerX, markerY, 16f, borderPaint)
    }
    
    /**
     * Draw date and time in the center
     */
    private fun drawCenterText(canvas: Canvas, centerX: Float, centerY: Float) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val dateText = dateFormat.format(calendar.time)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(calendar.time)
        
        val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
        
        val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val dateTextY = centerY - 20f - (dateTextPaint.descent() + dateTextPaint.ascent()) / 2
        canvas.drawText(dateText, centerX, dateTextY, dateTextPaint)
        
        val timeTextY = centerY + 40f - (timeTextPaint.descent() + timeTextPaint.ascent()) / 2
        canvas.drawText(timeText, centerX, timeTextY, timeTextPaint)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
}

