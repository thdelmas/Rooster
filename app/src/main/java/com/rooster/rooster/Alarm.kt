package com.rooster.rooster

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.Log
import java.util.Date

class AlarmCreation(
    val label: String,
    val enabled: Boolean,
    val mode: String,
    var ringtoneUri: String,
    var relative1: String,
    var relative2: String,
    var time1: Long,
    var time2: Long,
    var calculatedTime: Long
    )

data class Alarm(
    val id: Long,
    var label: String,
    var enabled: Boolean,
    var mode: String,
    var ringtoneUri: String,
    var relative1: String,
    var relative2: String,
    var time1: Long,
    var time2: Long,
    var calculatedTime: Long,
    var monday: Boolean,
    var tuesday: Boolean,
    var wednesday: Boolean,
    var thursday: Boolean,
    var friday: Boolean,
    var saturday: Boolean,
    var sunday: Boolean,
    var vibrate: Boolean = true,
    var snoozeEnabled: Boolean = true,
    var snoozeDuration: Int = 10,
    var snoozeCount: Int = 3,
    var volume: Int = 80,
    var gradualVolume: Boolean = false
) {
    fun setDayEnabled(day: String, checked: Boolean) {
        when (day) {
            "monday" -> this.monday = checked
            "tuesday" -> this.tuesday = checked
            "wednesday" -> this.wednesday = checked
            "thursday" -> this.thursday = checked
            "friday" -> this.friday = checked
            "saturday" -> this.saturday = checked
            "sunday" -> this.sunday = checked
            else -> throw IllegalArgumentException("Invalid day of the week: $day")
        }
    }

    fun getDayEnabled(d: String): Boolean {
        val day = d.lowercase() // Convert input string to lowercase

        return when (day) {
            "monday" -> this.monday
            "tuesday" -> this.tuesday
            "wednesday" -> this.wednesday
            "thursday" -> this.thursday
            "friday" -> this.friday
            "saturday" -> this.saturday
            "sunday" -> this.sunday
            else -> throw IllegalArgumentException("Invalid day of the week: $d")
        }
    }

    fun getFormattedTime(timeInSec: Long, dst: Boolean): String {
        val fullDateFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInSec // Convert seconds to milliseconds
        // Consider daylight saving time (DST)
        // Adjust the calendar based on the input time zone's DST offset
        if (dst) {
            val timeZone = TimeZone.getTimeZone("GMT")
            fullDateFormat.timeZone = timeZone
            if (timeZone.inDaylightTime(calendar.time)) {
                val dstOffsetInMillis = timeZone.dstSavings
                calendar.add(Calendar.MILLISECOND, dstOffsetInMillis)
            }
        }
        return fullDateFormat.format(calendar.time)
    }

    var extended: Boolean = false
}
