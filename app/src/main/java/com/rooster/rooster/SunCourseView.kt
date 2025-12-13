package com.rooster.rooster

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class SunCourseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    private var sunrise: Long = 0
    private var sunset: Long = 0
    private var solarNoon: Long = 0
    private var civilDawn: Long = 0
    private var civilDusk: Long = 0
    private var nauticalDawn: Long = 0
    private var nauticalDusk: Long = 0
    private var astroDawn: Long = 0
    private var astroDusk: Long = 0
    private var markerTime: Long = 0
    private var markerLabel: String = ""
    
    // Interaction callback
    var onSolarEventSelected: ((String) -> Unit)? = null
    var interactive: Boolean = true
    
    private val sunColor = Color.parseColor("#FFB86C")
    private val skyColorDay = Color.parseColor("#64B5F6")
    private val skyColorNight = Color.parseColor("#1A2332")
    private val horizonColor = Color.parseColor("#51443A")
    
    init {
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
    }
    
    fun setSunTimes(
        civilDawn: Long,
        sunrise: Long,
        solarNoon: Long,
        sunset: Long,
        civilDusk: Long
    ) {
        this.civilDawn = civilDawn
        this.sunrise = sunrise
        this.solarNoon = solarNoon
        this.sunset = sunset
        this.civilDusk = civilDusk
        invalidate()
    }
    
    fun setAllSunTimes(
        astroDawn: Long,
        nauticalDawn: Long,
        civilDawn: Long,
        sunrise: Long,
        solarNoon: Long,
        sunset: Long,
        civilDusk: Long,
        nauticalDusk: Long,
        astroDusk: Long
    ) {
        this.astroDawn = astroDawn
        this.nauticalDawn = nauticalDawn
        this.civilDawn = civilDawn
        this.sunrise = sunrise
        this.solarNoon = solarNoon
        this.sunset = sunset
        this.civilDusk = civilDusk
        this.nauticalDusk = nauticalDusk
        this.astroDusk = astroDusk
        invalidate()
    }
    
    fun setMarker(time: Long, label: String) {
        this.markerTime = time
        this.markerLabel = label
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (sunrise == 0L || sunset == 0L) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height * 0.65f
        val arcHeight = height * 0.5f
        
        // Draw horizon line
        paint.color = horizonColor
        paint.strokeWidth = 2f
        canvas.drawLine(0f, centerY, width, centerY, paint)
        
        // Draw sun path arc
        paint.color = sunColor
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        
        path.reset()
        val startX = width * 0.1f
        val endX = width * 0.9f
        val arcWidth = endX - startX
        
        // Create arc path
        path.moveTo(startX, centerY)
        val cp1X = startX + arcWidth * 0.33f
        val cp1Y = centerY - arcHeight * 0.7f
        val cp2X = startX + arcWidth * 0.67f
        val cp2Y = centerY - arcHeight * 0.7f
        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, centerY)
        
        canvas.drawPath(path, paint)
        
        // Draw sun at solar noon
        if (solarNoon > 0) {
            val noonProgress = 0.5f // Middle of arc
            val noonPoint = getPointOnPath(noonProgress, startX, endX, arcWidth, centerY, arcHeight)
            
            paint.style = Paint.Style.FILL
            paint.color = sunColor
            canvas.drawCircle(noonPoint.x, noonPoint.y, 16f, paint)
            
            // Draw sun rays
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            for (i in 0 until 8) {
                val angle = Math.PI * 2 * i / 8
                val startRadius = 20f
                val endRadius = 28f
                val startRayX = noonPoint.x + (Math.cos(angle) * startRadius).toFloat()
                val startRayY = noonPoint.y + (Math.sin(angle) * startRadius).toFloat()
                val endRayX = noonPoint.x + (Math.cos(angle) * endRadius).toFloat()
                val endRayY = noonPoint.y + (Math.sin(angle) * endRadius).toFloat()
                canvas.drawLine(startRayX, startRayY, endRayX, endRayY, paint)
            }
        }
        
        // Draw all solar events if available
        if (astroDawn > 0) {
            drawSolarEventMarker(canvas, "🌄", "Astro Dawn", astroDawn, startX, endX, arcWidth, centerY, arcHeight, 0.02f)
        }
        if (nauticalDawn > 0) {
            drawSolarEventMarker(canvas, "🌅", "Nautical Dawn", nauticalDawn, startX, endX, arcWidth, centerY, arcHeight, 0.04f)
        }
        if (civilDawn > 0) {
            drawSolarEventMarker(canvas, "🌆", "Civil Dawn", civilDawn, startX, endX, arcWidth, centerY, arcHeight, 0.06f)
        }
        if (sunrise > 0) {
            drawSolarEventMarker(canvas, "🌅", "Sunrise", sunrise, startX, endX, arcWidth, centerY, arcHeight, 0.08f)
        }
        if (solarNoon > 0) {
            drawSolarEventMarker(canvas, "☀️", "Solar Noon", solarNoon, startX, endX, arcWidth, centerY, arcHeight, 0.5f)
        }
        if (sunset > 0) {
            drawSolarEventMarker(canvas, "🌇", "Sunset", sunset, startX, endX, arcWidth, centerY, arcHeight, 0.92f)
        }
        if (civilDusk > 0) {
            drawSolarEventMarker(canvas, "🌆", "Civil Dusk", civilDusk, startX, endX, arcWidth, centerY, arcHeight, 0.94f)
        }
        if (nauticalDusk > 0) {
            drawSolarEventMarker(canvas, "🌃", "Nautical Dusk", nauticalDusk, startX, endX, arcWidth, centerY, arcHeight, 0.96f)
        }
        if (astroDusk > 0) {
            drawSolarEventMarker(canvas, "🌌", "Astro Dusk", astroDusk, startX, endX, arcWidth, centerY, arcHeight, 0.98f)
        }
        
        // Draw custom marker if set (alarm time)
        if (markerTime > 0 && markerLabel.isNotEmpty()) {
            val progress = calculateProgress(markerTime)
            drawCustomMarker(canvas, markerLabel, progress, startX, endX, arcWidth, centerY, arcHeight)
        }
        
        // Draw time labels
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#D5C3B5")
        
        if (sunrise > 0) {
            val sunriseTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunrise))
            canvas.drawText(sunriseTime, startX, centerY + 40f, textPaint)
        }
        
        if (sunset > 0) {
            val sunsetTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sunset))
            canvas.drawText(sunsetTime, endX, centerY + 40f, textPaint)
        }
    }
    
    private fun drawTimeMarker(
        canvas: Canvas,
        emoji: String,
        progress: Float,
        startX: Float,
        endX: Float,
        arcWidth: Float,
        centerY: Float,
        arcHeight: Float
    ) {
        val point = getPointOnPath(progress, startX, endX, arcWidth, centerY, arcHeight)
        textPaint.textSize = 28f
        canvas.drawText(emoji, point.x, point.y - 20f, textPaint)
    }
    
    private fun drawSolarEventMarker(
        canvas: Canvas,
        emoji: String,
        label: String,
        time: Long,
        startX: Float,
        endX: Float,
        arcWidth: Float,
        centerY: Float,
        arcHeight: Float,
        defaultProgress: Float
    ) {
        val progress = if (time > 0) calculateProgress(time) else defaultProgress
        val point = getPointOnPath(progress, startX, endX, arcWidth, centerY, arcHeight)
        
        // Draw touchable circle for interaction
        if (interactive) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#40FFB86C")
            canvas.drawCircle(point.x, point.y, 30f, paint)
        }
        
        // Draw emoji
        textPaint.textSize = 24f
        textPaint.color = Color.WHITE
        canvas.drawText(emoji, point.x, point.y - 15f, textPaint)
        
        // Draw label below
        textPaint.textSize = 18f
        textPaint.color = Color.parseColor("#D5C3B5")
        val labelY = if (progress < 0.5f) point.y + 35f else point.y + 25f
        canvas.drawText(label, point.x, labelY, textPaint)
    }
    
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (!interactive || onSolarEventSelected == null) return false
        
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                
                // Check which solar event was tapped
                val tappedEvent = findTappedSolarEvent(x, y)
                tappedEvent?.let { event ->
                    onSolarEventSelected?.invoke(event)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun findTappedSolarEvent(x: Float, y: Float): String? {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height * 0.65f
        val arcHeight = height * 0.5f
        val startX = width * 0.1f
        val endX = width * 0.9f
        val arcWidth = endX - startX
        
        val events = listOf(
            Triple("Astronomical Dawn", astroDawn, 0.02f),
            Triple("Nautical Dawn", nauticalDawn, 0.04f),
            Triple("Civil Dawn", civilDawn, 0.06f),
            Triple("Sunrise", sunrise, 0.08f),
            Triple("Solar Noon", solarNoon, 0.5f),
            Triple("Sunset", sunset, 0.92f),
            Triple("Civil Dusk", civilDusk, 0.94f),
            Triple("Nautical Dusk", nauticalDusk, 0.96f),
            Triple("Astronomical Dusk", astroDusk, 0.98f)
        )
        
        for ((eventName, time, defaultProgress) in events) {
            if (time <= 0) continue
            val progress = if (time > 0) calculateProgress(time) else defaultProgress
            val point = getPointOnPath(progress, startX, endX, arcWidth, centerY, arcHeight)
            
            val distance = Math.sqrt(
                Math.pow((x - point.x).toDouble(), 2.0) + 
                Math.pow((y - point.y).toDouble(), 2.0)
            )
            
            if (distance < 40) {
                return eventName
            }
        }
        
        return null
    }
    
    private fun drawCustomMarker(
        canvas: Canvas,
        label: String,
        progress: Float,
        startX: Float,
        endX: Float,
        arcWidth: Float,
        centerY: Float,
        arcHeight: Float
    ) {
        val point = getPointOnPath(progress, startX, endX, arcWidth, centerY, arcHeight)
        
        // Draw marker circle
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF8A65")
        canvas.drawCircle(point.x, point.y, 12f, paint)
        
        // Draw marker label
        textPaint.textSize = 20f
        textPaint.color = Color.WHITE
        canvas.drawText(label, point.x, point.y - 30f, textPaint)
    }
    
    private fun getPointOnPath(
        progress: Float,
        startX: Float,
        endX: Float,
        arcWidth: Float,
        centerY: Float,
        arcHeight: Float
    ): PointF {
        val t = progress
        val x = startX + arcWidth * t
        
        // Cubic bezier calculation for y
        val cp1X = startX + arcWidth * 0.33f
        val cp1Y = centerY - arcHeight * 0.7f
        val cp2X = startX + arcWidth * 0.67f
        val cp2Y = centerY - arcHeight * 0.7f
        
        val y = Math.pow(1 - t.toDouble(), 3.0).toFloat() * centerY +
                3 * Math.pow(1 - t.toDouble(), 2.0).toFloat() * t * cp1Y +
                3 * (1 - t) * t * t * cp2Y +
                t * t * t * centerY
        
        return PointF(x, y)
    }
    
    private fun calculateProgress(time: Long): Float {
        if (sunrise == 0L || sunset == 0L) return 0.5f
        
        return when {
            time < sunrise -> 0.1f
            time > sunset -> 0.9f
            else -> {
                val totalDaylight = (sunset - sunrise).toFloat()
                val elapsed = (time - sunrise).toFloat()
                0.1f + (elapsed / totalDaylight) * 0.8f
            }
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 250
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(desiredHeight, heightSize)
            else -> desiredHeight
        }
        
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            height
        )
    }
}


