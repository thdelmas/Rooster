package com.rooster.rooster.widget

import android.content.Context
import android.graphics.*
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import com.rooster.rooster.data.local.entity.AlarmEntity
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles all rendering logic for the Solar Ring Widget.
 * Generates the ring bitmap with gradient, markers, and center text.
 */
object SolarRingRenderer {

    private const val REFERENCE_SIZE = 512f
    private const val MIN_SCALE = 0.5f
    private const val MAX_SCALE = 1.5f
    private const val BASE_RING_THICKNESS = 67f
    private const val BASE_EDGE_PADDING = 8f
    private const val POSITION_EPSILON = 0.0001f

    private val NIGHT_COLOR = Color.parseColor("#2A2A3A")
    private val SOLAR_NOON_BRIGHTEST = Color.parseColor("#FFE8B8")
    private val DAY_SKY_COLOR = Color.parseColor("#FFD89C")
    private val GOLDEN_HOUR_COLOR = Color.parseColor("#FFB86C")
    private val SUNRISE_COLOR = Color.parseColor("#FF8E53")

    fun generateRingBitmap(
        context: Context,
        astronomyData: AstronomyDataEntity?,
        size: Int,
        nextAlarm: AlarmEntity? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = size / 2f
        val centerY = size / 2f
        val scaleFactor = (size / REFERENCE_SIZE).coerceIn(MIN_SCALE, MAX_SCALE)

        val ringThickness = BASE_RING_THICKNESS * scaleFactor
        val edgePadding = BASE_EDGE_PADDING * scaleFactor
        val radius = (size / 2f) - (ringThickness / 2f) - edgePadding

        canvas.drawColor(ContextCompat.getColor(context, R.color.md_theme_dark_background))

        drawGradientRing(canvas, centerX, centerY, radius, ringThickness, astronomyData)

        if (astronomyData != null) {
            drawSolarEventMarkers(context, canvas, centerX, centerY, radius, astronomyData, scaleFactor)
        }

        drawCurrentTimeMarker(context, canvas, centerX, centerY, radius, astronomyData, scaleFactor)
        drawCenterText(context, canvas, centerX, centerY, scaleFactor, nextAlarm)

        return bitmap
    }

