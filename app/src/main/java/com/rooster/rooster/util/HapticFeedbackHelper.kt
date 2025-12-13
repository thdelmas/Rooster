package com.rooster.rooster.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for providing haptic feedback throughout the app
 */
object HapticFeedbackHelper {
    
    /**
     * Provides a light click haptic feedback
     */
    fun performLightClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Provides a standard click haptic feedback
     */
    fun performClick(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
    
    /**
     * Provides a heavy click haptic feedback for important actions
     */
    fun performHeavyClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Provides feedback for successful actions
     */
    fun performSuccessFeedback(context: Context) {
        vibratePattern(context, longArrayOf(0, 50, 50, 50))
    }
    
    /**
     * Provides feedback for delete/destructive actions
     */
    fun performDeleteFeedback(context: Context) {
        vibratePattern(context, longArrayOf(0, 100, 50, 100))
    }
    
    /**
     * Provides feedback for error actions
     */
    fun performErrorFeedback(context: Context) {
        vibratePattern(context, longArrayOf(0, 50, 100, 50, 100, 50))
    }
    
    /**
     * Provides feedback for toggle actions
     */
    fun performToggleFeedback(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Vibrates with a custom pattern
     */
    private fun vibratePattern(context: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
