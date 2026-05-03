package com.rooster.rooster.util

import android.icu.util.Calendar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TimeUtils
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TimeUtilsTest {

    // ==================== timeToMillis ====================

    @Test
    fun `timeToMillis zero hours and minutes returns 0`() {
        assertEquals(0L, TimeUtils.timeToMillis(0, 0))
    }

    @Test
    fun `timeToMillis 1 hour returns 3600000`() {
        assertEquals(AppConstants.MILLIS_PER_HOUR, TimeUtils.timeToMillis(1, 0))
    }

    @Test
    fun `timeToMillis 1 minute returns 60000`() {
        assertEquals(AppConstants.MILLIS_PER_MINUTE, TimeUtils.timeToMillis(0, 1))
    }

    @Test
    fun `timeToMillis 2 hours 30 minutes`() {
        val expected = 2 * AppConstants.MILLIS_PER_HOUR + 30 * AppConstants.MILLIS_PER_MINUTE
        assertEquals(expected, TimeUtils.timeToMillis(2, 30))
    }

    // ==================== formatTimeDifference ====================

    @Test
    fun `formatTimeDifference negative returns in the past`() {
        assertEquals("in the past", TimeUtils.formatTimeDifference(100, 50))
    }

    @Test
    fun `formatTimeDifference less than a minute`() {
        assertEquals("less than a minute", TimeUtils.formatTimeDifference(0, 30_000))
    }

    @Test
    fun `formatTimeDifference 1 minute`() {
        assertEquals("1 minute", TimeUtils.formatTimeDifference(0, 60_000))
    }

    @Test
    fun `formatTimeDifference 5 minutes`() {
        assertEquals("5 minutes", TimeUtils.formatTimeDifference(0, 5 * 60_000))
    }

    @Test
    fun `formatTimeDifference 1 hour`() {
        assertEquals("1 hour", TimeUtils.formatTimeDifference(0, 3_600_000))
    }

    @Test
    fun `formatTimeDifference 2 hours 15 minutes`() {
        val diff = 2 * 3_600_000L + 15 * 60_000L
        assertEquals("2 hours, 15 minutes", TimeUtils.formatTimeDifference(0, diff))
    }

    @Test
    fun `formatTimeDifference 1 day`() {
        assertEquals("1 day", TimeUtils.formatTimeDifference(0, 24 * 3_600_000L))
    }

    @Test
    fun `formatTimeDifference 2 days 3 hours`() {
        val diff = 2 * 24 * 3_600_000L + 3 * 3_600_000L
        assertEquals("2 days, 3 hours", TimeUtils.formatTimeDifference(0, diff))
    }

    // ==================== getDayName ====================

    @Test
    fun `getDayName returns correct names`() {
        assertEquals("Sunday", TimeUtils.getDayName(Calendar.SUNDAY))
        assertEquals("Monday", TimeUtils.getDayName(Calendar.MONDAY))
        assertEquals("Tuesday", TimeUtils.getDayName(Calendar.TUESDAY))
        assertEquals("Wednesday", TimeUtils.getDayName(Calendar.WEDNESDAY))
        assertEquals("Thursday", TimeUtils.getDayName(Calendar.THURSDAY))
        assertEquals("Friday", TimeUtils.getDayName(Calendar.FRIDAY))
        assertEquals("Saturday", TimeUtils.getDayName(Calendar.SATURDAY))
    }

    @Test
    fun `getDayName invalid returns Unknown`() {
        assertEquals("Unknown", TimeUtils.getDayName(99))
    }

    // ==================== getShortDayName ====================

    @Test
    fun `getShortDayName returns correct abbreviations`() {
        assertEquals("Sun", TimeUtils.getShortDayName(Calendar.SUNDAY))
        assertEquals("Mon", TimeUtils.getShortDayName(Calendar.MONDAY))
        assertEquals("Sat", TimeUtils.getShortDayName(Calendar.SATURDAY))
    }

    @Test
    fun `getShortDayName invalid returns question mark`() {
        assertEquals("?", TimeUtils.getShortDayName(99))
    }

    // ==================== isSameDay ====================

    @Test
    fun `isSameDay same timestamp returns true`() {
        val now = System.currentTimeMillis()
        assertTrue(TimeUtils.isSameDay(now, now))
    }

    @Test
    fun `isSameDay different times same day returns true`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        val morning = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 20)
        val evening = cal.timeInMillis
        assertTrue(TimeUtils.isSameDay(morning, evening))
    }

    @Test
    fun `isSameDay different days returns false`() {
        val cal = Calendar.getInstance()
        val today = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = cal.timeInMillis
        assertFalse(TimeUtils.isSameDay(today, tomorrow))
    }

    // ==================== isToday ====================

    @Test
    fun `isToday with current time returns true`() {
        assertTrue(TimeUtils.isToday(System.currentTimeMillis()))
    }

    @Test
    fun `isToday with yesterday returns false`() {
        val yesterday = System.currentTimeMillis() - AppConstants.MILLIS_PER_DAY
        assertFalse(TimeUtils.isToday(yesterday))
    }

    // ==================== addDays ====================

    @Test
    fun `addDays adds correct number of days`() {
        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.JANUARY, 1, 12, 0, 0)
        val base = cal.timeInMillis

        val result = TimeUtils.addDays(base, 5)
        val resultCal = Calendar.getInstance().apply { timeInMillis = result }

        assertEquals(6, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `addDays with negative subtracts days`() {
        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.JANUARY, 10, 12, 0, 0)
        val base = cal.timeInMillis

        val result = TimeUtils.addDays(base, -3)
        val resultCal = Calendar.getInstance().apply { timeInMillis = result }

        assertEquals(7, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    // ==================== addHours / addMinutes ====================

    @Test
    fun `addHours adds correct milliseconds`() {
        val base = 0L
        assertEquals(2 * AppConstants.MILLIS_PER_HOUR, TimeUtils.addHours(base, 2))
    }

    @Test
    fun `addMinutes adds correct milliseconds`() {
        val base = 0L
        assertEquals(30 * AppConstants.MILLIS_PER_MINUTE, TimeUtils.addMinutes(base, 30))
    }

    // ==================== formatMinutesAsHours ====================

    @Test
    fun `formatMinutesAsHours less than 60 shows minutes`() {
        assertEquals("30m", TimeUtils.formatMinutesAsHours(30))
        assertEquals("0m", TimeUtils.formatMinutesAsHours(0))
        assertEquals("59m", TimeUtils.formatMinutesAsHours(59))
    }

    @Test
    fun `formatMinutesAsHours 60 or more shows hours`() {
        assertEquals("1h", TimeUtils.formatMinutesAsHours(60))
        assertEquals("2h", TimeUtils.formatMinutesAsHours(120))
        assertEquals("8h", TimeUtils.formatMinutesAsHours(480))
    }

    // ==================== todayAt / tomorrowAt ====================

    @Test
    fun `todayAt returns correct hour and minute`() {
        val result = TimeUtils.todayAt(14, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = result }

        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }

    @Test
    fun `tomorrowAt is after todayAt for same time`() {
        val today = TimeUtils.todayAt(6, 0)
        val tomorrow = TimeUtils.tomorrowAt(6, 0)

        assertTrue("tomorrowAt should be after todayAt", tomorrow > today)
        val diff = tomorrow - today
        val oneDayMs = 24 * 60 * 60 * 1000L
        // Should be approximately 1 day apart (within 1 hour for DST)
        assertTrue("Difference should be ~24h", diff in (oneDayMs - 3_600_000L)..(oneDayMs + 3_600_000L))
    }

    // ==================== startOfToday / endOfToday / startOfTomorrow ====================

    @Test
    fun `startOfToday is midnight`() {
        val start = TimeUtils.startOfToday()
        val cal = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `endOfToday is 23_59_59_999`() {
        val end = TimeUtils.endOfToday()
        val cal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `startOfTomorrow is after endOfToday`() {
        assertTrue(TimeUtils.startOfTomorrow() > TimeUtils.endOfToday())
    }

    // ==================== now ====================

    @Test
    fun `now returns current time`() {
        val before = System.currentTimeMillis()
        val result = TimeUtils.now()
        val after = System.currentTimeMillis()
        assertTrue(result in before..after)
    }

    // ==================== getPercentageOfDayPassed ====================

    @Test
    fun `getPercentageOfDayPassed returns non-negative`() {
        val pct = TimeUtils.getPercentageOfDayPassed()
        assertTrue("Percentage should be >= 0", pct >= 0f)
    }

    // ==================== getTimeUntilFormatted ====================

    @Test
    fun `getTimeUntilFormatted future time starts with in`() {
        val future = System.currentTimeMillis() + 2 * 3_600_000L
        val result = TimeUtils.getTimeUntilFormatted(future)
        assertTrue("Should start with 'in'", result.startsWith("in "))
    }

    @Test
    fun `getTimeUntilFormatted past time ends with ago`() {
        val past = System.currentTimeMillis() - 2 * 3_600_000L
        val result = TimeUtils.getTimeUntilFormatted(past)
        assertTrue("Should end with 'ago'", result.endsWith(" ago"))
    }
}
