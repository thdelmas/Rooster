package com.rooster.rooster.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

/**
 * Helper class for previewing alarm sounds with proper resource management
 */
class SoundPreviewHelper(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var isPlayingVar = false
    private var stopHandler: android.os.Handler? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    
    fun previewSound(ringtoneUri: String, durationMs: Long = 3000) {
        stopPreview() // Stop any existing preview
        
        Log.d("SoundPreviewHelper", "Previewing sound with URI: $ringtoneUri")
        
        val uri = when {
            ringtoneUri.isEmpty() || ringtoneUri.equals("default", ignoreCase = true) || ringtoneUri == "Default" -> {
                Log.d("SoundPreviewHelper", "Using default ringtone")
                null // Use default system ringtone
            }
            else -> {
                try {
                    val parsedUri = Uri.parse(ringtoneUri)
                    Log.d("SoundPreviewHelper", "Parsed URI: $parsedUri")
                    parsedUri
                } catch (e: Exception) {
                    Log.e("SoundPreviewHelper", "Error parsing URI: $ringtoneUri", e)
                    null
                }
            }
        }
        
        // Try using RingtoneManager first for system ringtones
        if (uri != null && isSystemRingtoneUri(uri)) {
            try {
                val rt = RingtoneManager.getRingtone(context, uri)
                if (rt != null) {
                    Log.d("SoundPreviewHelper", "Using RingtoneManager for system ringtone")
                    ringtone = rt
                    ringtone?.play()
                    isPlayingVar = true
                    
                    // Auto-stop after duration
                    stopHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    stopHandler?.postDelayed({
                        Log.d("SoundPreviewHelper", "Auto-stopping preview after $durationMs ms")
                        stopPreview()
                    }, durationMs)
                    return
                }
            } catch (e: Exception) {
                Log.w("SoundPreviewHelper", "Could not play with RingtoneManager, trying MediaPlayer", e)
            }
        }
        
        // Fall back to MediaPlayer for custom URIs or if RingtoneManager fails
        try {
            val actualUri = uri ?: Uri.parse("android.resource://${context.packageName}/raw/alarmclock")
            Log.d("SoundPreviewHelper", "Using MediaPlayer with URI: $actualUri")
            
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                
                try {
                    setDataSource(context, actualUri)
                    Log.d("SoundPreviewHelper", "DataSource set successfully")
                } catch (e: Exception) {
                    Log.e("SoundPreviewHelper", "Error setting data source with URI: $actualUri", e)
                    throw e
                }
                
                setVolume(0.5f, 0.5f) // Preview at 50% volume
                
                setOnPreparedListener { mp ->
                    try {
                        Log.d("SoundPreviewHelper", "MediaPlayer prepared, starting playback")
                        if (mp == mediaPlayer) { // Only start if this is still the current player
                            mp.start()
                            isPlayingVar = true
                            Log.d("SoundPreviewHelper", "Playback started")
                            
                            // Auto-stop after duration
                            stopHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            stopHandler?.postDelayed({
                                Log.d("SoundPreviewHelper", "Auto-stopping preview after $durationMs ms")
                                stopPreview()
                            }, durationMs)
                        } else {
                            Log.w("SoundPreviewHelper", "MediaPlayer reference changed, not starting")
                        }
                    } catch (e: Exception) {
                        Log.e("SoundPreviewHelper", "Error starting preview", e)
                        stopPreview()
                    }
                }
                
                setOnCompletionListener {
                    Log.d("SoundPreviewHelper", "Playback completed")
                    stopPreview()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("SoundPreviewHelper", "MediaPlayer error - What: $what, Extra: $extra")
                    stopPreview()
                    true
                }
                
                Log.d("SoundPreviewHelper", "Starting async preparation")
                prepareAsync()
            }
            
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("SoundPreviewHelper", "Error previewing sound", e)
            e.printStackTrace()
            stopPreview()
        }
    }
    
    /**
     * Check if URI is a system ringtone URI
     */
    private fun isSystemRingtoneUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        return scheme == "content" || scheme == "file" || scheme == null
    }
    
    fun stopPreview() {
        // Cancel any pending stop handlers
        stopHandler?.removeCallbacksAndMessages(null)
        stopHandler = null
        
        // Stop ringtone if playing
        ringtone?.let { rt ->
            try {
                if (rt.isPlaying) {
                    rt.stop()
                } else {
                    // Do nothing
                }
            } catch (e: Exception) {
                Log.w("SoundPreviewHelper", "Error stopping Ringtone", e)
            }
        }
        ringtone = null
        
        val player = mediaPlayer
        mediaPlayer = null // Clear reference first to prevent race conditions
        isPlayingVar = false
        
        if (player != null) {
            try {
                // Remove all listeners first to prevent callbacks during cleanup
                player.setOnPreparedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                
                // Stop if playing
                if (player.isPlaying) {
                    try {
                        player.stop()
                    } catch (e: Exception) {
                        Log.w("SoundPreviewHelper", "Error stopping MediaPlayer", e)
                    }
                }
                
                // Reset to clear state before release
                try {
                    player.reset()
                } catch (e: Exception) {
                    Log.w("SoundPreviewHelper", "Error resetting MediaPlayer", e)
                }
                
                // Release the player
                try {
                    player.release()
                } catch (e: Exception) {
                    Log.w("SoundPreviewHelper", "Error releasing MediaPlayer", e)
                }
            } catch (e: Exception) {
                Log.e("SoundPreviewHelper", "Error in MediaPlayer cleanup", e)
            }
        }
    }
    
    fun isPreviewPlaying(): Boolean {
        return isPlayingVar && (ringtone?.isPlaying == true || mediaPlayer?.isPlaying == true)
    }
    
    fun cleanup() {
        stopPreview()
    }
}


