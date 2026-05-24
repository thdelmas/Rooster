package com.rooster.rooster.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Single deep gong strike for solar noon. Distinct from [SolarEventTone]'s
 * struck-bowl bells: lower fundamental (~A2), inharmonic partials, slow
 * shimmering attack, and a long ~6-second decay. The non-integer partial
 * ratios produce the metallic beating character of a real bronze gong.
 */
object ZenithGongTone {

    private const val FUNDAMENTAL_HZ = 110.0
    private const val SAMPLE_RATE = 44100
    private const val DURATION_SEC = 6.0
    private const val ATTACK_SEC = 0.12
    private const val GAIN = 0.22f

    private val partials = listOf(
        Partial(ratio = 1.000, amp = 1.00, tau = 2.8),
        Partial(ratio = 1.498, amp = 0.55, tau = 2.2),
        Partial(ratio = 2.276, amp = 0.42, tau = 1.7),
        Partial(ratio = 3.012, amp = 0.30, tau = 1.3),
        Partial(ratio = 4.413, amp = 0.18, tau = 0.9),
        Partial(ratio = 5.802, amp = 0.10, tau = 0.6)
    )

    /** Total ms to allow the receiver to stay alive while the gong rings out. */
    fun durationMs(): Long = (DURATION_SEC * 1000).toLong() + 200L

    /**
     * Synthesize and play one gong strike. Blocks the calling thread for the
     * full duration — call from a worker thread inside goAsync().
     */
    fun play() {
        val durationMs = (DURATION_SEC * 1000).toInt()
        val track = buildGongTrack(durationMs) ?: return
        try {
            track.play()
            Thread.sleep(durationMs.toLong())
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private fun buildGongTrack(durationMs: Int): AudioTrack? {
        val totalSamples = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)
        val attackSamples = (SAMPLE_RATE * ATTACK_SEC).toInt().coerceAtLeast(1)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0
            for (p in partials) {
                sample += p.amp * exp(-t / p.tau) *
                    sin(2 * PI * FUNDAMENTAL_HZ * p.ratio * t)
            }
            // Slow shimmer attack — the strike "opens up" instead of cracking.
            val attack = if (i < attackSamples) {
                val x = i.toDouble() / attackSamples
                x * x
            } else {
                1.0
            }
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
