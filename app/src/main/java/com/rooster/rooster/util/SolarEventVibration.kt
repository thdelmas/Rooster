package com.rooster.rooster.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Per-event vibration signatures matching the audio cue rhythm. Patterns
 * mirror the tonal arpeggios in [SolarEventTone]: rising counts at dawn,
 * falling counts at dusk, brief peals at sunrise/sunset/noon. A user with
 * the phone face-down can still tell which event fired by touch alone.
 *
 * Pattern layout: [delay, on, off, on, ...]. Amplitudes (where supported)
 * are dim at twilight edges and bright at sunrise/noon/sunset.
 */
object SolarEventVibration {

    // Short = ~80ms tap, Medium = ~180ms pulse, Long = ~350ms thrum.
    private const val SHORT = 80L
    private const val MEDIUM = 180L
    private const val LONG = 350L
    private const val GAP_TIGHT = 120L
    private const val GAP_OPEN = 240L

    // Low/Mid/High amplitudes (out of 255). Twilight = low, day = high.
    private const val AMP_LOW = 80
    private const val AMP_MID = 160
    private const val AMP_HIGH = 230

    private data class Signature(val timings: LongArray, val amplitudes: IntArray) {
        init {
            require(timings.size == amplitudes.size) { "timings and amplitudes must align" }
        }
    }

    private val signatures: Map<String, Signature> = mapOf(
        // Astro Dawn: one slow deep thrum (the world is still dark)
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN to Signature(
            timings = longArrayOf(0, LONG),
            amplitudes = intArrayOf(0, AMP_LOW)
        ),
        // Nautical Dawn: two soft pulses rising
        AppConstants.SOLAR_EVENT_NAUTICAL_DAWN to Signature(
            timings = longArrayOf(0, MEDIUM, GAP_OPEN, MEDIUM),
            amplitudes = intArrayOf(0, AMP_LOW, 0, AMP_MID)
        ),
        // Civil Dawn: three pulses, climbing
        AppConstants.SOLAR_EVENT_CIVIL_DAWN to Signature(
            timings = longArrayOf(0, SHORT, GAP_TIGHT, MEDIUM, GAP_TIGHT, MEDIUM),
            amplitudes = intArrayOf(0, AMP_LOW, 0, AMP_MID, 0, AMP_HIGH)
        ),
        // Sunrise: a bright triple peal (the moment)
        AppConstants.SOLAR_EVENT_SUNRISE to Signature(
            timings = longArrayOf(0, MEDIUM, GAP_TIGHT, MEDIUM, GAP_TIGHT, LONG),
            amplitudes = intArrayOf(0, AMP_HIGH, 0, AMP_HIGH, 0, AMP_HIGH)
        ),
        // Solar Noon: tight high-low-high crown peal at the apex
        AppConstants.SOLAR_EVENT_SOLAR_NOON to Signature(
            timings = longArrayOf(0, SHORT, GAP_TIGHT, SHORT, GAP_TIGHT, SHORT),
            amplitudes = intArrayOf(0, AMP_HIGH, 0, AMP_MID, 0, AMP_HIGH)
        ),
        // Sunset: triple peal mirroring sunrise but warmer/longer at the end
        AppConstants.SOLAR_EVENT_SUNSET to Signature(
            timings = longArrayOf(0, LONG, GAP_TIGHT, MEDIUM, GAP_TIGHT, MEDIUM),
            amplitudes = intArrayOf(0, AMP_HIGH, 0, AMP_HIGH, 0, AMP_HIGH)
        ),
        // Civil Dusk: three pulses, descending
        AppConstants.SOLAR_EVENT_CIVIL_DUSK to Signature(
            timings = longArrayOf(0, MEDIUM, GAP_TIGHT, MEDIUM, GAP_TIGHT, SHORT),
            amplitudes = intArrayOf(0, AMP_HIGH, 0, AMP_MID, 0, AMP_LOW)
        ),
        // Nautical Dusk: two soft pulses fading
        AppConstants.SOLAR_EVENT_NAUTICAL_DUSK to Signature(
            timings = longArrayOf(0, MEDIUM, GAP_OPEN, MEDIUM),
            amplitudes = intArrayOf(0, AMP_MID, 0, AMP_LOW)
        ),
        // Astro Dusk: one slow deep thrum (mirrors astro dawn)
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK to Signature(
            timings = longArrayOf(0, LONG),
            amplitudes = intArrayOf(0, AMP_LOW)
        )
    )

    /** Total ms the cue takes — used to keep the receiver alive. */
    fun durationMsFor(event: String?): Long {
        val sig = signatures[event] ?: return 0L
        return sig.timings.sum()
    }

    fun vibrate(context: Context, event: String?) {
        val sig = signatures[event] ?: return
        val vibrator = obtainVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(sig.timings, sig.amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(sig.timings, -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(sig.timings, -1)
            }
        } catch (e: Exception) {
            Logger.e("SolarEventVibration", "Failed to vibrate for $event", e)
        }
    }

    private fun obtainVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
