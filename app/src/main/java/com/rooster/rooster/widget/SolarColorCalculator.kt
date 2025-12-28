package com.rooster.rooster.widget

import android.graphics.Color
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import java.util.*

/**
 * Utility class to calculate colors for each hour of the day based on solar events
 */
object SolarColorCalculator {
    
    // Solar event colors - transitioning through the day
    // Night colors made lighter to be visible against black background
    private val NIGHT_COLOR = Color.parseColor("#2A2A3A")        // Deep space (lighter for visibility)
    private val ASTRONOMICAL_COLOR = Color.parseColor("#3A3A4E")  // Astronomical twilight (lighter)
    private val NAUTICAL_COLOR = Color.parseColor("#4A4A6E")     // Nautical twilight (lighter)
    private val CIVIL_COLOR = Color.parseColor("#6A6A9E")        // Civil twilight (lighter)
    private val SUNRISE_COLOR = Color.parseColor("#FF8E53")      // Sunrise
    private val GOLDEN_HOUR_COLOR = Color.parseColor("#FFB86C")  // Golden hour
    private val DAY_SKY_COLOR = Color.parseColor("#FFD89C")      // Day sky
    private val SOLAR_NOON_COLOR = Color.parseColor("#FFD89C")   // Solar noon (brightest)
    
    /**
     * Get color for a specific hour of the day (0-23) based on solar events
     * Uses the midpoint of the hour for more accurate color representation
     * Solar noon is at the top (12 o'clock position), so hour 12 maps to top
     */
    fun getColorForHour(hour: Int, astronomyData: AstronomyDataEntity?): Int {
        if (astronomyData == null) {
            return NIGHT_COLOR
        }
        
        val calendar = Calendar.getInstance()
        val currentDayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Use the midpoint of the hour (30 minutes) for more accurate color
        val hourTime = currentDayStart + (hour * 60 * 60 * 1000L) + (30 * 60 * 1000L)
        
        // Normalize astronomy data times to today
        val normalizedData = normalizeAstronomyDataToToday(astronomyData, currentDayStart)
        
        // Calculate which solar period this hour falls into
        return getColorForTime(hourTime, normalizedData)
    }
    
    /**
     * Normalize astronomy data times to today's date
     * Extracts the time component (hour, minute, second) and applies it to today
     */
    private fun normalizeAstronomyDataToToday(data: AstronomyDataEntity, todayStart: Long): AstronomyDataEntity {
        val calendar = Calendar.getInstance()
        
        // Helper to get time of day component and apply to today
        fun normalizeTime(originalTime: Long): Long {
            calendar.timeInMillis = originalTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, second)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        
        return data.copy(
            sunrise = normalizeTime(data.sunrise),
            sunset = normalizeTime(data.sunset),
            solarNoon = normalizeTime(data.solarNoon),
            civilDawn = normalizeTime(data.civilDawn),
            civilDusk = normalizeTime(data.civilDusk),
            nauticalDawn = normalizeTime(data.nauticalDawn),
            nauticalDusk = normalizeTime(data.nauticalDusk),
            astroDawn = normalizeTime(data.astroDawn),
            astroDusk = normalizeTime(data.astroDusk)
        )
    }
    
