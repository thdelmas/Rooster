package com.rooster.rooster.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the three preferences that drive the "Smart" alarm mode:
 *   - target sleep duration (minutes)
 *   - mandatory wake-up time of day (minute of day, 0..1439), -1 = no cap
 *   - preferred solar anchor (one of AppConstants.SOLAR_EVENT_*), empty = no anchor
 */
object SmartWakePrefs {

    private const val KEY_TARGET_SLEEP_MINUTES = "smart_target_sleep_minutes"
    private const val KEY_MANDATORY_WAKE_MINUTE_OF_DAY = "smart_mandatory_wake_mod"
    private const val KEY_SOLAR_ANCHOR = "smart_solar_anchor"
    private const val KEY_ENABLED = "smart_wake_enabled"

    const val DEFAULT_TARGET_SLEEP_MINUTES = 8 * 60
    const val NO_MANDATORY_WAKE = -1
    const val NO_SOLAR_ANCHOR = ""

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    data class Snapshot(
        val targetSleepMinutes: Int,
        val mandatoryWakeMinuteOfDay: Int,
        val solarAnchor: String
    )

    fun get(prefs: SharedPreferences): Snapshot = Snapshot(
        targetSleepMinutes = prefs.getInt(KEY_TARGET_SLEEP_MINUTES, DEFAULT_TARGET_SLEEP_MINUTES),
        mandatoryWakeMinuteOfDay = prefs.getInt(KEY_MANDATORY_WAKE_MINUTE_OF_DAY, NO_MANDATORY_WAKE),
        solarAnchor = prefs.getString(KEY_SOLAR_ANCHOR, NO_SOLAR_ANCHOR) ?: NO_SOLAR_ANCHOR
    )

    fun setTargetSleepMinutes(prefs: SharedPreferences, minutes: Int) {
        prefs.edit().putInt(KEY_TARGET_SLEEP_MINUTES, minutes.coerceAtLeast(0)).apply()
    }

    fun setMandatoryWake(prefs: SharedPreferences, minuteOfDay: Int) {
        val sanitized = if (minuteOfDay in 0..1439) minuteOfDay else NO_MANDATORY_WAKE
        prefs.edit().putInt(KEY_MANDATORY_WAKE_MINUTE_OF_DAY, sanitized).apply()
    }

    fun setSolarAnchor(prefs: SharedPreferences, anchor: String) {
        prefs.edit().putString(KEY_SOLAR_ANCHOR, anchor).apply()
    }

    fun get(context: Context): Snapshot = get(prefsFor(context))
    fun setTargetSleepMinutes(context: Context, minutes: Int) =
        setTargetSleepMinutes(prefsFor(context), minutes)
    fun setMandatoryWake(context: Context, minuteOfDay: Int) =
        setMandatoryWake(prefsFor(context), minuteOfDay)
    fun setSolarAnchor(context: Context, anchor: String) =
        setSolarAnchor(prefsFor(context), anchor)
    fun isEnabled(context: Context): Boolean = isEnabled(prefsFor(context))
    fun setEnabled(context: Context, enabled: Boolean) =
        setEnabled(prefsFor(context), enabled)

    private fun prefsFor(context: Context): SharedPreferences =
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
}
