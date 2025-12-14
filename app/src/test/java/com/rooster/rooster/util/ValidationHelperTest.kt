package com.rooster.rooster.util

import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ValidationHelper
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ValidationHelperTest {
    
    @Test
    fun `test validateAlarm with valid alarm returns success`() {
        // Given
        val alarm = AlarmCreation(
            label = "Test Alarm",
            enabled = true,
            mode = AppConstants.ALARM_MODE_AT,
            ringtoneUri = "Default",
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
            relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = 0
        )
        
        // When
        val result = ValidationHelper.validateAlarm(alarm)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateAlarm with empty label returns error`() {
        // Given
        val alarm = AlarmCreation(
            label = "",
            enabled = true,
            mode = AppConstants.ALARM_MODE_AT,
            ringtoneUri = "Default",
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
            relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = 0
        )
        
        // When
        val result = ValidationHelper.validateAlarm(alarm)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention label", result.getErrorMessage().contains("name") || result.getErrorMessage().contains("label"))
    }
    
    @Test
    fun `test validateAlarm with label too long returns error`() {
        // Given
        val longLabel = "a".repeat(101) // 101 characters
        val alarm = AlarmCreation(
            label = longLabel,
            enabled = true,
            mode = AppConstants.ALARM_MODE_AT,
            ringtoneUri = "Default",
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
            relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = 0
        )
        
        // When
        val result = ValidationHelper.validateAlarm(alarm)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention length", result.getErrorMessage().contains("too long"))
    }
    
    @Test
    fun `test validateAlarm with invalid mode returns error`() {
        // Given
        val alarm = AlarmCreation(
            label = "Test Alarm",
            enabled = true,
            mode = "InvalidMode",
            ringtoneUri = "Default",
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
            relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = 0
        )
        
        // When
        val result = ValidationHelper.validateAlarm(alarm)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention mode", result.getErrorMessage().contains("mode"))
    }
    
    @Test
    fun `test validateExistingAlarm with valid alarm returns success`() {
        // Given
        val alarm = createTestAlarm()
        
        // When
        val result = ValidationHelper.validateExistingAlarm(alarm)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateExistingAlarm with invalid ID returns error`() {
        // Given
        val alarm = createTestAlarm(id = 0)
        
        // When
        val result = ValidationHelper.validateExistingAlarm(alarm)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention invalid alarm", result.getErrorMessage().contains("Invalid") || result.getErrorMessage().contains("alarm"))
    }
    
    @Test
    fun `test validateExistingAlarm with no days selected returns error`() {
        // Given
        val alarm = createTestAlarm(
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
        
        // When
        val result = ValidationHelper.validateExistingAlarm(alarm)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention days", result.getErrorMessage().contains("day"))
    }
    
    @Test
    fun `test validateOffsetMinutes with valid offset returns success`() {
        // When
        val result = ValidationHelper.validateOffsetMinutes(30)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateOffsetMinutes with negative offset returns error`() {
        // When
        val result = ValidationHelper.validateOffsetMinutes(-10)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention negative", result.getErrorMessage().contains("negative"))
    }
    
    @Test
    fun `test validateOffsetMinutes with too large offset returns error`() {
        // When
        val result = ValidationHelper.validateOffsetMinutes(1500) // > 1440 minutes (24 hours)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention 24 hours", result.getErrorMessage().contains("24 hours"))
    }
    
    @Test
    fun `test validateSelectedTime with valid time returns success`() {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000 // 1 hour from now
        
        // When
        val result = ValidationHelper.validateSelectedTime(futureTime)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateSelectedTime with zero time returns error`() {
        // When
        val result = ValidationHelper.validateSelectedTime(0)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention time", result.getErrorMessage().contains("time"))
    }
    
    @Test
    fun `test validateSolarEventCombination with valid order returns success`() {
        // When
        val result = ValidationHelper.validateSolarEventCombination(
            AppConstants.SOLAR_EVENT_SUNRISE,
            AppConstants.SOLAR_EVENT_SUNSET,
            AppConstants.ALARM_MODE_BETWEEN
        )
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateSolarEventCombination with reversed order returns error`() {
        // When
        val result = ValidationHelper.validateSolarEventCombination(
            AppConstants.SOLAR_EVENT_SUNSET,
            AppConstants.SOLAR_EVENT_SUNRISE,
            AppConstants.ALARM_MODE_BETWEEN
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention order", result.getErrorMessage().contains("before"))
    }
    
    @Test
    fun `test validateDaySelection with at least one day returns success`() {
        // When
        val result = ValidationHelper.validateDaySelection(
            monday = true,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateDaySelection with no days returns error`() {
        // When
        val result = ValidationHelper.validateDaySelection(
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention day", result.getErrorMessage().contains("day"))
    }
    
    @Test
    fun `test validateSnoozeSettings with valid settings returns success`() {
        // When
        val result = ValidationHelper.validateSnoozeSettings(
            snoozeDuration = 10,
            snoozeCount = 3
        )
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateSnoozeSettings with duration too low returns error`() {
        // When
        val result = ValidationHelper.validateSnoozeSettings(
            snoozeDuration = 4, // < 5 minutes
            snoozeCount = 3
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention duration", result.getErrorMessage().contains("duration"))
    }
    
    @Test
    fun `test validateSnoozeSettings with duration too high returns error`() {
        // When
        val result = ValidationHelper.validateSnoozeSettings(
            snoozeDuration = 31, // > 30 minutes
            snoozeCount = 3
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention duration", result.getErrorMessage().contains("duration"))
    }
    
    @Test
    fun `test validateSnoozeSettings with count too low returns error`() {
        // When
        val result = ValidationHelper.validateSnoozeSettings(
            snoozeDuration = 10,
            snoozeCount = 0 // < 1
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention count", result.getErrorMessage().contains("count"))
    }
    
    @Test
    fun `test validateSnoozeSettings with count too high returns error`() {
        // When
        val result = ValidationHelper.validateSnoozeSettings(
            snoozeDuration = 10,
            snoozeCount = 11 // > 10
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention count", result.getErrorMessage().contains("count"))
    }
    
    @Test
    fun `test validateVolume with valid volume returns success`() {
        // When
        val result = ValidationHelper.validateVolume(50)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateVolume with negative volume returns error`() {
        // When
        val result = ValidationHelper.validateVolume(-1)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention volume", result.getErrorMessage().contains("Volume"))
    }
    
    @Test
    fun `test validateVolume with volume too high returns error`() {
        // When
        val result = ValidationHelper.validateVolume(101)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention volume", result.getErrorMessage().contains("Volume"))
    }
    
    @Test
    fun `test validateCoordinates with valid coordinates returns success`() {
        // When
        val result = ValidationHelper.validateCoordinates(40.7128f, -74.0060f)
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateCoordinates with invalid latitude returns error`() {
        // When
        val result = ValidationHelper.validateCoordinates(91f, -74.0060f)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention location", result.getErrorMessage().contains("location") || result.getErrorMessage().contains("Invalid"))
    }
    
    @Test
    fun `test validateCoordinates with invalid longitude returns error`() {
        // When
        val result = ValidationHelper.validateCoordinates(40.7128f, -181f)
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention location", result.getErrorMessage().contains("location") || result.getErrorMessage().contains("Invalid"))
    }
    
    @Test
    fun `test validateCoordinates with zero coordinates returns warning`() {
        // When
        val result = ValidationHelper.validateCoordinates(0f, 0f)
        
        // Then
        assertTrue("Result should be warning", result.isWarning())
        assertTrue("Warning should mention coordinates or location", result.getErrorMessage().contains("coordinates") || result.getErrorMessage().contains("location") || result.getErrorMessage().contains("default"))
    }
    
    @Test
    fun `test validateAlarmEditorInputs with valid inputs returns success`() {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        
        // When
        val result = ValidationHelper.validateAlarmEditorInputs(
            label = "Test Alarm",
            mode = "sun",
            sunTimingMode = AppConstants.ALARM_MODE_AT,
            solarEvent1 = AppConstants.SOLAR_EVENT_SUNRISE,
            solarEvent2 = AppConstants.SOLAR_EVENT_SUNSET,
            offsetMinutes = 30,
            selectedTime = futureTime,
            monday = true,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false,
            snoozeDuration = 10,
            snoozeCount = 3,
            volume = 80
        )
        
        // Then
        assertTrue("Result should be success", result.isSuccess())
    }
    
    @Test
    fun `test validateAlarmEditorInputs with invalid label returns error`() {
        // When
        val result = ValidationHelper.validateAlarmEditorInputs(
            label = "",
            mode = "sun",
            sunTimingMode = AppConstants.ALARM_MODE_AT,
            solarEvent1 = AppConstants.SOLAR_EVENT_SUNRISE,
            solarEvent2 = AppConstants.SOLAR_EVENT_SUNSET,
            offsetMinutes = 30,
            selectedTime = System.currentTimeMillis() + 3600000,
            monday = true,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false,
            snoozeDuration = 10,
            snoozeCount = 3,
            volume = 80
        )
        
        // Then
        assertTrue("Result should be error", result.isError())
        assertTrue("Error should mention label", result.getErrorMessage().contains("name") || result.getErrorMessage().contains("label"))
    }
    
    @Test
    fun `test sanitizeLabel trims whitespace`() {
        // When
        val result = ValidationHelper.sanitizeLabel("  Test Alarm  ")
        
        // Then
        assertEquals("Should trim whitespace", "Test Alarm", result)
    }
    
    @Test
    fun `test sanitizeLabel limits to 100 characters`() {
        // Given
        val longLabel = "a".repeat(150)
        
        // When
        val result = ValidationHelper.sanitizeLabel(longLabel)
        
        // Then
        assertEquals("Should limit to 100 characters", 100, result.length)
    }
    
    @Test
    fun `test isValidRelativeTime with valid relative time returns true`() {
        // When
        val result = ValidationHelper.isValidRelativeTime(AppConstants.SOLAR_EVENT_SUNRISE)
        
        // Then
        assertTrue("Should be valid", result)
    }
    
    @Test
    fun `test isValidRelativeTime with invalid relative time returns false`() {
        // When
        val result = ValidationHelper.isValidRelativeTime("Invalid Time")
        
        // Then
        assertFalse("Should be invalid", result)
    }
    
    @Test
    fun `test isValidTimeInMillis with valid time returns true`() {
        // Given
        val futureTime = System.currentTimeMillis() + 3600000
        
        // When
        val result = ValidationHelper.isValidTimeInMillis(futureTime)
        
        // Then
        assertTrue("Should be valid", result)
    }
    
    @Test
    fun `test isValidTimeInMillis with zero time returns false`() {
        // When
        val result = ValidationHelper.isValidTimeInMillis(0)
        
        // Then
        assertFalse("Should be invalid", result)
    }
    
    private fun createTestAlarm(
        id: Long = 1,
        label: String = "Test Alarm",
        monday: Boolean = true,
        tuesday: Boolean = false,
        wednesday: Boolean = false,
        thursday: Boolean = false,
        friday: Boolean = false,
        saturday: Boolean = false,
        sunday: Boolean = false
    ): Alarm {
        return Alarm(
            id = id,
            label = label,
            enabled = true,
            mode = AppConstants.ALARM_MODE_AT,
            ringtoneUri = "Default",
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
            relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = 0,
            monday = monday,
            tuesday = tuesday,
            wednesday = wednesday,
            thursday = thursday,
            friday = friday,
            saturday = saturday,
            sunday = sunday
        )
    }
}
