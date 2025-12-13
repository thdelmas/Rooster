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
    private var markerTime: Long = 0
    private var markerLabel: String = ""
    
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
        
        // Draw sunrise marker
        drawTimeMarker(canvas, "🌅", 0.05f, startX, endX, arcWidth, centerY, arcHeight)
        
        // Draw sunset marker
        drawTimeMarker(canvas, "🌇", 0.95f, startX, endX, arcWidth, centerY, arcHeight)
        
        // Draw custom marker if set
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


