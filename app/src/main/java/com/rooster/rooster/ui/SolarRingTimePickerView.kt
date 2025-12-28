package com.rooster.rooster.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.widget.SolarColorCalculator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive solar ring time picker for selecting time in sun mode
 * Allows users to tap on the ring to select a time
 */
class SolarRingTimePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var astronomyData: AstronomyDataEntity? = null
    private var selectedTime: Long = 0
    private var onTimeSelectedListener: ((Long) -> Unit)? = null
    
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private val selectedMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        // Initialize selected time to current time
        selectedTime = System.currentTimeMillis()
    }
    
    /**
     * Set astronomy data for the ring
     */
    fun setAstronomyData(data: AstronomyDataEntity?) {
        astronomyData = data
        invalidate()
    }
    
    /**
     * Set selected time
     */
    fun setSelectedTime(time: Long) {
        selectedTime = time
        invalidate()
    }
    
    /**
     * Get selected time
     */
    fun getSelectedTime(): Long = selectedTime
    
    /**
     * Set listener for time selection
     */
    fun setOnTimeSelectedListener(listener: (Long) -> Unit) {
        onTimeSelectedListener = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = minOf(width, height)
        if (size <= 0) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Calculate radius
        val ringThickness = (size * 67f / 512f).coerceAtLeast(30f).coerceAtMost(100f)
        val edgePadding = 8f
        val radius = (size / 2f) - (ringThickness / 2f) - edgePadding
        
        // Draw background
        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))
        
        // Create and draw gradient ring (using same logic as SolarRingView)
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
        
        // Draw selected time marker
        drawSelectedTimeMarker(canvas, centerX, centerY, radius, ringThickness)
        
        // Draw selected time text in center
        drawSelectedTimeText(canvas, centerX, centerY)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val x = event.x
            val y = event.y
            
            val centerX = width / 2f
            val centerY = height / 2f
            
            val size = minOf(width, height)
            val ringThickness = (size * 67f / 512f).coerceAtLeast(30f).coerceAtMost(100f)
            val edgePadding = 8f
            val radius = (size / 2f) - (ringThickness / 2f) - edgePadding
            
            // Calculate distance from center
            val dx = x - centerX
            val dy = y - centerY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            
            // Check if touch is on or near the ring (within ring thickness + some margin)
            val margin = 40f
            if (distance >= radius - ringThickness / 2f - margin && 
                distance <= radius + ringThickness / 2f + margin) {
                
                // Calculate angle from center (0 degrees = 3 o'clock, -90 degrees = 12 o'clock)
                var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
                // Convert to -180 to 180 range, then adjust so -90 = 12 o'clock
                angle = angle - 90.0
                if (angle < -180) angle += 360
                
                // Convert angle to time
                val time = angleToTime(angle, astronomyData)
                selectedTime = time
                invalidate()
                
                onTimeSelectedListener?.invoke(selectedTime)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * Convert angle (in degrees, where -90 = 12 o'clock) to time in milliseconds
     */
    private fun angleToTime(angleDeg: Double, astronomyData: AstronomyDataEntity?): Long {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        if (astronomyData != null && astronomyData.solarNoon > 0) {
            // Use solar noon as reference
            calendar.timeInMillis = astronomyData.solarNoon
            val solarNoonHour = calendar.get(Calendar.HOUR_OF_DAY)
            val solarNoonMinute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, solarNoonHour)
            calendar.set(Calendar.MINUTE, solarNoonMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val normalizedSolarNoon = calendar.timeInMillis
            
            // Convert angle to hours from solar noon (each hour = 15 degrees)
            val hoursFromSolarNoon = angleDeg / 15.0
            
            // Calculate time in milliseconds
            val timeInMillis = normalizedSolarNoon + (hoursFromSolarNoon * 60 * 60 * 1000).toLong()
            
            // Normalize to today
            val dayInMillis = 24 * 60 * 60 * 1000L
            var normalizedTime = timeInMillis
            while (normalizedTime < todayStart) normalizedTime += dayInMillis
            while (normalizedTime >= todayStart + dayInMillis) normalizedTime -= dayInMillis
            
            return normalizedTime
        } else {
            // Fallback: use 12:00 as reference
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            val noon = calendar.timeInMillis
            
            val hoursFromNoon = angleDeg / 15.0
            val timeInMillis = noon + (hoursFromNoon * 60 * 60 * 1000).toLong()
            
            val dayInMillis = 24 * 60 * 60 * 1000L
            var normalizedTime = timeInMillis
            while (normalizedTime < todayStart) normalizedTime += dayInMillis
            while (normalizedTime >= todayStart + dayInMillis) normalizedTime -= dayInMillis
            
            return normalizedTime
        }
    }
    
    /**
     * Draw marker for selected time
     */
    private fun drawSelectedTimeMarker(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        ringThickness: Float
    ) {
        val calendar = Calendar.getInstance()
        val currentTime = selectedTime
        
        val angle = if (astronomyData != null && astronomyData!!.solarNoon > 0) {
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            calendar.timeInMillis = astronomyData!!.solarNoon
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
            val currentHour = calendar.apply { timeInMillis = currentTime }.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val hourOffset = currentHour - 12
            val minuteOffset = currentMinute / 60f
            val totalHourOffset = hourOffset + minuteOffset
            totalHourOffset * 15f - 90f
        }
        
        val angleRad = Math.toRadians(angle.toDouble())
        val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
        val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()
        
        // Draw larger, more prominent marker for selected time
        selectedMarkerPaint.color = ContextCompat.getColor(context, R.color.md_theme_dark_primary)
        canvas.drawCircle(markerX, markerY, 20f, selectedMarkerPaint)
        
        // Draw white border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.white)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(markerX, markerY, 20f, borderPaint)
    }
    
    /**
     * Draw selected time text in center
     */
    private fun drawSelectedTimeText(canvas: Canvas, centerX: Float, centerY: Float) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedTime
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeText = timeFormat.format(calendar.time)
        
        val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val timeTextY = centerY - (timeTextPaint.descent() + timeTextPaint.ascent()) / 2
        canvas.drawText(timeText, centerX, timeTextY, timeTextPaint)
    }
    
    /**
     * Create gradient color stops for the time picker
     * Uses the same colors as SolarRingView but simplified for performance
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
        val SUNRISE_COLOR = Color.parseColor("#FF8E53")
        val nightColor = Color.parseColor("#2A2A3A")
        
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
        
        val events = mutableListOf<Pair<Long, Int>>()
        
        if (normalizedData.astroDawn > 0) {
            events.add(Pair(normalizedData.astroDawn, SolarColorCalculator.getColorForTime(normalizedData.astroDawn, normalizedData)))
        }
        if (normalizedData.sunrise > 0) {
            events.add(Pair(normalizedData.sunrise, SUNRISE_COLOR))
        }
        if (normalizedData.solarNoon > 0) {
            events.add(Pair(normalizedData.solarNoon, SOLAR_NOON_BRIGHTEST))
        }
        if (normalizedData.sunset > 0) {
            events.add(Pair(normalizedData.sunset, SUNRISE_COLOR))
        }
        if (normalizedData.astroDusk > 0) {
            events.add(Pair(normalizedData.astroDusk, SolarColorCalculator.getColorForTime(normalizedData.astroDusk, normalizedData)))
        }
        events.add(Pair(todayStart, nightColor))
        
        val eventPositions = events.map { Pair(timeToPosition(it.first), it.second) }
            .sortedBy { it.first }
        
        val colors = eventPositions.map { it.second }
        val positions = eventPositions.map { it.first }
        
        if (colors.size < 2) {
            return Pair(listOf(nightColor, nightColor), listOf(0f, 1f))
        }
        
        return Pair(colors, positions)
    }
    
    /**
     * Simplified gradient creation for the time picker
     * Uses the same colors as SolarRingView but with fewer stops for performance
     */
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
}

