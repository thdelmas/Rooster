package com.rooster.rooster.util

import android.content.Context

/**
 * Maps each solar event to a thematic, non-stressful sound bundled in res/raw/.
 *
 * Resource lookup is done at runtime via getIdentifier(), so the code compiles
 * even when a themed audio file is missing — the missing theme falls back to
 * the generic alarm tone instead of failing to build.
 */
object SolarEventSoundMapper {

    data class Theme(
        val event: String,
        val rawResourceName: String,
        val displayName: String,
        val emoji: String
    )

    private val themes: List<Theme> = listOf(
        Theme(AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN, "gentle_chime",   "Gentle Chime",   "🌄"),
        Theme(AppConstants.SOLAR_EVENT_NAUTICAL_DAWN,     "ocean_waves",    "Ocean Waves",    "🌅"),
        Theme(AppConstants.SOLAR_EVENT_CIVIL_DAWN,        "morning_birds",  "Morning Birds",  "🌆"),
        Theme(AppConstants.SOLAR_EVENT_SUNRISE,           "rooster_crow",   "Rooster Crow",   "🌅"),
        Theme(AppConstants.SOLAR_EVENT_SOLAR_NOON,        "wind_chime",     "Wind Chime",     "☀️"),
        Theme(AppConstants.SOLAR_EVENT_SUNSET,            "evening_bell",   "Evening Bell",   "🌇"),
        Theme(AppConstants.SOLAR_EVENT_CIVIL_DUSK,        "cricket_song",   "Cricket Song",   "🌆"),
        Theme(AppConstants.SOLAR_EVENT_NAUTICAL_DUSK,     "night_breeze",   "Night Breeze",   "🌃"),
        Theme(AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK, "owl_hoot",       "Owl Hoot",       "🌌")
    )

    private val themesByEvent: Map<String, Theme> = themes.associateBy { it.event }
    private val themesByResource: Map<String, Theme> = themes.associateBy { it.rawResourceName }

    fun all(): List<Theme> = themes

    /** Themes whose audio file is actually bundled in res/raw/. */
    fun bundledThemes(context: Context): List<Theme> = themes.filter {
        context.resources.getIdentifier(it.rawResourceName, "raw", context.packageName) != 0
    }

    fun themeFor(event: String?): Theme? = event?.trim()?.let { themesByEvent[it] }

    /**
     * URI for the themed sound, or the default alarm URI if no theme exists for
     * this event or the audio file hasn't been bundled yet.
     */
    fun uriFor(context: Context, event: String?): String {
        val theme = themeFor(event) ?: return defaultUri(context)
        val resId = context.resources.getIdentifier(theme.rawResourceName, "raw", context.packageName)
        return if (resId != 0) {
            "${AppConstants.RINGTONE_RESOURCE_PATH}${context.packageName}/raw/${theme.rawResourceName}"
        } else {
            defaultUri(context)
        }
    }

    /** True if the URI points at one of our bundled solar-themed sounds. */
    fun isThemedUri(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return themes.any { uri.endsWith("/raw/${it.rawResourceName}") }
    }

    /**
     * Whether the alarm's current ringtone should be auto-updated when the
     * user changes the solar event. True for the literal "Default" URI and for
     * any of our themed URIs (so picking Sunset after Sunrise swaps the tone).
     */
    fun shouldAutoRetheme(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return true
        if (uri.equals(AppConstants.DEFAULT_RINGTONE_URI, ignoreCase = true)) return true
        return isThemedUri(uri)
    }

    fun displayNameForUri(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        return themes.firstOrNull { uri.endsWith("/raw/${it.rawResourceName}") }?.displayName
    }

    private fun defaultUri(context: Context): String =
        "${AppConstants.RINGTONE_RESOURCE_PATH}${context.packageName}/raw/rooster_crow"
}
