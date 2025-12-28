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
        
        // Determine which period this time falls into
        when {
            // Morning: sunrise -> solar noon
            time >= astronomyData.sunrise && time < solarNoon -> {
                val range = (solarNoon - astronomyData.sunrise).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.sunrise).toFloat() / range
                } else 0f
                return interpolateColor(SUNRISE_COLOR, SOLAR_NOON_COLOR, progress)
            }
            
            // Afternoon: solar noon -> sunset (exclude sunset itself, handled separately)
            time >= solarNoon && time < astronomyData.sunset -> {
                val range = (astronomyData.sunset - solarNoon).toFloat()
                val progress = if (range > 0) {
                    (time - solarNoon).toFloat() / range
                } else 0f
                return interpolateColor(SOLAR_NOON_COLOR, SUNRISE_COLOR, progress)
            }
            
            // Morning: civil dawn -> sunrise (symmetric to sunset -> civil dusk)
            time >= astronomyData.civilDawn && time <= astronomyData.sunrise -> {
                if (time == astronomyData.sunrise) {
                    return SUNRISE_COLOR
                }
                val morningRange = (astronomyData.sunrise - astronomyData.civilDawn).toFloat()
                val morningProgress = if (morningRange > 0) {
                    (time - astronomyData.civilDawn).toFloat() / morningRange
                } else 0f
                // Map to symmetric afternoon position: reverse the progress
                // At civil dawn (progress 0) -> maps to civil dusk (progress 1)
                // At sunrise (progress 1) -> maps to sunset (progress 0)
                val afternoonProgress = 1f - morningProgress
                // Use same color interpolation: SUNRISE_COLOR at sunset/civilDusk boundary, CIVIL_COLOR at civilDusk
                return interpolateColor(SUNRISE_COLOR, CIVIL_COLOR, afternoonProgress)
            }
            
            // Afternoon: sunset -> civil dusk (reference for morning symmetry)
            time >= astronomyData.sunset && time < astronomyData.civilDusk -> {
                if (time == astronomyData.sunset) {
                    return SUNRISE_COLOR
                }
                val range = (astronomyData.civilDusk - astronomyData.sunset).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.sunset).toFloat() / range
                } else 0f
                // SUNRISE_COLOR at sunset (progress 0), CIVIL_COLOR at civil dusk (progress 1)
                return interpolateColor(SUNRISE_COLOR, CIVIL_COLOR, progress)
            }
            
            // Morning: nautical dawn -> civil dawn (symmetric to civil dusk -> nautical dusk)
            time >= astronomyData.nauticalDawn && time < astronomyData.civilDawn -> {
                val morningRange = (astronomyData.civilDawn - astronomyData.nauticalDawn).toFloat()
                val morningProgress = if (morningRange > 0) {
                    (time - astronomyData.nauticalDawn).toFloat() / morningRange
                } else 0f
                val afternoonProgress = 1f - morningProgress
                return interpolateColor(CIVIL_COLOR, NAUTICAL_COLOR, afternoonProgress)
            }
            
            // Afternoon: civil dusk -> nautical dusk (reference for morning)
            time >= astronomyData.civilDusk && time < astronomyData.nauticalDusk -> {
                val range = (astronomyData.nauticalDusk - astronomyData.civilDusk).toFloat()
                val progress = if (range > 0) {
                    (time - astronomyData.civilDusk).toFloat() / range
                } else 0f
                return interpolateColor(CIVIL_COLOR, NAUTICAL_COLOR, progress)
            }
            
            // Morning: astro dawn -> nautical dawn (symmetric to nautical dusk -> astro dusk)
            time >= astronomyData.astroDawn && time < astronomyData.nauticalDawn -> {
                val morningRange = (astronomyData.nauticalDawn - astronomyData.astroDawn).toFloat()
                val morningProgress = if (morningRange > 0) {
                    (time - astronomyData.astroDawn).toFloat() / morningRange
                } else 0f
                val afternoonProgress = 1f - morningProgress
                return interpolateColor(NAUTICAL_COLOR, ASTRONOMICAL_COLOR, afternoonProgress)
            }
            
            // Afternoon: nautical dusk -> astro dusk (reference for morning)
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

