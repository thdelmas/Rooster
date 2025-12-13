package com.rooster.rooster.util

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import java.util.Locale

/**
 * Utility functions for time and date operations
 */
object TimeUtils {
    
    /**
     * Get current time in milliseconds
     */
    fun now(): Long = System.currentTimeMillis()
    
    /**
     * Get start of today (midnight)
     */
    fun startOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of today (23:59:59.999)
     */
    fun endOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Get start of tomorrow (midnight)
     */
    fun startOfTomorrow(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Convert hours and minutes to milliseconds since midnight
     */
    fun timeToMillis(hours: Int, minutes: Int): Long {
        return (hours * 60L * 60L * 1000L) + (minutes * 60L * 1000L)
    }
    
    /**
     * Get time in milliseconds for a specific time today
     */
    fun todayAt(hours: Int, minutes: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get time in milliseconds for a specific time tomorrow
     */
    fun tomorrowAt(hours: Int, minutes: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Calculate time difference in a human-readable format
     */
    fun formatTimeDifference(fromMillis: Long, toMillis: Long): String {
        val diffMillis = toMillis - fromMillis
        
        if (diffMillis < 0) {
            return "in the past"
        }
        
        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> {
                val remainingHours = hours % 24
                if (remainingHours > 0) {
                    "$days day${if (days > 1) "s" else ""}, $remainingHours hour${if (remainingHours > 1) "s" else ""}"
                } else {
                    "$days day${if (days > 1) "s" else ""}"
                }
            }
            hours > 0 -> {
                val remainingMinutes = minutes % 60
                if (remainingMinutes > 0) {
                    "$hours hour${if (hours > 1) "s" else ""}, $remainingMinutes minute${if (remainingMinutes > 1) "s" else ""}"
                } else {
                    "$hours hour${if (hours > 1) "s" else ""}"
                }
            }
            minutes > 0 -> {
                "$minutes minute${if (minutes > 1) "s" else ""}"
            }
            else -> {
                "less than a minute"
            }
        }
    }
    
    /**
     * Format milliseconds to a readable time string
     */
    fun formatTime(millis: Long, pattern: String = "HH:mm"): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(calendar.time)
    }
    
    /**
     * Format milliseconds to a readable date string
     */
    fun formatDate(millis: Long, pattern: String = "MMM dd, yyyy"): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(calendar.time)
    }
    
    /**
     * Format milliseconds to a readable date and time string
     */
    fun formatDateTime(millis: Long, pattern: String = "MMM dd, yyyy HH:mm"): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(calendar.time)
    }
    
    /**
     * Get day of week name
     */
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }
    
    /**
     * Get short day name
     */
    fun getShortDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "?"
        }
    }
    
    /**
     * Check if two timestamps are on the same day
     */
    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Check if timestamp is today
     */
    fun isToday(millis: Long): Boolean {
        return isSameDay(millis, now())
    }
    
    /**
     * Check if timestamp is tomorrow
     */
    fun isTomorrow(millis: Long): Boolean {
        return isSameDay(millis, startOfTomorrow())
    }
    
    /**
     * Add days to timestamp
     */
    fun addDays(millis: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.timeInMillis
    }
    
    /**
     * Add hours to timestamp
     */
    fun addHours(millis: Long, hours: Int): Long {
        return millis + (hours * 60 * 60 * 1000L)
    }
    
    /**
     * Add minutes to timestamp
     */
    fun addMinutes(millis: Long, minutes: Int): Long {
        return millis + (minutes * 60 * 1000L)
    }
    
    /**
     * Get time until next occurrence of a specific time
     * Returns milliseconds until next occurrence
     */
    fun timeUntilNext(hours: Int, minutes: Int): Long {
        val targetTime = todayAt(hours, minutes)
        return if (targetTime > now()) {
            targetTime - now()
        } else {
            tomorrowAt(hours, minutes) - now()
        }
    }
    
    /**
     * Get percentage of day passed (0-100)
     */
    fun getPercentageOfDayPassed(): Float {
        val now = now()
        val startOfDay = startOfToday()
        val elapsed = now - startOfDay
        val dayLength = 24 * 60 * 60 * 1000L
        return (elapsed.toFloat() / dayLength) * 100f
    }
    
    /**
     * Get formatted time until a timestamp
     * Returns a human-readable string like "in 2 hours, 30 minutes"
     */
    fun getTimeUntilFormatted(targetMillis: Long): String {
        return if (targetMillis > now()) {
            "in " + formatTimeDifference(now(), targetMillis)
        } else {
            formatTimeDifference(targetMillis, now()) + " ago"
        }
    }
}
