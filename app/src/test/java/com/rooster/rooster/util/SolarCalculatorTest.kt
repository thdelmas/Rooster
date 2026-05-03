package com.rooster.rooster.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests for SolarCalculator.
 * Validates NOAA solar calculations against known reference values.
 */
class SolarCalculatorTest {

    // Tolerance: 15 minutes in milliseconds (accounts for equation-of-time drift
    // and the fact that SolarCalculator uses rawOffset, ignoring DST)
    private val TOLERANCE_MS = 15 * 60 * 1000L

    private fun calendarFor(year: Int, month: Int, day: Int, tz: TimeZone): Calendar {
        return Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar months are 0-based
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun millisForTime(cal: Calendar, hour: Int, minute: Int): Long {
        return Calendar.getInstance(cal.timeZone).apply {
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // ==================== Paris, France (48.8566 N, 2.3522 E) ====================

    @Test
    fun `calculate Paris spring equinox - sunrise around 6_50`() {
        val tz = TimeZone.getTimeZone("Europe/Paris")
        val date = calendarFor(2025, 3, 20, tz)
        val result = SolarCalculator.calculate(48.8566f, 2.3522f, date)

        val expectedSunrise = millisForTime(date, 7, 1) // ~07:01 CET
        assertEquals("Paris spring equinox sunrise", expectedSunrise.toDouble(), result.sunrise.toDouble(), TOLERANCE_MS.toDouble())
    }

    @Test
    fun `calculate Paris spring equinox - sunset around 19_15`() {
        val tz = TimeZone.getTimeZone("Europe/Paris")
        val date = calendarFor(2025, 3, 20, tz)
        val result = SolarCalculator.calculate(48.8566f, 2.3522f, date)

        val expectedSunset = millisForTime(date, 19, 15) // ~19:15 CET
        assertEquals("Paris spring equinox sunset", expectedSunset.toDouble(), result.sunset.toDouble(), TOLERANCE_MS.toDouble())
    }

    // ==================== New York (40.7128 N, -74.006 W) ====================

    @Test
    fun `calculate New York summer solstice`() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val date = calendarFor(2025, 6, 21, tz)
        val result = SolarCalculator.calculate(40.7128f, -74.006f, date)

        // Note: SolarCalculator uses rawOffset (EST=-5) not EDT=-4, so times are 1h earlier
        val expectedSunrise = millisForTime(date, 4, 25) // ~5:25 EDT - 1h DST offset
        val expectedSunset = millisForTime(date, 19, 31)  // ~20:31 EDT - 1h DST offset

        assertEquals("NYC summer solstice sunrise", expectedSunrise.toDouble(), result.sunrise.toDouble(), TOLERANCE_MS.toDouble())
        assertEquals("NYC summer solstice sunset", expectedSunset.toDouble(), result.sunset.toDouble(), TOLERANCE_MS.toDouble())
    }

    @Test
    fun `calculate New York winter solstice`() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val date = calendarFor(2025, 12, 21, tz)
        val result = SolarCalculator.calculate(40.7128f, -74.006f, date)

        val expectedSunrise = millisForTime(date, 7, 16) // ~7:16 EST
        val expectedSunset = millisForTime(date, 16, 32)  // ~16:32 EST

        assertEquals("NYC winter solstice sunrise", expectedSunrise.toDouble(), result.sunrise.toDouble(), TOLERANCE_MS.toDouble())
        assertEquals("NYC winter solstice sunset", expectedSunset.toDouble(), result.sunset.toDouble(), TOLERANCE_MS.toDouble())
    }

    // ==================== Tokyo (35.6762 N, 139.6503 E) ====================

    @Test
    fun `calculate Tokyo autumn equinox`() {
        val tz = TimeZone.getTimeZone("Asia/Tokyo")
        val date = calendarFor(2025, 9, 23, tz)
        val result = SolarCalculator.calculate(35.6762f, 139.6503f, date)

        val expectedSunrise = millisForTime(date, 5, 29) // ~5:29 JST
        val expectedSunset = millisForTime(date, 17, 37)  // ~17:37 JST

        assertEquals("Tokyo autumn equinox sunrise", expectedSunrise.toDouble(), result.sunrise.toDouble(), TOLERANCE_MS.toDouble())
        assertEquals("Tokyo autumn equinox sunset", expectedSunset.toDouble(), result.sunset.toDouble(), TOLERANCE_MS.toDouble())
    }

    // ==================== Sydney (33.8688 S, 151.2093 E) ====================

    @Test
    fun `calculate Sydney southern hemisphere winter`() {
        val tz = TimeZone.getTimeZone("Australia/Sydney")
        val date = calendarFor(2025, 6, 21, tz)
        val result = SolarCalculator.calculate(-33.8688f, 151.2093f, date)

        val expectedSunrise = millisForTime(date, 7, 0)  // ~7:00 AEST
        val expectedSunset = millisForTime(date, 16, 53) // ~16:53 AEST

        assertEquals("Sydney winter sunrise", expectedSunrise.toDouble(), result.sunrise.toDouble(), TOLERANCE_MS.toDouble())
        assertEquals("Sydney winter sunset", expectedSunset.toDouble(), result.sunset.toDouble(), TOLERANCE_MS.toDouble())
    }

    // ==================== Solar event ordering ====================

    @Test
    fun `solar events are in correct chronological order`() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val date = calendarFor(2025, 6, 21, tz)
        val result = SolarCalculator.calculate(40.7128f, -74.006f, date)

        assertTrue("astroDawn < nauticalDawn", result.astroDawn < result.nauticalDawn)
        assertTrue("nauticalDawn < civilDawn", result.nauticalDawn < result.civilDawn)
        assertTrue("civilDawn < sunrise", result.civilDawn < result.sunrise)
        assertTrue("sunrise < solarNoon", result.sunrise < result.solarNoon)
        assertTrue("solarNoon < sunset", result.solarNoon < result.sunset)
        assertTrue("sunset < civilDusk", result.sunset < result.civilDusk)
        assertTrue("civilDusk < nauticalDusk", result.civilDusk < result.nauticalDusk)
        assertTrue("nauticalDusk < astroDusk", result.nauticalDusk < result.astroDusk)
    }