    private fun drawGradientRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        ringThickness: Float,
        astronomyData: AstronomyDataEntity?
    ) {
        val (colors, positions) = createGradientColorStops(astronomyData)

        val gradient = SweepGradient(
            centerX, centerY,
            colors.toIntArray(),
            positions.toFloatArray()
        )

        val matrix = Matrix()
        matrix.setRotate(-90f, centerX, centerY)
        gradient.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            style = Paint.Style.STROKE
            strokeWidth = ringThickness
            strokeCap = Paint.Cap.ROUND
        }

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        canvas.drawArc(rect, -90f, 360f, false, paint)
    }

    private fun createGradientColorStops(
        astronomyData: AstronomyDataEntity?
    ): Pair<List<Int>, List<Float>> {
        if (astronomyData == null) {
            return Pair(listOf(NIGHT_COLOR, NIGHT_COLOR), listOf(0f, 1f))
        }

        val todayStart = SolarRingDataProvider.getTodayStart()
        val normalizedData = SolarRingDataProvider.normalizeAstronomyData(astronomyData, todayStart)

        val events = mutableListOf<Pair<Long, Int>>()

        // Dawn events
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

        // Sunrise to solar noon gradient
        val nearNoonColor = if (normalizedData.solarNoon > 0) {
            interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.97f)
        } else {
            DAY_SKY_COLOR
        }

        if (normalizedData.sunrise > 0 && normalizedData.solarNoon > 0 && normalizedData.sunrise < normalizedData.solarNoon) {
            val range = normalizedData.solarNoon - normalizedData.sunrise
            events.add(Pair(normalizedData.sunrise + (range * 0.2f).toLong(), GOLDEN_HOUR_COLOR))
            events.add(Pair(normalizedData.sunrise + (range * 0.4f).toLong(), DAY_SKY_COLOR))
            events.add(Pair(normalizedData.sunrise + (range * 0.6f).toLong(), interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.3f)))
            events.add(Pair(normalizedData.sunrise + (range * 0.8f).toLong(), interpolateColor(DAY_SKY_COLOR, SOLAR_NOON_BRIGHTEST, 0.6f)))
            events.add(Pair(normalizedData.solarNoon - (range * 0.02f).toLong(), nearNoonColor))
        }

        // Solar noon
        if (normalizedData.solarNoon > 0) {
            events.add(Pair(normalizedData.solarNoon, SOLAR_NOON_BRIGHTEST))
        }

        // Solar noon to sunset gradient
        if (normalizedData.solarNoon > 0 && normalizedData.sunset > 0 && normalizedData.solarNoon < normalizedData.sunset) {
            val range = normalizedData.sunset - normalizedData.solarNoon
            events.add(Pair(normalizedData.solarNoon + (range * 0.02f).toLong(), nearNoonColor))
            events.add(Pair(normalizedData.solarNoon + (range * 0.2f).toLong(), interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.6f)))
            events.add(Pair(normalizedData.solarNoon + (range * 0.4f).toLong(), interpolateColor(SOLAR_NOON_BRIGHTEST, DAY_SKY_COLOR, 0.3f)))
            events.add(Pair(normalizedData.solarNoon + (range * 0.6f).toLong(), DAY_SKY_COLOR))
            events.add(Pair(normalizedData.solarNoon + (range * 0.8f).toLong(), GOLDEN_HOUR_COLOR))
        }

        // Dusk events
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

        // Midnight for full circle coverage
        events.add(Pair(todayStart, NIGHT_COLOR))

        return buildGradientArrays(events, normalizedData.solarNoon)
    }

    private fun buildGradientArrays(
        events: List<Pair<Long, Int>>,
        solarNoon: Long
    ): Pair<List<Int>, List<Float>> {
        val eventPositions = mutableListOf<Pair<Float, Int>>()
        var solarNoonColor: Int? = null

        for ((time, color) in events) {
            if (time == solarNoon && solarNoon > 0) {
                solarNoonColor = color
                eventPositions.add(Pair(0.0f, color))
            } else {
                eventPositions.add(Pair(SolarRingDataProvider.timeToPosition(time, solarNoon), color))
            }
        }

        val sortedEvents = eventPositions.sortedBy { it.first }

        val colors = mutableListOf<Int>()
        val positions = mutableListOf<Float>()
        var lastPosition = -1f

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

        // Ensure solar noon is at the start
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
            return Pair(listOf(NIGHT_COLOR, NIGHT_COLOR), listOf(0f, 1f))
        }

        return Pair(colors, positions)
    }

    private fun drawSolarEventMarkers(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        astronomyData: AstronomyDataEntity,
        scaleFactor: Float
    ) {
        val todayStart = SolarRingDataProvider.getTodayStart()
        val normalizedSolarNoon = SolarRingDataProvider.normalizeTime(astronomyData.solarNoon, todayStart)

        val events = listOf(
            Pair("\uD83C\uDF0C", SolarRingDataProvider.normalizeTime(astronomyData.astroDawn, todayStart)),
            Pair("\uD83C\uDF03", SolarRingDataProvider.normalizeTime(astronomyData.nauticalDawn, todayStart)),
            Pair("\uD83C\uDF06", SolarRingDataProvider.normalizeTime(astronomyData.civilDawn, todayStart)),
            Pair("\uD83C\uDF05", SolarRingDataProvider.normalizeTime(astronomyData.sunrise, todayStart)),
            Pair("\u2600\uFE0F", normalizedSolarNoon),
            Pair("\uD83C\uDF07", SolarRingDataProvider.normalizeTime(astronomyData.sunset, todayStart)),
            Pair("\uD83C\uDF06", SolarRingDataProvider.normalizeTime(astronomyData.civilDusk, todayStart)),
            Pair("\uD83C\uDF03", SolarRingDataProvider.normalizeTime(astronomyData.nauticalDusk, todayStart)),
            Pair("\uD83C\uDF0C", SolarRingDataProvider.normalizeTime(astronomyData.astroDusk, todayStart))
        )

        val baseMarkerRadius = 20f
        val baseEmojiSize = 32f
        val baseEmojiOffset = 10f

        for ((emoji, eventTime) in events) {
            if (eventTime <= 0) continue

            val angle = SolarRingDataProvider.timeToAngle(eventTime, normalizedSolarNoon)
            val angleRad = Math.toRadians(angle.toDouble())
            val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
            val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()

            val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.md_theme_dark_background)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(markerX, markerY, baseMarkerRadius * scaleFactor, markerPaint)

            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = baseEmojiSize * scaleFactor
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(emoji, markerX, markerY + (baseEmojiOffset * scaleFactor), emojiPaint)
        }
    }

    private fun drawCurrentTimeMarker(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        astronomyData: AstronomyDataEntity?,
        scaleFactor: Float
    ) {
        val currentTime = System.currentTimeMillis()
        val solarNoon = if (astronomyData != null && astronomyData.solarNoon > 0) {
            val todayStart = SolarRingDataProvider.getTodayStart()
            SolarRingDataProvider.normalizeTime(astronomyData.solarNoon, todayStart)
        } else {
            0L
        }

        val angle = SolarRingDataProvider.timeToAngle(currentTime, solarNoon)
        val angleRad = Math.toRadians(angle.toDouble())
        val markerX = centerX + (radius * Math.cos(angleRad)).toFloat()
        val markerY = centerY + (radius * Math.sin(angleRad)).toFloat()

        val baseCurrentMarkerRadius = 16f
        val markerRadius = baseCurrentMarkerRadius * scaleFactor

        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.accent_coral)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(markerX, markerY, markerRadius, markerPaint)

        val baseBorderWidth = 3f
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.white)
            style = Paint.Style.STROKE
            strokeWidth = baseBorderWidth * scaleFactor
        }
        canvas.drawCircle(markerX, markerY, markerRadius, borderPaint)
    }

    private fun drawCenterText(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        scaleFactor: Float,
        nextAlarm: AlarmEntity?
    ) {
        val calendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val dateText = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(calendar.time)

        // Date
        val baseDateTextSize = 48f
        val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = baseDateTextSize * scaleFactor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        val baseDateOffset = 20f
        val dateTextY = centerY - (baseDateOffset * scaleFactor) - (dateTextPaint.descent() + dateTextPaint.ascent()) / 2
        canvas.drawText(dateText, centerX, dateTextY, dateTextPaint)

        // Time
        val baseTimeTextSize = 72f
        val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground)
            textSize = baseTimeTextSize * scaleFactor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val baseTimeOffset = 40f
        val timeTextY = centerY + (baseTimeOffset * scaleFactor) - (timeTextPaint.descent() + timeTextPaint.ascent()) / 2
        canvas.drawText(timeText, centerX, timeTextY, timeTextPaint)

        // Next alarm
        if (nextAlarm != null && nextAlarm.calculatedTime > 0) {
            val alarmCalendar = Calendar.getInstance()
            alarmCalendar.timeInMillis = nextAlarm.calculatedTime
            val alarmTimeText = timeFormat.format(alarmCalendar.time)

            val alarmLabel = if (nextAlarm.label.isNotBlank()) nextAlarm.label else "Alarm"
            val alarmText = "Next: $alarmTimeText ($alarmLabel)"

            val baseAlarmTextSize = 32f
            val alarmTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.md_theme_dark_onBackground).let { c ->
                    Color.argb(
                        (Color.alpha(c) * 0.7f).toInt(),
                        Color.red(c),
                        Color.green(c),
                        Color.blue(c)
                    )
                }
                textSize = baseAlarmTextSize * scaleFactor
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }

            val baseAlarmOffset = 80f
            val alarmTextY = centerY + (baseAlarmOffset * scaleFactor) - (alarmTextPaint.descent() + alarmTextPaint.ascent()) / 2
            canvas.drawText(alarmText, centerX, alarmTextY, alarmTextPaint)
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val a = (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * f).toInt()
        val r = (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * f).toInt()
        val g = (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * f).toInt()
        val b = (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * f).toInt()
        return Color.argb(a, r, g, b)
    }
}
