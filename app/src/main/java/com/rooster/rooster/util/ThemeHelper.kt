package com.rooster.rooster.util

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Helper class for managing app theme (Light/Dark/Auto) and Material You dynamic colors
 */
object ThemeHelper {
    
    private const val PREFS_NAME = "rooster_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    
    // Theme modes
    const val THEME_AUTO = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    
    /**
     * Apply the saved theme to the app
     */
    fun applyTheme(context: Context) {
        val themeMode = getThemeMode(context)
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    /**
     * Get the current theme mode
     */
    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_AUTO)
    }
    
    /**
     * Set the theme mode
     */
    fun setThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(context)
    }
    
    /**
     * Get theme mode name for display
     */
    fun getThemeModeName(mode: Int): String {
        return when (mode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            else -> "Auto (System)"
        }
    }
    
    /**
     * Check if dynamic colors are enabled
     */
    fun isDynamicColorsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false // Dynamic colors only available on Android 12+
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DYNAMIC_COLORS, true) // Default to true on supported devices
    }
    
    /**
     * Set dynamic colors preference
     */
    fun setDynamicColorsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
    }
    
    /**
     * Check if device supports dynamic colors (Material You)
     */
    fun supportsDynamicColors(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * Check if app is currently in dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}












