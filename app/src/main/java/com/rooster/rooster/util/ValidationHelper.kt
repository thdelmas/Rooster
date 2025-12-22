package com.rooster.rooster.util

import com.rooster.rooster.Alarm
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.AlarmCreation

/**
 * Helper class for validating alarm data
 */
object ValidationHelper {
    
    private const val TAG = "ValidationHelper"
    
    /**
     * Validate an alarm before saving
     */
    fun validateAlarm(alarm: AlarmCreation): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate label
        if (alarm.label.isBlank()) {
            errors.add("Please enter a name for your alarm")
        }
        
        if (alarm.label.length > 100) {
            errors.add("Alarm name is too long. Please use 100 characters or less")
        }
        
        // Validate mode
        if (!isValidMode(alarm.mode)) {
            errors.add("Invalid alarm mode selected. Please choose a valid mode")
        }
        
        // Validate relative times
        if (alarm.mode in listOf(AppConstants.ALARM_MODE_AT, AppConstants.ALARM_MODE_BETWEEN, AppConstants.ALARM_MODE_AFTER, AppConstants.ALARM_MODE_BEFORE)) {
            if (alarm.relative1.isBlank() && alarm.time1 == 0L) {
                errors.add("Please set a time for your alarm")
            }
        }
        
        // Validate time values
        if (alarm.time1 < 0) {
            errors.add("Invalid alarm time. Please set a valid time")
        }
        
        if (alarm.mode == AppConstants.ALARM_MODE_BETWEEN && alarm.time2 < 0) {
            errors.add("Please set both start and end times for the time range")
        }
        
