package com.rooster.rooster.presentation.compose

import com.rooster.rooster.Alarm
import com.rooster.rooster.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun formatAlarmTime(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Calendar.getInstance().apply { this.timeInMillis = timeInMillis }.time)
}

internal fun modeBadgeFor(alarm: Alarm): String =
    if (alarm.relative1 != "Pick Time") "☀️" else "🕐"

internal fun modeDescriptionFor(alarm: Alarm): String {
    if (alarm.relative1 == "Pick Time") return "Classic alarm"
    return when (alarm.mode) {
        "At" -> "At ${alarm.relative1}"
        "Before" -> {
            val minutes = (alarm.time1 / 1000 / 60).toInt()
            "${TimeUtils.formatMinutesAsHours(minutes)} before ${alarm.relative2}"
        }
        "After" -> {
            val minutes = (alarm.time1 / 1000 / 60).toInt()
            "${TimeUtils.formatMinutesAsHours(minutes)} after ${alarm.relative2}"
        }
        "Between" -> "Between ${alarm.relative1} and ${alarm.relative2}"
        else -> "Sun course alarm"
    }
}

internal fun repeatDaysFor(alarm: Alarm): String {
    val activeDays = buildList {
        if (alarm.monday) add(Calendar.MONDAY)
        if (alarm.tuesday) add(Calendar.TUESDAY)
        if (alarm.wednesday) add(Calendar.WEDNESDAY)
        if (alarm.thursday) add(Calendar.THURSDAY)
        if (alarm.friday) add(Calendar.FRIDAY)
        if (alarm.saturday) add(Calendar.SATURDAY)
        if (alarm.sunday) add(Calendar.SUNDAY)
    }
    return when {
        activeDays.isEmpty() -> oneShotDayLabel(alarm.calculatedTime)
        activeDays.size == 7 -> "Every day"
        activeDays.size == 5 && !alarm.saturday && !alarm.sunday -> "Weekdays"
        activeDays.size == 2 && alarm.saturday && alarm.sunday -> "Weekends"
        else -> {
            val sorted = activeDays.sorted()
            val names = sorted.map(::shortDayName)
            if (sorted.size > 2 && areConsecutive(sorted)) {
                "${names.first()} - ${names.last()}"
            } else {
                names.joinToString(", ")
            }
        }
    }
}

private fun oneShotDayLabel(timeInMillis: Long): String {
    val alarmCal = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    return when {
        sameDay(alarmCal, today) -> "Today"
        sameDay(alarmCal, tomorrow) -> "Tomorrow"
        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(alarmCal.time)
    }
}

internal fun ringScheduleMessage(alarm: Alarm): String {
    val alarmCal = Calendar.getInstance().apply { timeInMillis = alarm.calculatedTime }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(alarmCal.time)
    val whenLabel = when {
        sameDay(alarmCal, today) -> "today at $formattedTime"
        sameDay(alarmCal, tomorrow) -> "tomorrow at $formattedTime"
        else -> "${SimpleDateFormat("EEEE", Locale.getDefault()).format(alarmCal.time)} at $formattedTime"
    }
    return "Alarm will ring $whenLabel"
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun shortDayName(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    Calendar.SUNDAY -> "Sun"
    else -> "?"
}

private fun areConsecutive(days: List<Int>): Boolean {
    if (days.size < 2) return false
    for (i in 0 until days.size - 1) {
        if (days[i + 1] != days[i] + 1) return false
    }
    return true
}

internal fun sortByTimeOfDay(alarms: List<Alarm>): List<Alarm> = alarms.sortedBy { alarm ->
    val cal = Calendar.getInstance().apply { timeInMillis = alarm.calculatedTime }
    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}
