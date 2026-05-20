package com.rooster.rooster.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the preferences that drive the "Smart" alarm mode:
 *   - target sleep duration (minutes) — informational; used only as a fallback
 *     when neither sunrise nor mandatory wake is available
 *   - mandatory wake-up time of day (minute of day, 0..1439), -1 = no cap
 *   - wake offset from sunrise (minutes, -60..+60). Always sunrise-anchored
 *     when Smart Wake is enabled; the offset shifts the wake before/after.
 */
object SmartWakePrefs {

    private const val KEY_TARGET_SLEEP_MINUTES = "smart_target_sleep_minutes"
    private const val KEY_MANDATORY_WAKE_MINUTE_OF_DAY = "smart_mandatory_wake_mod"
    private const val KEY_SUNRISE_OFFSET_MINUTES = "smart_sunrise_offset_minutes"
    private const val KEY_ENABLED = "smart_wake_enabled"

    const val DEFAULT_TARGET_SLEEP_MINUTES = 8 * 60
    const val NO_MANDATORY_WAKE = -1
    const val DEFAULT_SUNRISE_OFFSET_MINUTES = 0
    const val MIN_SUNRISE_OFFSET_MINUTES = -60
    const val MAX_SUNRISE_OFFSET_MINUTES = 60

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    data class Snapshot(
        val targetSleepMinutes: Int,
        val mandatoryWakeMinuteOfDay: Int,
        val sunriseOffsetMinutes: Int,
    )

    fun get(prefs: SharedPreferences): Snapshot = Snapshot(
        targetSleepMinutes = prefs.getInt(KEY_TARGET_SLEEP_MINUTES, DEFAULT_TARGET_SLEEP_MINUTES),
        mandatoryWakeMinuteOfDay = prefs.getInt(KEY_MANDATORY_WAKE_MINUTE_OF_DAY, NO_MANDATORY_WAKE),
        sunriseOffsetMinutes = prefs.getInt(KEY_SUNRISE_OFFSET_MINUTES, DEFAULT_SUNRISE_OFFSET_MINUTES),
    )

    fun setTargetSleepMinutes(prefs: SharedPreferences, minutes: Int) {
        prefs.edit().putInt(KEY_TARGET_SLEEP_MINUTES, minutes.coerceAtLeast(0)).apply()
    }

    fun setMandatoryWake(prefs: SharedPreferences, minuteOfDay: Int) {
        val sanitized = if (minuteOfDay in 0..1439) minuteOfDay else NO_MANDATORY_WAKE
        prefs.edit().putInt(KEY_MANDATORY_WAKE_MINUTE_OF_DAY, sanitized).apply()
    }

    fun setSunriseOffset(prefs: SharedPreferences, offsetMinutes: Int) {
        val clamped = offsetMinutes.coerceIn(MIN_SUNRISE_OFFSET_MINUTES, MAX_SUNRISE_OFFSET_MINUTES)
        prefs.edit().putInt(KEY_SUNRISE_OFFSET_MINUTES, clamped).apply()
    }

    fun get(context: Context): Snapshot = get(prefsFor(context))
    fun setTargetSleepMinutes(context: Context, minutes: Int) =
        setTargetSleepMinutes(prefsFor(context), minutes)
    fun setMandatoryWake(context: Context, minuteOfDay: Int) =
        setMandatoryWake(prefsFor(context), minuteOfDay)
    fun setSunriseOffset(context: Context, offsetMinutes: Int) =
        setSunriseOffset(prefsFor(context), offsetMinutes)
    fun isEnabled(context: Context): Boolean = isEnabled(prefsFor(context))
    fun setEnabled(context: Context, enabled: Boolean) =
        setEnabled(prefsFor(context), enabled)

    private fun prefsFor(context: Context): SharedPreferences =
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
}