        // Validate ringtone URI
        if (alarm.ringtoneUri.isBlank()) {
            Logger.w(TAG, "Ringtone URI is empty, using default")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate an existing alarm
     */
    fun validateExistingAlarm(alarm: Alarm): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate ID
        if (alarm.id <= 0) {
            errors.add("Invalid alarm. Please try creating a new alarm")
        }
        
        // Validate label
        if (alarm.label.isBlank()) {
            errors.add("Please enter a name for your alarm")
        }
        
        // Validate mode
        if (!isValidMode(alarm.mode)) {
            errors.add("Invalid alarm mode selected. Please choose a valid mode")
        }
        
        // Note: Day selection is optional. If no days are selected, alarm will fire today or tomorrow.
        
        // Validate calculated time
        if (alarm.enabled && alarm.calculatedTime <= 0) {
            Logger.w(TAG, "Enabled alarm has invalid calculated time")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Check if alarm mode is valid
     */
    private fun isValidMode(mode: String): Boolean {
        return mode in listOf(AppConstants.ALARM_MODE_AT, AppConstants.ALARM_MODE_BETWEEN, AppConstants.ALARM_MODE_AFTER, AppConstants.ALARM_MODE_BEFORE)
    }
    
    /**
     * Validate relative time string
     */
    fun isValidRelativeTime(relative: String): Boolean {
        val validTimes = listOf(
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN,
            AppConstants.SOLAR_EVENT_NAUTICAL_DAWN,
            AppConstants.SOLAR_EVENT_CIVIL_DAWN,
            AppConstants.SOLAR_EVENT_SUNRISE,
            AppConstants.SOLAR_EVENT_SOLAR_NOON,
            AppConstants.SOLAR_EVENT_SUNSET,
            AppConstants.SOLAR_EVENT_CIVIL_DUSK,
            AppConstants.SOLAR_EVENT_NAUTICAL_DUSK,
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK,
            AppConstants.RELATIVE_TIME_PICK_TIME
        )
        return relative in validTimes
    }
    
    /**
     * Validate time in milliseconds
     */
    fun isValidTimeInMillis(time: Long): Boolean {
        // Check if time is within reasonable bounds (not too far in future/past)
        val now = System.currentTimeMillis()
        
        return time > 0 && time < (now + AppConstants.MAX_VALIDATION_TIME_MS)
    }
    
    /**
     * Sanitize alarm label
     */
    fun sanitizeLabel(label: String): String {
        return label.trim().take(100)
    }
    
    /**
     * Validate location coordinates
     */
    fun validateCoordinates(latitude: Float, longitude: Float): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (latitude < -90 || latitude > 90) {
            errors.add("Invalid location. Please select a valid location on the map")
        }
        
        if (longitude < -180 || longitude > 180) {
            errors.add("Invalid location. Please select a valid location on the map")
        }
        
        if (latitude == 0f && longitude == 0f) {
            return ValidationResult.Warning("Location appears to be at the default coordinates. Please select your actual location")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate offset minutes for sun mode alarms
     */
    fun validateOffsetMinutes(offsetMinutes: Int): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (offsetMinutes < 0) {
            errors.add("Time offset cannot be negative. Please enter a positive value")
        }
        
        // Maximum 24 hours (1440 minutes)
        if (offsetMinutes > 1440) {
            errors.add("Time offset cannot exceed 24 hours. Please enter a value between 0 and 1440 minutes")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate selected time for classic mode alarms
     */
    fun validateSelectedTime(selectedTime: Long): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (selectedTime <= 0) {
            errors.add("Selected time must be set")
        }
        
        // Check if time is within reasonable bounds (not too far in past/future)
        val now = System.currentTimeMillis()
        val maxFutureTime = now + AppConstants.MAX_VALIDATION_TIME_MS
        
        if (selectedTime > maxFutureTime) {
            errors.add("Selected time is too far in the future")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate solar event combination for "Between" mode
     * Event1 should occur before Event2 chronologically
     */
    fun validateSolarEventCombination(
        event1: String,
        event2: String,
        mode: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (mode == AppConstants.ALARM_MODE_BETWEEN) {
            // Define the chronological order of solar events
            val eventOrder = listOf(
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
            
            val index1 = eventOrder.indexOf(event1)
            val index2 = eventOrder.indexOf(event2)
            
            if (index1 == -1) {
                errors.add("Please select a valid first solar event")
            }
            
            if (index2 == -1) {
                errors.add("Please select a valid second solar event")
            }
            
            if (index1 != -1 && index2 != -1 && index1 >= index2) {
                errors.add("The first solar event must occur before the second one. Please adjust your selection")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate day selection
     * Note: Day selection is optional. If no days are selected, alarm will fire today or tomorrow.
     */
    fun validateDaySelection(
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean
    ): ValidationResult {
        // Day selection is optional - no validation needed
        return ValidationResult.Success
    }
    
    /**
     * Validate snooze settings
     */
    fun validateSnoozeSettings(
        snoozeDuration: Int,
        snoozeCount: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Snooze duration should be between 5 and 30 minutes
        if (snoozeDuration < 5 || snoozeDuration > 30) {
            errors.add("Snooze duration must be between 5 and 30 minutes. Please adjust the value")
        }
        
        // Snooze count should be between 1 and 10
        if (snoozeCount < 1 || snoozeCount > 10) {
            errors.add("Snooze count must be between 1 and 10. Please adjust the value")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validate alarm volume
     */
    fun validateVolume(volume: Int): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (volume < 0 || volume > 100) {
            errors.add("Volume must be between 0% and 100%. Please adjust the volume slider")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Comprehensive validation for alarm editor inputs
     * This validates all inputs from AlarmEditorActivity
     */
    fun validateAlarmEditorInputs(
        label: String,
        mode: String,
        sunTimingMode: String,
        solarEvent1: String,
        solarEvent2: String,
        offsetMinutes: Int,
        selectedTime: Long,
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean,
        snoozeDuration: Int,
        snoozeCount: Int,
        volume: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate label
        val sanitizedLabel = sanitizeLabel(label)
        if (sanitizedLabel.isBlank()) {
            errors.add("Please enter a name for your alarm")
        }
        
        // Validate mode
        if (mode != "sun" && mode != "classic") {
            errors.add("Please select either Sun Mode or Classic Mode")
        }
        
        // Validate sun mode specific inputs
        if (mode == "sun") {
            if (!isValidMode(sunTimingMode)) {
                errors.add("Please select when the alarm should trigger (At, Before, After, or Between)")
            }
            
            if (!isValidRelativeTime(solarEvent1)) {
                errors.add("Please select a valid solar event (sunrise, sunset, etc.)")
            }
            
            // Validate offset for Before/After modes
            if (sunTimingMode == AppConstants.ALARM_MODE_BEFORE || 
                sunTimingMode == AppConstants.ALARM_MODE_AFTER) {
                val offsetResult = validateOffsetMinutes(offsetMinutes)
                if (offsetResult.isError()) {
                    errors.addAll((offsetResult as ValidationResult.Error).errors)
                }
            }
            
            // Validate solar event combination for Between mode
            if (sunTimingMode == AppConstants.ALARM_MODE_BETWEEN) {
                if (!isValidRelativeTime(solarEvent2)) {
                    errors.add("Please select a valid second solar event")
                }
                val eventResult = validateSolarEventCombination(solarEvent1, solarEvent2, sunTimingMode)
                if (eventResult.isError()) {
                    errors.addAll((eventResult as ValidationResult.Error).errors)
                }
            }
        } else {
            // Validate classic mode - selected time
            val timeResult = validateSelectedTime(selectedTime)
            if (timeResult.isError()) {
                errors.addAll((timeResult as ValidationResult.Error).errors)
            }
        }
        
        // Validate day selection (optional - if no days selected, alarm fires today or tomorrow)
        // No validation needed - day selection is optional
        
        // Validate snooze settings
        val snoozeResult = validateSnoozeSettings(snoozeDuration, snoozeCount)
        if (snoozeResult.isError()) {
            errors.addAll((snoozeResult as ValidationResult.Error).errors)
        }
        
        // Validate volume
        val volumeResult = validateVolume(volume)
        if (volumeResult.isError()) {
            errors.addAll((volumeResult as ValidationResult.Error).errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

/**
 * Result of a validation operation
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isWarning(): Boolean = this is Warning
    
    fun getErrorMessage(): String {
        return when (this) {
            is Error -> errors.joinToString("\n")
            is Warning -> message
            is Success -> ""
        }
    }
}
