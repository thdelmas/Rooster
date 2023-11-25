package com.rooster.rooster

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import java.util.Date

class AlarmCreation(
    val label: String,
    val enabled: Boolean,
    val mode: String,
    var relative1: String,
    var relative2: String,
    var time1: Long,
    var time2: Long,
    var calculatedTime: Long
    )

class Alarm(
    val id: Long,
    var label: String,
    var enabled: Boolean,
    var mode: String,
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
    var sunday: Boolean
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
        return when (d) {
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
    fun getFormattedTime(timeInSec: Long): CharSequence? {
        val fullDateFormat = SimpleDateFormat("HH:mm")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInSec
        var formattedDate = fullDateFormat.format(calendar.time)
        return formattedDate
    }

    var extended: Boolean = false
}
