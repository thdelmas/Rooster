package com.rooster.rooster.util

import android.content.Context

/**
 * On/off toggles for the solar-event cue. Sound and vibration are
 * independent: a user can leave only one enabled. Defaults are ON for both
 * — the cue is the headline feature this setting governs.
 */
object SolarEventPrefs {

    private const val PREFS_NAME = "rooster_prefs"
    private const val KEY_SOUND = "solar_event_sound_enabled"
    private const val KEY_VIBRATION = "solar_event_vibration_enabled"

    fun isSoundEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true)

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isAnyEnabled(context: Context): Boolean =
        isSoundEnabled(context) || isVibrationEnabled(context)
}
