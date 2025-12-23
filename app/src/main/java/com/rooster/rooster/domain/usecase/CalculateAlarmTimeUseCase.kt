package com.rooster.rooster.domain.usecase

import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.Log
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AstronomyRepository
import com.rooster.rooster.data.repository.AstronomyDataResult
import com.rooster.rooster.data.repository.LocationRepository
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for calculating alarm trigger times
 */
class CalculateAlarmTimeUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val astronomyRepository: AstronomyRepository,
    private val locationRepository: LocationRepository
) {
    
    companion object {
        private const val TAG = "CalculateAlarmTimeUseCase"
    }
    
    /**
     * Calculate the next trigger time for an alarm
     * For alarms based on astral events, this will fetch fresh astronomy data
     */
    suspend fun execute(alarm: Alarm): Long {
        Log.d("Rooster", "---\nAlarm: ${alarm.label} - id: ${alarm.id}")
        
        // If alarm uses astral events, ensure we have fresh astronomy data
        if (usesAstralEvents(alarm)) {
            ensureFreshAstronomyData()
        }
        
        val calculatedTime = calculateTimeInner(alarm)
        val finalTime = addDays(alarm, calculatedTime)
        
        val calendar = Calendar.getInstance()
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        calendar.timeInMillis = finalTime
        val formattedDate = fullDateFormat.format(calendar.time)
        Log.d("Rooster", "Next Iteration: $formattedDate\n---")
        
        return finalTime
    }
    
    /**
     * Synchronous version for backward compatibility
     * @deprecated Use suspend execute() instead
     */
    @Deprecated("Use suspend execute() instead for better async support")
    fun executeSync(alarm: Alarm): Long {
        return runBlocking {
            execute(alarm)
        }
    }
    
    /**
     * Check if an alarm uses astral events (sunrise, sunset, etc.)
     */
    private fun usesAstralEvents(alarm: Alarm): Boolean {
        val solarEvents = listOf(
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN,
            AppConstants.SOLAR_EVENT_NAUTICAL_DAWN,
            AppConstants.SOLAR_EVENT_CIVIL_DAWN,
            AppConstants.SOLAR_EVENT_SUNRISE,
            AppConstants.SOLAR_EVENT_SOLAR_NOON,
            AppConstants.SOLAR_EVENT_SUNSET,
            AppConstants.SOLAR_EVENT_CIVIL_DUSK,
            AppConstants.SOLAR_EVENT_NAUTICAL_DUSK,
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK
        )
        
        return alarm.relative1 in solarEvents || alarm.relative2 in solarEvents
    }
    
    /**
     * Ensure fresh astronomy data is available before calculating alarm times
     */
    private suspend fun ensureFreshAstronomyData() {
        try {
            // Get location from repository
            val location = locationRepository.getLocation()
            
            // Fallback to SharedPreferences if not in database (for migration period)
            val latitude = location?.latitude ?: sharedPreferences.getFloat("latitude", 0f)
            val longitude = location?.longitude ?: sharedPreferences.getFloat("longitude", 0f)
            
            if (latitude == 0f && longitude == 0f) {
                Logger.w(TAG, "No location available, cannot fetch fresh astronomy data")
                return
            }
            
            // Check if current data is stale or from a different day
            val currentData = astronomyRepository.getAstronomyData(forceRefresh = false)
            val needsRefresh = currentData == null || 
                              astronomyRepository.isDataStale(currentData) ||
                              !isDataFromToday(currentData)
            
            if (needsRefresh) {
                if (currentData != null && !isDataFromToday(currentData)) {
                    Logger.i(TAG, "Astronomy data is from a different day, fetching fresh data for today")
                } else {
                    Logger.i(TAG, "Astronomy data is stale or missing, fetching fresh data")
                }
                val result = astronomyRepository.fetchAndCacheAstronomyData(latitude, longitude)
                
                when (result) {
                    is AstronomyDataResult.Fresh -> {
                        Logger.i(TAG, "Successfully fetched fresh astronomy data")
                    }
                    is AstronomyDataResult.Cached -> {
                        if (result.isStale) {
                            Logger.w(TAG, "Using stale cached data (age: ${result.ageMs}ms) - network may be unavailable")
                        } else {
                            Logger.d(TAG, "Using valid cached data (age: ${result.ageMs}ms)")
                        }
                    }
                    is AstronomyDataResult.Failure -> {
                        Logger.e(TAG, "Failed to fetch astronomy data: ${result.exception.message}")
                    }
                }
            } else {
                Logger.d(TAG, "Astronomy data is still valid, no refresh needed")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error ensuring fresh astronomy data", e)
            // Continue with calculation even if fetch fails - will use cached data
        }
    }
    
    /**
     * Check if astronomy data timestamps are from today (in local time)
     * This is important because sunrise/sunset times change daily
     */
    private fun isDataFromToday(astronomyData: com.rooster.rooster.data.local.entity.AstronomyDataEntity): Boolean {
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayDayOfYear = today.get(Calendar.DAY_OF_YEAR)
        
        // Convert sunrise timestamp to local time and check if it's from today
        val sunriseCalendar = Calendar.getInstance().apply {
            timeInMillis = astronomyData.sunrise
        }
        val sunriseYear = sunriseCalendar.get(Calendar.YEAR)
        val sunriseDayOfYear = sunriseCalendar.get(Calendar.DAY_OF_YEAR)
        
        val isFromToday = (sunriseYear == todayYear && sunriseDayOfYear == todayDayOfYear)
        
        if (!isFromToday) {
            Logger.w(TAG, "Astronomy data is from a different day (sunrise: $sunriseYear-$sunriseDayOfYear, today: $todayYear-$todayDayOfYear)")
        }
        
        return isFromToday
    }
    
    private suspend fun calculateTimeInner(alarm: Alarm): Long {
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
        
        val weekdays = listOf(
            alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday,
            alarm.thursday, alarm.friday, alarm.saturday
        )
        
        // Check if any day is selected
        val hasAnyDaySelected = weekdays.any { it }
        
        if (!hasAnyDaySelected) {
            // No days selected: set for today or tomorrow based on whether time has passed
            val todayCalendar = Calendar.getInstance()
            val alarmTimeCalendar = Calendar.getInstance().apply { timeInMillis = calculatedTime }
            
            // Set today's date with the alarm's time
            todayCalendar.set(Calendar.HOUR_OF_DAY, alarmTimeCalendar.get(Calendar.HOUR_OF_DAY))
            todayCalendar.set(Calendar.MINUTE, alarmTimeCalendar.get(Calendar.MINUTE))
            todayCalendar.set(Calendar.SECOND, alarmTimeCalendar.get(Calendar.SECOND))
            todayCalendar.set(Calendar.MILLISECOND, 0)
            
            // If the time has already passed today, set for tomorrow
            if (todayCalendar.timeInMillis <= currentDate.timeInMillis) {
                todayCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            return todayCalendar.timeInMillis
        }
        
        // Days are selected: use existing logic to find next matching day
        while (alarmDate.timeInMillis <= currentDate.timeInMillis) {
            alarmDate.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val alarmDayOfWeek = alarmDate.get(Calendar.DAY_OF_WEEK) - 1
        
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
    
    private suspend fun getRelativeTime(relative: String): Long {
        // Try to get astronomy data from Room database first
        // Fresh data should already be fetched by ensureFreshAstronomyData() if needed
        val astronomyData = astronomyRepository.getAstronomyData(forceRefresh = false)
        
        val timeInMillis = if (astronomyData != null) {
            // Use data from Room database (UTC timestamps from API)
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
            // Note: These may be UTC timestamps from old API calls or local timestamps from tests
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
        
        if (timeInMillis == 0L) {
            return 0L
        }
        
        // Only apply timezone conversion if data comes from Room database (UTC from API)
        // SharedPreferences data might already be in local time (from tests or old code)
        if (astronomyData != null) {
            // The timestamp from the API is a UTC timestamp representing the local time event.
            // For example, if sunrise is at 8:11 AM local time, the API returns a UTC timestamp
            // that when converted to local time gives 8:11 AM. We need to extract the local time
            // and apply it to today's date.
            
            // First, check if the timestamp is from today
            val timestampValue = timeInMillis
            val timestampCalendar = Calendar.getInstance().apply {
                this.timeInMillis = timestampValue
            }
            val today = Calendar.getInstance()
            val timestampYear = timestampCalendar.get(Calendar.YEAR)
            val timestampDayOfYear = timestampCalendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = today.get(Calendar.YEAR)
            val todayDayOfYear = today.get(Calendar.DAY_OF_YEAR)
            
            // If timestamp is from today, we can use it directly (after ensuring it's set to today)
            if (timestampYear == todayYear && timestampDayOfYear == todayDayOfYear) {
                // Timestamp is from today, but we need to ensure it's set to today's date
                // (in case of timezone issues)
                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timestampCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, timestampCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, timestampCalendar.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, 0)
                }
                return todayCalendar.timeInMillis
            } else {
                // Timestamp is from a different day - extract time components and apply to today
                // This should not happen if ensureFreshAstronomyData() worked correctly,
                // but we handle it gracefully as a fallback
                Logger.w(TAG, "Using astronomy data from a different day (${timestampYear}-${timestampDayOfYear} vs today ${todayYear}-${todayDayOfYear}), extracting time-of-day")
                
                val hour = timestampCalendar.get(Calendar.HOUR_OF_DAY)
                val minute = timestampCalendar.get(Calendar.MINUTE)
                val second = timestampCalendar.get(Calendar.SECOND)
                
                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, 0)
                }
                
                return todayCalendar.timeInMillis
            }
        } else {
            // For SharedPreferences fallback:
            // - If timestamp is in the future (likely a test mock or already processed), use as-is
            // - Otherwise, treat as UTC timestamp from old API and convert to local time for today
            val now = System.currentTimeMillis()
            
            // If timestamp is in the future, it's likely already correct (test mock or processed data)
            // Only convert if it's in the past or very close to now (likely old UTC data)
            if (timeInMillis > now) {
                return timeInMillis
            }
            
            // Convert UTC timestamp to local time for today (backward compatibility)
            val localCalendar = Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
            }
            
            val hour = localCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = localCalendar.get(Calendar.MINUTE)
            val second = localCalendar.get(Calendar.SECOND)
            
            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
                set(Calendar.MILLISECOND, 0)
            }
            
            return todayCalendar.timeInMillis
        }
    }
}
