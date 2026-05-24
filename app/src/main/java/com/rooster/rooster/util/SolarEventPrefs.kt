package com.rooster.rooster.util

import android.content.Context

/**
 * On/off toggles for the solar-event cue. Sound and vibration are
 * independent: a user can leave only one enabled. Defaults are ON for both
 * — the cue is the headline feature this setting governs.
 *
 * The Zenith Sun Gong is a separate, opt-in cue: when enabled, the standard
 * noon bell is replaced by a deep gong strike. It can fire even with sound
 * disabled — a user can choose to hear only the gong at solar noon.
 */
object SolarEventPrefs {

    private const val PREFS_NAME = "rooster_prefs"
    private const val KEY_SOUND = "solar_event_sound_enabled"
    private const val KEY_VIBRATION = "solar_event_vibration_enabled"
    private const val KEY_ZENITH_GONG = "solar_event_zenith_gong_enabled"

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

    fun isZenithGongEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ZENITH_GONG, false)

    fun setZenithGongEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ZENITH_GONG, enabled).apply()
    }

    fun isAnyEnabled(context: Context): Boolean =
        isSoundEnabled(context) || isVibrationEnabled(context) || isZenithGongEnabled(context)
}
