package com.rooster.rooster.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Unique tactile signature paired with the Zenith Sun Gong at solar noon —
 * a three-step rise (low tap → mid pulse → long bright thrum) suggesting the
 * sun climbing to its apex. Fires alongside the gong so the user still feels
 * the moment when audio is suppressed (DND, silent, low volume).
 *
 * Distinct from [SolarEventVibration]'s tight "crown peal" which fires at
 * noon only when general vibration is on.
 */
object ZenithVibration {

    private val TIMINGS = longArrayOf(0, 120, 320, 220, 320, 420)
    private val AMPLITUDES = intArrayOf(0, 90, 0, 170, 0, 230)

    /** Total ms — used to keep the receiver alive while the pattern plays. */
    fun durationMs(): Long = TIMINGS.sum()

    fun vibrate(context: Context) {
        val vibrator = obtainVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(TIMINGS, AMPLITUDES, -1)
                } else {
                    VibrationEffect.createWaveform(TIMINGS, -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(TIMINGS, -1)
            }
        } catch (e: Exception) {
            Logger.e("ZenithVibration", "Failed to fire zenith vibration", e)
        }
    }

    private fun obtainVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