    /**
     * Get color for a specific time based on solar events
     * Uses symmetric color calculation for dawn/dusk pairs to ensure matching colors
     */
    fun getColorForTime(time: Long, astronomyData: AstronomyDataEntity): Int {
        // Before astronomical dawn or after astronomical dusk = night
        if (time < astronomyData.astroDawn || time >= astronomyData.astroDusk) {
            return NIGHT_COLOR
        }
        
        val solarNoon = astronomyData.solarNoon
        
        // Determine which period this time falls into and map to symmetric segment
        when {
            // Morning periods - map to corresponding afternoon periods for symmetry
            time >= astronomyData.astroDawn && time < astronomyData.nauticalDawn -> {
                // Map astro dawn -> nautical dawn to nautical dusk -> astro dusk (reversed)
                return getSymmetricColor(
                    time, astronomyData.astroDawn, astronomyData.nauticalDawn,
                    astronomyData.nauticalDusk, astronomyData.astroDusk,
                    ASTRONOMICAL_COLOR, NAUTICAL_COLOR, true
                )
            }
            
            time >= astronomyData.nauticalDawn && time < astronomyData.civilDawn -> {
                // Map nautical dawn -> civil dawn to civil dusk -> nautical dusk (reversed)
                return getSymmetricColor(
                    time, astronomyData.nauticalDawn, astronomyData.civilDawn,
                    astronomyData.civilDusk, astronomyData.nauticalDusk,
                    NAUTICAL_COLOR, CIVIL_COLOR, true
                )
            }
            
            time >= astronomyData.civilDawn && time < astronomyData.sunrise -> {
                // Map civil dawn -> sunrise to sunset -> civil dusk (reversed)
                return getSymmetricColor(
                    time, astronomyData.civilDawn, astronomyData.sunrise,
                    astronomyData.sunset, astronomyData.civilDusk,
                    CIVIL_COLOR, SUNRISE_COLOR, true
                )
            }
            
            time >= astronomyData.sunrise && time < solarNoon -> {
                // Map sunrise -> solar noon to solar noon -> sunset
                return getSymmetricColor(
                    time, astronomyData.sunrise, solarNoon,
                    solarNoon, astronomyData.sunset,
                    SUNRISE_COLOR, SOLAR_NOON_COLOR, false
                )
            }
            
            // Afternoon periods - use directly
            time >= solarNoon && time < astronomyData.sunset -> {
                val range = (astronomyData.sunset - solarNoon).toFloat()
                val progress = if (range > 0) {
                    (time - solarNoon).toFloat() / range
                } else 0f
                return interpolateColor(SOLAR_NOON_COLOR, SUNRISE_COLOR, progress)
            }
            
            time >= astronomyData.sunset && time < astronomyData.civilDusk -> {
                val range = (astronomyData.civilDusk - astronomyData.sunset).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.sunset).toFloat() / range
                } else 0f
                return interpolateColor(SUNRISE_COLOR, CIVIL_COLOR, progress)
            }
            
            time >= astronomyData.civilDusk && time < astronomyData.nauticalDusk -> {
                val range = (astronomyData.nauticalDusk - astronomyData.civilDusk).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.civilDusk).toFloat() / range
                } else 0f
                return interpolateColor(CIVIL_COLOR, NAUTICAL_COLOR, progress)
            }
            
            time >= astronomyData.nauticalDusk && time < astronomyData.astroDusk -> {
                val range = (astronomyData.astroDusk - astronomyData.nauticalDusk).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.nauticalDusk).toFloat() / range
                } else 0f
                return interpolateColor(NAUTICAL_COLOR, ASTRONOMICAL_COLOR, progress)
            }
            
            else -> return SOLAR_NOON_COLOR
        }
    }
    
    /**
     * Get symmetric color by mapping morning segment to corresponding afternoon segment
     */
    private fun getSymmetricColor(
        time: Long,
        morningStart: Long, morningEnd: Long,
        afternoonStart: Long, afternoonEnd: Long,
        startColor: Int, endColor: Int,
        reversed: Boolean
    ): Int {
        // Calculate progress in morning segment
        val morningRange = (morningEnd - morningStart).toFloat()
        val progress = if (morningRange > 0) {
            (time - morningStart).toFloat() / morningRange
        } else {
            0f
        }
        
        // Apply same progress to afternoon segment (reversed if needed)
        val symmetricProgress = if (reversed) 1f - progress else progress
        
        // Interpolate using the same color transition
        return interpolateColor(startColor, endColor, symmetricProgress)
    }
    
    /**
     * Interpolate between two colors
     */
    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
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
    
    /**
     * Get colors for all 24 hours
     */
    fun getColorsFor24Hours(astronomyData: AstronomyDataEntity?): List<Int> {
        return (0..23).map { hour -> getColorForHour(hour, astronomyData) }
    }
}

