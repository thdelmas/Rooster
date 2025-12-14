package com.rooster.rooster.util

/**
 * Application-wide constants
 */
object AppConstants {
    // Alarm scheduling
    const val ALARM_SNOOZE_DELAY_SECONDS = 30L
    const val ALARM_WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes
    const val ALARM_GRADUAL_VOLUME_STEPS = 30
    const val ALARM_GRADUAL_VOLUME_DURATION_MS = 30 * 1000L // 30 seconds
    
    // Location updates
    const val LOCATION_UPDATE_INTERVAL_MS = 10000L // 10 seconds
    const val LOCATION_MAX_AGE_MS = 30 * 60 * 1000L // 30 minutes
    
    // UI refresh
    const val UI_REFRESH_INTERVAL_MS = 1000L // 1 second
    
    // Vibration patterns
    val VIBRATION_PATTERN = longArrayOf(0, 1000, 1000) // Vibrate 1s, pause 1s, repeat
    
    // Default values
    const val DEFAULT_SNOOZE_DURATION_MINUTES = 10
    const val DEFAULT_SNOOZE_COUNT = 3
    const val DEFAULT_VOLUME = 80
    const val DEFAULT_ALARM_VOLUME_START = 0.1f
    
    // Notification
    const val NOTIFICATION_ID_ALARM = 1
    const val NOTIFICATION_CHANNEL_ALARM = "ALARM_CHANNEL"
    
    // Intent actions
    const val ACTION_ALARM = "com.rooster.alarmmanager"
    const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    
    // SharedPreferences keys
    const val PREFS_NAME = "rooster_prefs"
    const val PREFS_KEY_LATITUDE = "latitude"
    const val PREFS_KEY_LONGITUDE = "longitude"
    const val PREFS_KEY_ALTITUDE = "altitude"
    const val PREFS_KEY_LOCATION_UPDATE_TIME = "location_update_time"
    
    // Ringtone
    const val DEFAULT_RINGTONE_URI = "Default"
    const val RINGTONE_RESOURCE_PATH = "android.resource://"
    
    // SeekBar threshold for alarm dismissal
    const val ALARM_DISMISS_THRESHOLD = 90
}

