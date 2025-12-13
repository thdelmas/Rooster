package com.rooster.rooster.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Helper class for previewing alarm sounds with proper resource management
 */
class SoundPreviewHelper(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingVar = false
    
    fun previewSound(ringtoneUri: String, durationMs: Long = 3000) {
        stopPreview() // Stop any existing preview
        
        val uri = when {
            ringtoneUri.isEmpty() || ringtoneUri.equals("default", ignoreCase = true) || ringtoneUri == "Default" -> 
                Uri.parse("android.resource://${context.packageName}/raw/alarmclock")
            else -> Uri.parse(ringtoneUri)
        }
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setVolume(0.5f, 0.5f) // Preview at 50% volume
                setOnPreparedListener {
                    start()
                    isPlayingVar = true
                    
                    // Auto-stop after duration
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        stopPreview()
                    }, durationMs)
                }
                setOnCompletionListener {
                    stopPreview()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("SoundPreviewHelper", "MediaPlayer error - What: $what, Extra: $extra")
                    stopPreview()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Error previewing sound", e)
            stopPreview()
        }
    }
    
    fun stopPreview() {
        try {
            mediaPlayer?.apply {
                if (isPlaying()) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Error stopping preview", e)
        } finally {
            mediaPlayer = null
            isPlayingVar = false
        }
    }
    
    fun isPreviewPlaying(): Boolean = isPlayingVar
    
    fun cleanup() {
        stopPreview()
    }
}


