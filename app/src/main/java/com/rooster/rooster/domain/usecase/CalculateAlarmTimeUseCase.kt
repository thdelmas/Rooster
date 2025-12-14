package com.rooster.rooster.domain.usecase

import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.Log
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AstronomyRepository
import com.rooster.rooster.util.AppConstants
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
            AppConstants.ALARM_MODE_AT -> {
                if (alarm.relative1 == AppConstants.RELATIVE_TIME_PICK_TIME) {
                    alarm.time1
                } else {
                    getRelativeTime(alarm.relative1)
                }
            }
            AppConstants.ALARM_MODE_BETWEEN -> {
                val time1 = if (alarm.relative1 != AppConstants.RELATIVE_TIME_PICK_TIME) {
                    getRelativeTime(alarm.relative1)
                } else {
                    alarm.time1
                }
                
                val time2 = if (alarm.relative2 != AppConstants.RELATIVE_TIME_PICK_TIME) {
                    getRelativeTime(alarm.relative2)
                } else {
                    alarm.time2
                }
                
                calculateBetweenTime(time1, time2)
            }
            AppConstants.ALARM_MODE_AFTER -> {
                val time1 = getRelativeTime(alarm.relative2)
                val time2 = alarm.time1
                time1 + time2
            }
            AppConstants.ALARM_MODE_BEFORE -> {
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
                AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN -> astronomyData.astroDawn
                AppConstants.SOLAR_EVENT_NAUTICAL_DAWN -> astronomyData.nauticalDawn
                AppConstants.SOLAR_EVENT_CIVIL_DAWN -> astronomyData.civilDawn
                AppConstants.SOLAR_EVENT_SUNRISE -> astronomyData.sunrise
                AppConstants.SOLAR_EVENT_SUNSET -> astronomyData.sunset
                AppConstants.SOLAR_EVENT_CIVIL_DUSK -> astronomyData.civilDusk
                AppConstants.SOLAR_EVENT_NAUTICAL_DUSK -> astronomyData.nauticalDusk
                AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK -> astronomyData.astroDusk
                AppConstants.SOLAR_EVENT_SOLAR_NOON -> astronomyData.solarNoon
                else -> 0
            }
        } else {
            // Fallback to SharedPreferences if not in database (for migration period)
            when (relative) {
                AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN -> sharedPreferences.getLong("astroDawn", 0)
                AppConstants.SOLAR_EVENT_NAUTICAL_DAWN -> sharedPreferences.getLong("nauticalDawn", 0)
                AppConstants.SOLAR_EVENT_CIVIL_DAWN -> sharedPreferences.getLong("civilDawn", 0)
                AppConstants.SOLAR_EVENT_SUNRISE -> sharedPreferences.getLong("sunrise", 0)
                AppConstants.SOLAR_EVENT_SUNSET -> sharedPreferences.getLong("sunset", 0)
                AppConstants.SOLAR_EVENT_CIVIL_DUSK -> sharedPreferences.getLong("civilDusk", 0)
                AppConstants.SOLAR_EVENT_NAUTICAL_DUSK -> sharedPreferences.getLong("nauticalDusk", 0)
                AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK -> sharedPreferences.getLong("astroDusk", 0)
                AppConstants.SOLAR_EVENT_SOLAR_NOON -> sharedPreferences.getLong("solarNoon", 0)
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
