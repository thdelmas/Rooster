package com.rooster.rooster.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Singing-bowl-style cues for solar events. Each event has its own short
 * multi-note arpeggio (the "signature") synthesized as struck-bell tones —
 * fundamental + octave + octave-fifth + two-octaves, each partial decaying
 * faster than the one below. Same voice family as the W2F BreathingTones
 * player, so cues feel meditative rather than alarming.
 *
 * Pitches are picked from a C-pentatonic-with-octaves cluster so any
 * combination is consonant. Dawn signatures rise; dusk signatures fall;
 * sunrise/sunset are 3-note peals; solar noon is a tight high peal at the
 * apex; astro events use the deepest pitches at the edges of the day.
 */
object SolarEventTone {

    // C major pentatonic spread across two octaves: C3 → C5
    private const val C3 = 130.81
    private const val G3 = 196.00
    private const val C4 = 261.63
    private const val E4 = 329.63
    private const val G4 = 392.00
    private const val A4 = 440.00
    private const val C5 = 523.25

    private const val SAMPLE_RATE = 44100
    private const val ATTACK_SEC = 0.015
    private const val GAIN = 0.16f
    private const val NOTE_DURATION_SEC = 1.4
    private const val NOTE_GAP_MS = 220L

    /**
     * Each event has a distinct ascending/descending pattern so the user can
     * identify which solar event fired by ear alone. Arrays are short — 2-3
     * notes — to keep the cue minimalist (~3-4 seconds total).
     */
    private val signatures: Map<String, DoubleArray> = mapOf(
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN to doubleArrayOf(C3, G3),       // deep, two-step rise from rest
        AppConstants.SOLAR_EVENT_NAUTICAL_DAWN     to doubleArrayOf(G3, C4),        // continuing the climb
        AppConstants.SOLAR_EVENT_CIVIL_DAWN        to doubleArrayOf(C4, E4, G4),    // bright triad rising
        AppConstants.SOLAR_EVENT_SUNRISE           to doubleArrayOf(C4, G4, C5),    // wide ascending peal
        AppConstants.SOLAR_EVENT_SOLAR_NOON        to doubleArrayOf(C5, G4, C5),    // crown-bell at the apex
        AppConstants.SOLAR_EVENT_SUNSET            to doubleArrayOf(C5, G4, C4),    // descending mirror of sunrise
        AppConstants.SOLAR_EVENT_CIVIL_DUSK        to doubleArrayOf(G4, E4, C4),    // soft triad falling
        AppConstants.SOLAR_EVENT_NAUTICAL_DUSK     to doubleArrayOf(C4, G3),        // settling toward night
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK to doubleArrayOf(G3, C3)         // deepest, day fully closed
    )

    /** Total time (ms) to allow the receiver to stay alive while the cue plays. */
    fun durationMsFor(event: String?): Long {
        val notes = signatures[event] ?: return 0L
        val perNoteMs = (NOTE_DURATION_SEC * 1000).toLong()
        return notes.size * perNoteMs + (notes.size - 1).coerceAtLeast(0) * NOTE_GAP_MS + 200L
    }

    /**
     * Synthesize and play the signature for [event]. Blocks the calling
     * thread for the full cue duration (intended to be called from a worker
     * thread inside a BroadcastReceiver's goAsync()).
     */
    fun play(event: String?) {
        val notes = signatures[event] ?: return
        for ((idx, hz) in notes.withIndex()) {
            playStruck(hz)
            if (idx < notes.lastIndex) {
                Thread.sleep(NOTE_GAP_MS)
            }
        }
    }

    private fun playStruck(fundamentalHz: Double) {
        val durationMs = (NOTE_DURATION_SEC * 1000).toInt()
        val track = buildBellTrack(fundamentalHz, durationMs) ?: return
        try {
            track.play()
            // Block until the note has effectively rung out.
            Thread.sleep(durationMs.toLong())
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private fun buildBellTrack(fundamentalHz: Double, durationMs: Int): AudioTrack? {
        val totalSamples = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)
        val attackSamples = (SAMPLE_RATE * ATTACK_SEC).toInt().coerceAtLeast(1)
        val durationSec = durationMs / 1000.0

        // Higher partials decay faster — physical realism for a struck bowl.
        val partials = listOf(
            Partial(ratio = 1.0, amp = 1.00, tau = durationSec / 3.0),
            Partial(ratio = 2.0, amp = 0.45, tau = durationSec / 5.0),
            Partial(ratio = 3.0, amp = 0.22, tau = durationSec / 7.0),
            Partial(ratio = 4.0, amp = 0.10, tau = durationSec / 10.0)
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0
            for (p in partials) {
                sample += p.amp * exp(-t / p.tau) *
                    sin(2 * PI * fundamentalHz * p.ratio * t)
            }
            val attack = if (i < attackSamples) i.toFloat() / attackSamples else 1f
            buffer[i] = (sample * attack * GAIN * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        val byteSize = buffer.size * 2
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also { it.write(buffer, 0, buffer.size) }
        }.getOrNull()
    }

    private data class Partial(val ratio: Double, val amp: Double, val tau: Double)
}