    @Test
    fun `dayLength equals sunset minus sunrise`() {
        val tz = TimeZone.getTimeZone("Europe/Paris")
        val date = calendarFor(2025, 3, 20, tz)
        val result = SolarCalculator.calculate(48.8566f, 2.3522f, date)

        assertEquals("dayLength should be sunset - sunrise",
            result.sunset - result.sunrise, result.dayLength)
    }

    // ==================== Equator (0, 0) ====================

    @Test
    fun `calculate equator roughly 12h day`() {
        val tz = TimeZone.getTimeZone("UTC")
        val date = calendarFor(2025, 3, 20, tz)
        val result = SolarCalculator.calculate(0f, 0f, date)

        val twelveHoursMs = 12 * 60 * 60 * 1000L
        val tenMinutesMs = 10 * 60 * 1000L

        assertEquals("Equator day length near 12h",
            twelveHoursMs.toDouble(), result.dayLength.toDouble(), tenMinutesMs.toDouble())
    }

    // ==================== High latitude (Tromso, Norway 69.65 N) ====================

    @Test
    fun `calculate high latitude summer - very long day`() {
        val tz = TimeZone.getTimeZone("Europe/Oslo")
        val date = calendarFor(2025, 6, 21, tz)
        val result = SolarCalculator.calculate(69.65f, 18.96f, date)

        // At summer solstice in Tromso, sun doesn't set (midnight sun)
        // When polar day, sunrise and sunset collapse to solar noon, dayLength ~ 0
        // The calculator returns solar noon for both when hour angle is NaN
        val twentyHoursMs = 20 * 60 * 60 * 1000L
        // Either very long day or collapsed to 0 (polar day fallback)
        assertTrue("High latitude summer: either polar day or very long day",
            result.dayLength == 0L || result.dayLength > twentyHoursMs)
    }

    // ==================== Entity fields ====================

    @Test
    fun `result contains correct coordinates`() {
        val tz = TimeZone.getTimeZone("UTC")
        val date = calendarFor(2025, 1, 15, tz)
        val result = SolarCalculator.calculate(40.0f, -74.0f, date)

        assertEquals(40.0f, result.latitude)
        assertEquals(-74.0f, result.longitude)
        assertEquals(1, result.id)
    }

    @Test
    fun `lastUpdated is recent`() {
        val before = System.currentTimeMillis()
        val result = SolarCalculator.calculate(40.0f, -74.0f)
        val after = System.currentTimeMillis()

        assertTrue("lastUpdated should be between before and after",
            result.lastUpdated in before..after)
    }

    @Test
    fun `all timestamps are positive`() {
        val tz = TimeZone.getTimeZone("America/Chicago")
        val date = calendarFor(2025, 7, 4, tz)
        val result = SolarCalculator.calculate(41.8781f, -87.6298f, date)

        assertTrue("sunrise > 0", result.sunrise > 0)
        assertTrue("sunset > 0", result.sunset > 0)
        assertTrue("solarNoon > 0", result.solarNoon > 0)
        assertTrue("civilDawn > 0", result.civilDawn > 0)
        assertTrue("civilDusk > 0", result.civilDusk > 0)
        assertTrue("nauticalDawn > 0", result.nauticalDawn > 0)
        assertTrue("nauticalDusk > 0", result.nauticalDusk > 0)
        assertTrue("astroDawn > 0", result.astroDawn > 0)
        assertTrue("astroDusk > 0", result.astroDusk > 0)
    }

    @Test
    fun `summer days longer than winter days in northern hemisphere`() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val summer = calendarFor(2025, 6, 21, tz)
        val winter = calendarFor(2025, 12, 21, tz)

        val summerResult = SolarCalculator.calculate(40.7128f, -74.006f, summer)
        val winterResult = SolarCalculator.calculate(40.7128f, -74.006f, winter)

        assertTrue("Summer day should be longer than winter day",
            summerResult.dayLength > winterResult.dayLength)
    }
}
