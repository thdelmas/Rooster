package com.rooster.rooster.util

import android.util.Log
import com.rooster.rooster.Alarm
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
            errors.add("Alarm label cannot be empty")
        }
        
        if (alarm.label.length > 100) {
            errors.add("Alarm label is too long (max 100 characters)")
        }
        
        // Validate mode
        if (!isValidMode(alarm.mode)) {
            errors.add("Invalid alarm mode: ${alarm.mode}")
        }
        
        // Validate relative times
        if (alarm.mode in listOf("At", "Between", "After", "Before")) {
            if (alarm.relative1.isBlank() && alarm.time1 == 0L) {
                errors.add("Alarm time is not set")
            }
        }
        
        // Validate time values
        if (alarm.time1 < 0) {
            errors.add("Invalid time1 value")
        }
        
        if (alarm.mode == "Between" && alarm.time2 < 0) {
            errors.add("Invalid time2 value")
        }
        
        // Validate ringtone URI
        if (alarm.ringtoneUri.isBlank()) {
            Log.w(TAG, "Ringtone URI is empty, using default")
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
            errors.add("Invalid alarm ID")
        }
        
        // Validate label
        if (alarm.label.isBlank()) {
            errors.add("Alarm label cannot be empty")
        }
        
        // Validate mode
        if (!isValidMode(alarm.mode)) {
            errors.add("Invalid alarm mode: ${alarm.mode}")
        }
        
        // Validate at least one day is selected
        if (!hasAtLeastOneDaySelected(alarm)) {
            errors.add("At least one day must be selected")
        }
        
        // Validate calculated time
        if (alarm.enabled && alarm.calculatedTime <= 0) {
            Log.w(TAG, "Enabled alarm has invalid calculated time")
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
        return mode in listOf("At", "Between", "After", "Before")
    }
    
    /**
     * Check if at least one day is selected
     */
    private fun hasAtLeastOneDaySelected(alarm: Alarm): Boolean {
        return alarm.monday || alarm.tuesday || alarm.wednesday || 
               alarm.thursday || alarm.friday || alarm.saturday || alarm.sunday
    }
    
    /**
     * Validate relative time string
     */
    fun isValidRelativeTime(relative: String): Boolean {
        val validTimes = listOf(
            "Astronomical Dawn",
            "Nautical Dawn",
            "Civil Dawn",
            "Sunrise",
            "Solar Noon",
            "Sunset",
            "Civil Dusk",
            "Nautical Dusk",
            "Astronomical Dusk",
            "Pick Time"
        )
        return relative in validTimes
    }
    
    /**
     * Validate time in milliseconds
     */
    fun isValidTimeInMillis(time: Long): Boolean {
        // Check if time is within reasonable bounds (not too far in future/past)
        val now = System.currentTimeMillis()
        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
        
        return time > 0 && time < (now + oneYearInMillis)
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
            errors.add("Invalid latitude: $latitude (must be between -90 and 90)")
        }
        
        if (longitude < -180 || longitude > 180) {
            errors.add("Invalid longitude: $longitude (must be between -180 and 180)")
        }
        
        if (latitude == 0f && longitude == 0f) {
            return ValidationResult.Warning("Location is at coordinates (0, 0)")
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
