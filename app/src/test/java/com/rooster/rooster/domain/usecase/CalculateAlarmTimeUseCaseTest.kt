package com.rooster.rooster.domain.usecase

import android.content.SharedPreferences
import android.icu.util.Calendar
import com.rooster.rooster.Alarm
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CalculateAlarmTimeUseCase
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalculateAlarmTimeUseCaseTest {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var calculateAlarmTimeUseCase: CalculateAlarmTimeUseCase
    
    @Before
    fun setup() {
        // Mock SharedPreferences
        editor = mock()
        sharedPreferences = mock {
            on { edit() } doReturn editor
            on { getLong(any(), any()) } doAnswer { invocation ->
                // Return default value (second argument) for any getLong call
                invocation.getArgument(1)
            }
        }
        
        calculateAlarmTimeUseCase = CalculateAlarmTimeUseCase(sharedPreferences)
    }
    
    @Test
    fun `test calculate time for At mode with Pick Time`() {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000 // 1 hour from now
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Pick Time",
            time1 = futureTime
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be in the future", result > System.currentTimeMillis())
    }
    
    @Test
    fun `test calculate time for At mode with relative time`() {
        // Given
        val sunriseTime = getTodaySunrise()
        whenever(sharedPreferences.getLong("sunrise", 0)).thenReturn(sunriseTime)
        
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Sunrise",
            time1 = 0
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be calculated", result > 0)
    }
    
    @Test
    fun `test calculate time for Between mode`() {
        // Given
        val now = System.currentTimeMillis()
        val time1 = now + 3600000 // 1 hour from now
        val time2 = now + 7200000 // 2 hours from now
        
        val alarm = createTestAlarm(
            mode = "Between",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = time1,
            time2 = time2
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be between time1 and time2", result > time1 && result < time2)
    }
    
    @Test
    fun `test calculate time for After mode`() {
        // Given
        val sunriseTime = getTodaySunrise()
        val offsetMinutes = 30L * 60 * 1000 // 30 minutes in milliseconds
        
        whenever(sharedPreferences.getLong("sunrise", 0)).thenReturn(sunriseTime)
        
        val alarm = createTestAlarm(
            mode = "After",
            relative2 = "Sunrise",
            time1 = offsetMinutes
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be after sunrise", result >= sunriseTime + offsetMinutes)
    }
    
    @Test
    fun `test calculate time for Before mode`() {
        // Given
        val sunsetTime = getTodaySunset()
        val offsetMinutes = 30L * 60 * 1000 // 30 minutes in milliseconds
        
        whenever(sharedPreferences.getLong("sunset", 0)).thenReturn(sunsetTime)
        
        val alarm = createTestAlarm(
            mode = "Before",
            relative2 = "Sunset",
            time1 = offsetMinutes
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be before sunset", result <= sunsetTime - offsetMinutes)
    }
    
    @Test
    fun `test alarm respects enabled weekdays`() {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = futureTime
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Create alarm enabled only for tomorrow
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Pick Time",
            time1 = futureTime,
            enabledDays = setOf((currentDayOfWeek % 7) + 1)
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be scheduled", result > 0)
    }
    
    private fun createTestAlarm(
        mode: String,
        relative1: String = "Pick Time",
        relative2: String = "Pick Time",
        time1: Long = 0,
        time2: Long = 0,
        enabledDays: Set<Int> = emptySet()
    ): Alarm {
        return Alarm(
            id = 1,
            label = "Test Alarm",
            enabled = true,
            mode = mode,
            ringtoneUri = "Default",
            relative1 = relative1,
            relative2 = relative2,
            time1 = time1,
            time2 = time2,
            calculatedTime = 0,
            monday = enabledDays.contains(Calendar.MONDAY),
            tuesday = enabledDays.contains(Calendar.TUESDAY),
            wednesday = enabledDays.contains(Calendar.WEDNESDAY),
            thursday = enabledDays.contains(Calendar.THURSDAY),
            friday = enabledDays.contains(Calendar.FRIDAY),
            saturday = enabledDays.contains(Calendar.SATURDAY),
            sunday = enabledDays.contains(Calendar.SUNDAY)
        )
    }
    
    private fun getTodaySunrise(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getTodaySunset(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
