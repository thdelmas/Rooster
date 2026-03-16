package com.rooster.rooster.util

import android.content.Context

object SleepProfileHelper {

    private const val PREFS_NAME = "rooster_prefs"
    private const val KEY_SLEEP_PROFILE = "sleep_profile"

    fun getSleepProfile(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SLEEP_PROFILE, AppConstants.SLEEP_PROFILE_NONE)
    }

    fun setSleepProfile(context: Context, profile: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SLEEP_PROFILE, profile).apply()
    }

    fun getProfileName(profile: Int): String {
        return when (profile) {
            AppConstants.SLEEP_PROFILE_EARLY_BIRD -> "Early Bird"
            AppConstants.SLEEP_PROFILE_NIGHT_OWL -> "Night Owl"
            else -> "None"
        }
    }
}
