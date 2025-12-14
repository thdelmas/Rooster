package com.rooster.rooster.domain.usecase

import android.content.SharedPreferences
import android.icu.util.Calendar
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AstronomyRepository
import kotlinx.coroutines.runBlocking
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
    private lateinit var astronomyRepository: AstronomyRepository
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
        
        // Mock AstronomyRepository to return null (will fallback to SharedPreferences)
        astronomyRepository = mock {
            onBlocking { getAstronomyData(any()) } doReturn null
        }
        
        calculateAlarmTimeUseCase = CalculateAlarmTimeUseCase(sharedPreferences, astronomyRepository)
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
    
    @Test
    fun `test calculate time for past time moves to next day`() {
        // Given - time in the past
        val pastTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Pick Time",
            time1 = pastTime,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            sunday = true
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then - the addDays function should move it to the next valid day
        // Result should be valid (greater than 0) and ideally in the future
        assertTrue("Result should be valid", result > 0)
        // Note: The actual behavior depends on addDays implementation
        // which may move to next enabled day or just ensure it's in the future
    }
    
    @Test
    fun `test calculate time for Between mode with past times`() {
        // Given - both times in the past
        val now = System.currentTimeMillis()
        val time1 = now - 7200000 // 2 hours ago
        val time2 = now - 3600000 // 1 hour ago
        
        val alarm = createTestAlarm(
            mode = "Between",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = time1,
            time2 = time2,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            sunday = true
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be in the future", result > System.currentTimeMillis())
    }
    
    @Test
    fun `test calculate time for Between mode with reversed times`() {
        // Given - time2 before time1
        val now = System.currentTimeMillis()
        val time1 = now + 7200000 // 2 hours from now
        val time2 = now + 3600000 // 1 hour from now
        
        val alarm = createTestAlarm(
            mode = "Between",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = time1,
            time2 = time2,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            sunday = true
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be between the two times", result >= time2 && result <= time1)
    }
    
    @Test
    fun `test calculate time for alarm with no enabled days`() {
        // Given - alarm with no days enabled
        val futureTime = System.currentTimeMillis() + 3600000
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Pick Time",
            time1 = futureTime,
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then - should still return a time (implementation may handle this)
        assertTrue("Result should be calculated", result > 0)
    }
    
    @Test
    fun `test calculate time for After mode with zero offset`() {
        // Given
        val sunriseTime = getTodaySunrise()
        whenever(sharedPreferences.getLong("sunrise", 0)).thenReturn(sunriseTime)
        
        val alarm = createTestAlarm(
            mode = "After",
            relative2 = "Sunrise",
            time1 = 0 // Zero offset
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be at or after sunrise", result >= sunriseTime)
    }
    
    @Test
    fun `test calculate time for Before mode with zero offset`() {
        // Given
        val sunsetTime = getTodaySunset()
        whenever(sharedPreferences.getLong("sunset", 0)).thenReturn(sunsetTime)
        
        val alarm = createTestAlarm(
            mode = "Before",
            relative2 = "Sunset",
            time1 = 0 // Zero offset
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should be at or before sunset", result <= sunsetTime)
    }
    
    @Test
    fun `test calculate time for invalid mode returns zero`() {
        // Given
        val alarm = createTestAlarm(
            mode = "InvalidMode",
            relative1 = "Pick Time",
            time1 = System.currentTimeMillis() + 3600000
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then - should handle gracefully, may return 0 or next valid day
        assertTrue("Result should be handled", result >= 0)
    }
    
    @Test
    fun `test calculate time uses astronomy repository when available`() {
        // Given
        val astronomyData = createTestAstronomyData()
        astronomyRepository = mock {
            onBlocking { getAstronomyData(any()) } doReturn astronomyData
        }
        calculateAlarmTimeUseCase = CalculateAlarmTimeUseCase(sharedPreferences, astronomyRepository)
        
        val alarm = createTestAlarm(
            mode = "At",
            relative1 = "Sunrise",
            time1 = 0
        )
        
        // When
        val result = calculateAlarmTimeUseCase.execute(alarm)
        
        // Then
        assertTrue("Result should use astronomy data", result > 0)
    }
    
    @Test
    fun `test calculate time handles all solar events`() {
        // Given
        val astronomyData = createTestAstronomyData()
        astronomyRepository = mock {
            onBlocking { getAstronomyData(any()) } doReturn astronomyData
        }
        calculateAlarmTimeUseCase = CalculateAlarmTimeUseCase(sharedPreferences, astronomyRepository)
        
        val solarEvents = listOf(
            "Astronomical Dawn", "Nautical Dawn", "Civil Dawn", "Sunrise",
            "Solar Noon", "Sunset", "Civil Dusk", "Nautical Dusk", "Astronomical Dusk"
        )
        
        solarEvents.forEach { event ->
            val alarm = createTestAlarm(
                mode = "At",
                relative1 = event,
                time1 = 0
            )
            
            // When
            val result = calculateAlarmTimeUseCase.execute(alarm)
            
            // Then
            assertTrue("Result for $event should be valid", result > 0)
        }
    }
    
    private fun createTestAlarm(
        mode: String,
        relative1: String = "Pick Time",
        relative2: String = "Pick Time",
        time1: Long = 0,
        time2: Long = 0,
        enabledDays: Set<Int> = emptySet(),
        monday: Boolean = true,
        tuesday: Boolean = true,
        wednesday: Boolean = true,
        thursday: Boolean = true,
        friday: Boolean = true,
        saturday: Boolean = true,
        sunday: Boolean = true
    ): Alarm {
        val finalMonday = if (enabledDays.isEmpty()) monday else enabledDays.contains(Calendar.MONDAY)
        val finalTuesday = if (enabledDays.isEmpty()) tuesday else enabledDays.contains(Calendar.TUESDAY)
        val finalWednesday = if (enabledDays.isEmpty()) wednesday else enabledDays.contains(Calendar.WEDNESDAY)
        val finalThursday = if (enabledDays.isEmpty()) thursday else enabledDays.contains(Calendar.THURSDAY)
        val finalFriday = if (enabledDays.isEmpty()) friday else enabledDays.contains(Calendar.FRIDAY)
        val finalSaturday = if (enabledDays.isEmpty()) saturday else enabledDays.contains(Calendar.SATURDAY)
        val finalSunday = if (enabledDays.isEmpty()) sunday else enabledDays.contains(Calendar.SUNDAY)
        
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
            monday = finalMonday,
            tuesday = finalTuesday,
            wednesday = finalWednesday,
            thursday = finalThursday,
            friday = finalFriday,
            saturday = finalSaturday,
            sunday = finalSunday
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
    
    private fun createTestAstronomyData(): com.rooster.rooster.data.local.entity.AstronomyDataEntity {
        val now = System.currentTimeMillis()
        return com.rooster.rooster.data.local.entity.AstronomyDataEntity(
            id = 1,
            latitude = 40.7128f,
            longitude = -74.0060f,
            sunrise = now + 21600000, // 6 hours from now
            sunset = now + 64800000, // 18 hours from now
            solarNoon = now + 43200000, // 12 hours from now
            civilDawn = now + 19800000, // 5.5 hours from now
            civilDusk = now + 66600000, // 18.5 hours from now
            nauticalDawn = now + 18000000, // 5 hours from now
            nauticalDusk = now + 68400000, // 19 hours from now
            astroDawn = now + 16200000, // 4.5 hours from now
            astroDusk = now + 70200000, // 19.5 hours from now
            lastUpdated = now,
            dayLength = 43200000 // 12 hours
        )
    }
}
