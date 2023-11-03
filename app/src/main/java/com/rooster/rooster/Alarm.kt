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
        val minutes = (timeInSec / 60) % 60
        val hours = timeInSec / 3600
        val calendar = Calendar.getInstance()
        if (hours < calendar.get(Calendar.HOUR_OF_DAY) ||
            (hours.toInt() == calendar.get(Calendar.HOUR_OF_DAY) && minutes < calendar.get(Calendar.MINUTE))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, hours.toInt())
        calendar.set(Calendar.MINUTE, minutes.toInt())
        calendar.set(Calendar.SECOND, 0)

        val timeInHumanReadableFormat = SimpleDateFormat("HH:mm").format(calendar.time)
        return timeInHumanReadableFormat
    }

    var extended: Boolean = false
}
