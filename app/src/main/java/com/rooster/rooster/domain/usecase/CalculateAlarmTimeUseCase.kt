package com.rooster.rooster.domain.usecase

import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.Log
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AstronomyRepository
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for calculating alarm trigger times
 */
class CalculateAlarmTimeUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val astronomyRepository: AstronomyRepository
) {
    
    /**
     * Calculate the next trigger time for an alarm
     */
    fun execute(alarm: Alarm): Long {
        Log.d("Rooster", "---\nAlarm: ${alarm.label} - id: ${alarm.id}")
        val calculatedTime = calculateTimeInner(alarm)
        val finalTime = addDays(alarm, calculatedTime)
        
        val calendar = Calendar.getInstance()
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        calendar.timeInMillis = finalTime
        val formattedDate = fullDateFormat.format(calendar.time)
        Log.d("Rooster", "Next Iteration: $formattedDate\n---")
        
        return finalTime
    }
    
    private fun calculateTimeInner(alarm: Alarm): Long {
        return when (alarm.mode) {
            "At" -> {
                if (alarm.relative1 == "Pick Time") {
                    alarm.time1
                } else {
                    getRelativeTime(alarm.relative1)
                }
            }
            "Between" -> {
                val time1 = if (alarm.relative1 != "Pick Time") {
                    getRelativeTime(alarm.relative1)
                } else {
                    alarm.time1
                }
                
                val time2 = if (alarm.relative2 != "Pick Time") {
                    getRelativeTime(alarm.relative2)
                } else {
                    alarm.time2
                }
                
                calculateBetweenTime(time1, time2)
            }
            "After" -> {
                val time1 = getRelativeTime(alarm.relative2)
                val time2 = alarm.time1
                time1 + time2
            }
            "Before" -> {
                val time1 = getRelativeTime(alarm.relative2)
                val time2 = alarm.time1
                time1 - time2
            }
            else -> 0L
        }
    }
    
    private fun calculateBetweenTime(time1: Long, time2: Long): Long {
        val calendarNow = Calendar.getInstance()
        val calendar1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val calendar2 = Calendar.getInstance().apply { timeInMillis = time2 }
        
        // Ensure times are for today, adjust if in the past
        if (calendar1.before(calendarNow)) {
            calendar1.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
            calendar1.set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
            calendar1.set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH))
        }
        if (calendar2.before(calendarNow)) {
            calendar2.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
            calendar2.set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
            calendar2.set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH))
        }
        
        // Calculate the middle point
        val midTime = (calendar1.timeInMillis + calendar2.timeInMillis) / 2
        val calculatedCalendar = Calendar.getInstance().apply { timeInMillis = midTime }
        
        if (calculatedCalendar.timeInMillis <= System.currentTimeMillis()) {
            calculatedCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calculatedCalendar.timeInMillis
    }
    
    private fun addDays(alarm: Alarm, calculatedTime: Long): Long {
        val currentDate = Calendar.getInstance()
        val alarmDate = Calendar.getInstance().apply { timeInMillis = calculatedTime }
        
        while (alarmDate.timeInMillis <= currentDate.timeInMillis) {
            alarmDate.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val alarmDayOfWeek = alarmDate.get(Calendar.DAY_OF_WEEK) - 1
        val weekdays = listOf(
            alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday,
            alarm.thursday, alarm.friday, alarm.saturday
        )
        
        // Start searching from the current day and go up to 7 days
        for (i in 0 until 7) {
            val dayToCheck = (alarmDayOfWeek + i) % 7
            if (weekdays[dayToCheck]) {
                alarmDate.add(Calendar.DAY_OF_MONTH, i)
                return alarmDate.timeInMillis
            }
        }
        
        return alarmDate.timeInMillis
    }
    
    private fun getRelativeTime(relative: String): Long {
        // Try to get astronomy data from Room database first
        val astronomyData = runBlocking {
            astronomyRepository.getAstronomyData(forceRefresh = false)
        }
        
        val timeInMillis = if (astronomyData != null) {
            // Use data from Room database
            when (relative) {
                "Astronomical Dawn" -> astronomyData.astroDawn
                "Nautical Dawn" -> astronomyData.nauticalDawn
                "Civil Dawn" -> astronomyData.civilDawn
                "Sunrise" -> astronomyData.sunrise
                "Sunset" -> astronomyData.sunset
                "Civil Dusk" -> astronomyData.civilDusk
                "Nautical Dusk" -> astronomyData.nauticalDusk
                "Astronomical Dusk" -> astronomyData.astroDusk
                "Solar Noon" -> astronomyData.solarNoon
                else -> 0
            }
        } else {
            // Fallback to SharedPreferences if not in database (for migration period)
            when (relative) {
                "Astronomical Dawn" -> sharedPreferences.getLong("astroDawn", 0)
                "Nautical Dawn" -> sharedPreferences.getLong("nauticalDawn", 0)
                "Civil Dawn" -> sharedPreferences.getLong("civilDawn", 0)
                "Sunrise" -> sharedPreferences.getLong("sunrise", 0)
                "Sunset" -> sharedPreferences.getLong("sunset", 0)
                "Civil Dusk" -> sharedPreferences.getLong("civilDusk", 0)
                "Nautical Dusk" -> sharedPreferences.getLong("nauticalDusk", 0)
                "Astronomical Dusk" -> sharedPreferences.getLong("astroDusk", 0)
                "Solar Noon" -> sharedPreferences.getLong("solarNoon", 0)
                else -> 0
            }
        }
        
        val calendar = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("GMT")
        calendar.timeInMillis = timeInMillis
        
        if (timeZone.inDaylightTime(calendar.time)) {
            val dstOffsetInMillis = timeZone.dstSavings
            calendar.add(Calendar.MILLISECOND, dstOffsetInMillis)
        }
        
        return calendar.timeInMillis
    }
}
