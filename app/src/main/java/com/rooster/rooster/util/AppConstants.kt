package com.rooster.rooster.util

/**
 * Application-wide constants
 */
object AppConstants {
    // ========== Time Unit Conversions ==========
    const val MILLIS_PER_SECOND = 1000L
    const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
    const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
    const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR
    const val MILLIS_PER_YEAR = 365L * MILLIS_PER_DAY
    
    const val SECONDS_PER_MINUTE = 60
    const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE
    const val SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR
    
    // ========== Alarm Scheduling ==========
    const val ALARM_SNOOZE_DELAY_SECONDS = 30L
    const val ALARM_WAKE_LOCK_TIMEOUT_MS = 10 * MILLIS_PER_MINUTE // 10 minutes
    const val ALARM_GRADUAL_VOLUME_STEPS = 30
    const val ALARM_GRADUAL_VOLUME_DURATION_MS = 30 * MILLIS_PER_SECOND // 30 seconds
    
    // ========== Location Updates ==========
    const val LOCATION_UPDATE_INTERVAL_MS = 10 * MILLIS_PER_SECOND // 10 seconds
    const val LOCATION_MAX_AGE_MS = 30 * MILLIS_PER_MINUTE // 30 minutes
    const val LOCATION_UPDATE_DELAY_MS = 3 * MILLIS_PER_HOUR // 3 hours
    const val LOCATION_VALIDITY_MS = MILLIS_PER_DAY // 24 hours
    const val LOCATION_ACCEPT_MAX_AGE_MS = MILLIS_PER_HOUR // 1 hour
    
    // ========== Astronomy Data ==========
    const val ASTRONOMY_DATA_VALIDITY_MS = 6 * MILLIS_PER_HOUR // 6 hours
    const val ASTRONOMY_UPDATE_INTERVAL_MS = 23 * MILLIS_PER_HOUR // 23 hours
    const val ASTRONOMY_RETRY_DELAY_MS = 60 * MILLIS_PER_SECOND // 60 seconds
    
    // ========== UI Refresh ==========
    const val UI_REFRESH_INTERVAL_MS = MILLIS_PER_SECOND // 1 second
    
    // ========== Vibration Patterns ==========
    val VIBRATION_PATTERN = longArrayOf(0, MILLIS_PER_SECOND, MILLIS_PER_SECOND) // Vibrate 1s, pause 1s, repeat
    
    // ========== Default Values ==========
    const val DEFAULT_SNOOZE_DURATION_MINUTES = 10
    const val DEFAULT_SNOOZE_COUNT = 3
    const val DEFAULT_VOLUME = 80
    const val DEFAULT_ALARM_VOLUME_START = 0.1f
    
    // ========== Notification ==========
    const val NOTIFICATION_ID_ALARM = 1
    const val NOTIFICATION_CHANNEL_ALARM = "ALARM_CHANNEL"
    
    // ========== Intent Actions ==========
    const val ACTION_ALARM = "com.rooster.alarmmanager"
    const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    
    // ========== SharedPreferences Keys ==========
    const val PREFS_NAME = "rooster_prefs"
    const val PREFS_KEY_LATITUDE = "latitude"
    const val PREFS_KEY_LONGITUDE = "longitude"
    const val PREFS_KEY_ALTITUDE = "altitude"
    const val PREFS_KEY_LOCATION_UPDATE_TIME = "location_update_time"
    
    // ========== Ringtone ==========
    const val DEFAULT_RINGTONE_URI = "Default"
    const val RINGTONE_RESOURCE_PATH = "android.resource://"
    
    // ========== SeekBar Threshold ==========
    const val ALARM_DISMISS_THRESHOLD = 90
    
    // ========== Alarm Mode Strings ==========
    const val ALARM_MODE_AT = "At"
    const val ALARM_MODE_BEFORE = "Before"
    const val ALARM_MODE_AFTER = "After"
    const val ALARM_MODE_BETWEEN = "Between"
    
    // ========== Relative Time Strings ==========
    const val RELATIVE_TIME_PICK_TIME = "Pick Time"
    
    // ========== Solar Event Strings ==========
    const val SOLAR_EVENT_ASTRONOMICAL_DAWN = "Astronomical Dawn"
    const val SOLAR_EVENT_NAUTICAL_DAWN = "Nautical Dawn"
    const val SOLAR_EVENT_CIVIL_DAWN = "Civil Dawn"
    const val SOLAR_EVENT_SUNRISE = "Sunrise"
    const val SOLAR_EVENT_SOLAR_NOON = "Solar Noon"
    const val SOLAR_EVENT_SUNSET = "Sunset"
    const val SOLAR_EVENT_CIVIL_DUSK = "Civil Dusk"
    const val SOLAR_EVENT_NAUTICAL_DUSK = "Nautical Dusk"
    const val SOLAR_EVENT_ASTRONOMICAL_DUSK = "Astronomical Dusk"
    
    // ========== Validation ==========
    const val MAX_VALIDATION_TIME_MS = MILLIS_PER_YEAR // 1 year
}


