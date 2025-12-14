package com.rooster.rooster

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.google.android.material.appbar.MaterialToolbar
import com.rooster.rooster.presentation.viewmodel.AlarmViewModel
import com.rooster.rooster.receiver.SnoozeReceiver
import com.rooster.rooster.util.AlarmNotificationHelper
import com.rooster.rooster.util.ErrorHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class AlarmActivity : FragmentActivity() {
    
    private val viewModel: AlarmViewModel by viewModels()
    private var alarmId: Long = 0
    private var alarmIsRunning = false
    private var isVibrating = false
    private var snoozeCount = 0
    private var maxSnoozeCount = 3
    private var currentVolume = 1.0f

    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarm: Alarm? = null
    private var refreshJob: Job? = null
    private var volumeIncreaseJob: Job? = null
    private var playbackRetryCount = 0
    private var playbackFailed = false
    private val maxRetryAttempts = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        alarmId = intent.getStringExtra("alarm_id")?.toLong() ?: -1 // Handle the null case
        if (alarmId <= 0) {
            Log.e("AlarmActivity", "Invalid alarm ID: $alarmId")
            finish()
            return
        }
        
        Log.i("AlarmActivity", "Alarm id: $alarmId")

        alarmIsRunning = true
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Load alarm from ViewModel (uses AlarmRepository)
        viewModel.loadAlarm(alarmId)
        
        // Observe alarm data from ViewModel
        viewModel.getAlarmLiveData(alarmId).observe(this) { alarm ->
            if (alarm != null && currentAlarm == null) {
                // Only initialize once when alarm is first loaded
                currentAlarm = alarm
                maxSnoozeCount = alarm.snoozeCount
                setupSnoozeButton()
                setupDismissButton()
                setupToolbar()
                alarmRing(alarm.ringtoneUri, alarm)
            } else if (alarm == null && currentAlarm == null) {
                // Alarm not found
                Log.e("AlarmActivity", "Alarm with ID $alarmId not found")
                finish()
            }
        }

        setupSeekBar()
        refreshCycle()
    }

    private fun setupSeekBar() {
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 90) {
                    stopAlarm(seekBar)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun setupSnoozeButton() {
        val snoozeButton = findViewById<Button>(R.id.snoozeButton)
        currentAlarm?.let { alarm ->
            if (alarm.snoozeEnabled && snoozeCount < maxSnoozeCount) {
                snoozeButton.visibility = View.VISIBLE
                snoozeButton.text = "Snooze (${snoozeCount + 1}/${maxSnoozeCount})"
                snoozeButton.setOnClickListener {
                    snoozeAlarm()
                }
            } else {
                snoozeButton.visibility = View.GONE
            }
        }
    }
    
    private fun setupDismissButton() {
        val dismissButton = findViewById<Button>(R.id.dismissButton)
        dismissButton.setOnClickListener {
            stopAlarm(dismissButton)
        }
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            stopAlarm(null)
        }
    }

    private fun alarmRing(ringtoneUri: String, alarm: Alarm) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { /* Handle focus change */ }
            .build()

        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            wakePhone()
            playRingtone(ringtoneUri, alarm)
        }
    }

    private fun wakePhone() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE, "rooster:wakelock"
            )
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error acquiring wake lock", e)
            // Continue without wake lock - screen should still turn on
        }
    }

    private fun playRingtone(ringtoneUri: String, alarm: Alarm) {
        // Reset retry state
        playbackRetryCount = 0
        playbackFailed = false
        
        // Handle vibration
        if (alarm.vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
            val pattern = longArrayOf(0, 1000, 1000) // Vibrate for 1s, pause 1s, repeat
            vibrator?.vibrate(pattern, 0) // 0 means repeat indefinitely
        }
        
        // Start playback with retry logic
        playRingtoneWithRetry(ringtoneUri, alarm, useDefault = false)
    }
    
    private fun playRingtoneWithRetry(ringtoneUri: String, alarm: Alarm, useDefault: Boolean) {
        if (playbackRetryCount >= maxRetryAttempts) {
            handlePlaybackFailure(alarm)
            return
        }
        
        val uri = if (useDefault) {
            Uri.parse("android.resource://$packageName/raw/alarmclock")
        } else {
            when {
                ringtoneUri.isEmpty() || ringtoneUri.equals("default", ignoreCase = true) || ringtoneUri == "Default" -> 
                    Uri.parse("android.resource://$packageName/raw/alarmclock")
                else -> {
                    try {
                        Uri.parse(ringtoneUri)
                    } catch (e: Exception) {
                        ErrorHandler.logWarning("AlarmActivity", "Invalid ringtone URI: $ringtoneUri, using default")
                        Uri.parse("android.resource://$packageName/raw/alarmclock")
                    }
                }
            }
        }

        try {
            // Calculate initial volume
            val targetVolume = alarm.volume / 100f
            currentVolume = if (alarm.gradualVolume) 0.1f else targetVolume
            
            // Release existing MediaPlayer if any
            mediaPlayer?.release()
            mediaPlayer = null
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                
                var dataSourceSet = false
                try {
                    setDataSource(applicationContext, uri)
                    dataSourceSet = true
                    Log.i("AlarmActivity", "Data source set successfully: $uri")
                } catch (e: Exception) {
                    ErrorHandler.logError("AlarmActivity", "Error setting data source with URI: $uri", e)
                    
                    // Fallback to default ringtone if not already using it
                    if (!useDefault) {
                        try {
                            val defaultUri = Uri.parse("android.resource://$packageName/raw/alarmclock")
                            setDataSource(applicationContext, defaultUri)
                            dataSourceSet = true
                            Log.i("AlarmActivity", "Using default ringtone as fallback")
                        } catch (fallbackException: Exception) {
                            ErrorHandler.logError("AlarmActivity", "Error setting default ringtone", fallbackException)
                            dataSourceSet = false
                        }
                    } else {
                        dataSourceSet = false
                    }
                }
                
                if (!dataSourceSet) {
                    release()
                    mediaPlayer = null
                    // Retry with exponential backoff
                    retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault)
                    return
                }
                
                setOnErrorListener { mp, what, extra ->
                    ErrorHandler.logError("AlarmActivity", "MediaPlayer error - What: $what, Extra: $extra", null)
                    
                    // Try to recover by using default ringtone if not already using it
                    if (!useDefault && playbackRetryCount < maxRetryAttempts) {
                        try {
                            mp?.reset()
                            val defaultUri = Uri.parse("android.resource://$packageName/raw/alarmclock")
                            mp?.setDataSource(applicationContext, defaultUri)
                            mp?.prepareAsync()
                            playbackRetryCount++
                            Log.i("AlarmActivity", "Attempting recovery with default ringtone (attempt $playbackRetryCount)")
                        } catch (e: Exception) {
                            ErrorHandler.logError("AlarmActivity", "Failed to recover from MediaPlayer error", e)
                            // Retry with exponential backoff
                            retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault = true)
                        }
                    } else {
                        // Retry with exponential backoff
                        retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault)
                    }
                    true // Error handled
                }
                
                setOnPreparedListener { mp ->
                    try {
                        Log.i("AlarmActivity", "MediaPlayer prepared, starting playback")
                        mp.setVolume(currentVolume, currentVolume)
                        mp.start()
                        
                        // Reset retry count on successful playback
                        playbackRetryCount = 0
                        playbackFailed = false
                        
                        // Start gradual volume increase if enabled
                        if (alarm.gradualVolume) {
                            startGradualVolumeIncrease(targetVolume)
                        }
                    } catch (e: Exception) {
                        ErrorHandler.logError("AlarmActivity", "Error starting MediaPlayer", e)
                        // Retry with exponential backoff
                        retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault)
                    }
                }
                
                setOnCompletionListener { mp ->
                    // If playback completes unexpectedly (shouldn't happen with looping), retry
                    if (alarmIsRunning && !playbackFailed) {
                        Log.w("AlarmActivity", "MediaPlayer completed unexpectedly, retrying")
                        retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault)
                    }
                }
                
                isLooping = true
                prepareAsync() // Use async preparation for better performance
            }
        } catch (e: Exception) {
            ErrorHandler.logError("AlarmActivity", "Error initializing MediaPlayer", e)
            // Ensure vibration continues even if audio fails
            if (alarm.vibrate && vibrator == null) {
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = longArrayOf(0, 1000, 1000)
                vibrator?.vibrate(pattern, 0)
            }
            // Retry with exponential backoff
            retryPlaybackWithBackoff(ringtoneUri, alarm, useDefault)
        }
    }
    
    private fun retryPlaybackWithBackoff(ringtoneUri: String, alarm: Alarm, useDefault: Boolean) {
        playbackRetryCount++
        
        if (playbackRetryCount >= maxRetryAttempts) {
            handlePlaybackFailure(alarm)
            return
        }
        
        // Exponential backoff: 1s, 2s, 4s
        val delayMs = (1000L * (1 shl (playbackRetryCount - 1))).coerceAtMost(4000L)
        
        Log.i("AlarmActivity", "Retrying playback in ${delayMs}ms (attempt $playbackRetryCount/$maxRetryAttempts)")
        
        lifecycleScope.launch {
            delay(delayMs)
            if (alarmIsRunning && !playbackFailed) {
                playRingtoneWithRetry(ringtoneUri, alarm, useDefault = useDefault || playbackRetryCount > 1)
            }
        }
    }
    
    private fun handlePlaybackFailure(alarm: Alarm) {
        playbackFailed = true
        ErrorHandler.logError("AlarmActivity", "Alarm playback failed after $maxRetryAttempts attempts", null)
        
        // Show notification to user
        try {
            val alarmTitle = alarm.label.ifEmpty { "Alarm" }
            AlarmNotificationHelper.showAlarmPlaybackErrorNotification(
                this,
                alarmId,
                alarmTitle
            )
        } catch (e: Exception) {
            ErrorHandler.logError("AlarmActivity", "Failed to show error notification", e)
        }
        
        // Ensure vibration continues even if audio fails
        if (alarm.vibrate && vibrator == null) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(pattern, 0)
        }
        
        Log.w("AlarmActivity", "Alarm will continue with vibration only")
    }
    
    private fun startGradualVolumeIncrease(targetVolume: Float) {
        // Cancel any existing volume increase job
        volumeIncreaseJob?.cancel()
        
        volumeIncreaseJob = lifecycleScope.launch {
            val steps = 30 // Increase over 30 seconds
            val increment = (targetVolume - currentVolume) / steps
            
            repeat(steps) {
                if (!isActive || !alarmIsRunning) return@launch
                if (currentVolume < targetVolume) {
                    currentVolume += increment
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                    delay(1000L)
                }
            }
            
            // Ensure we reach target volume
            if (isActive && alarmIsRunning) {
                currentVolume = targetVolume
                mediaPlayer?.setVolume(currentVolume, currentVolume)
            }
        }
    }
    
    private fun snoozeAlarm() {
        if (snoozeCount >= maxSnoozeCount) {
            Log.w("AlarmActivity", "Max snooze count reached: $snoozeCount/$maxSnoozeCount")
            return
        }
        
        snoozeCount++
        val snoozeDuration = currentAlarm?.snoozeDuration ?: 10
        
        Log.i("AlarmActivity", "Snoozing alarm for $snoozeDuration minutes (count: $snoozeCount/$maxSnoozeCount)")
        
        // Stop alarm temporarily
        alarmIsRunning = false
        releaseResources()
        
        // Send broadcast to SnoozeReceiver to handle snooze reliably
        // This uses AlarmManager, stores state in database, and survives app kills/reboots
        currentAlarm?.let { alarm ->
            try {
                val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
                    action = SnoozeReceiver.ACTION_SNOOZE
                    putExtra("alarm_id", alarmId)
                }
                
                sendBroadcast(snoozeIntent)
                Log.i("AlarmActivity", "Snooze broadcast sent for alarm $alarmId")
            } catch (e: Exception) {
                Log.e("AlarmActivity", "Error sending snooze broadcast", e)
            }
        }
        
        finish()
    }

    // Remaining methods including refreshCycle, getPercentageOfDay, stopAlarm, onResume, onPause, releaseResources...

    private fun releaseResources() {
        // Release MediaPlayer with proper error handling
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w("AlarmActivity", "Error stopping MediaPlayer", e)
                    }
                }
                try {
                    release()
                } catch (e: Exception) {
                    Log.w("AlarmActivity", "Error releasing MediaPlayer", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error in MediaPlayer cleanup", e)
        } finally {
            mediaPlayer = null
        }
        
        // Release WakeLock with proper error handling
        val lock = wakeLock
        if (lock != null) {
            try {
                if (lock.isHeld) {
                    lock.release()
                    Log.d("AlarmActivity", "WakeLock released")
                }
            } catch (e: Exception) {
                Log.e("AlarmActivity", "Error releasing WakeLock", e)
            }
        }
        wakeLock = null
        
        // Cancel vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w("AlarmActivity", "Error cancelling vibrator", e)
        } finally {
            vibrator = null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines to prevent memory leaks
        refreshJob?.cancel()
        volumeIncreaseJob?.cancel()
        refreshJob = null
        volumeIncreaseJob = null
        releaseResources()
        Log.i("AlarmActivity", "Activity destroyed")
    }

    fun stopAlarm(view: View?) {
        alarmIsRunning = false
        isVibrating = false
        
        // Dismiss the alarm through ViewModel (uses AlarmRepository)
        currentAlarm?.let { alarm ->
            viewModel.dismissAlarm(alarm)
        }
        
        releaseResources()
        finish()
    }
    
    private fun refreshCycle() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_cycle)
        val progressText = findViewById<TextView>(R.id.progress_text)

        // Cancel any existing refresh job
        refreshJob?.cancel()
        
        refreshJob = lifecycleScope.launch {
            while (alarmIsRunning && isActive) {
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val formattedTime = sdf.format(Date(currentTime))
                val percentage = getPercentageOfDay().toInt()
                
                progressText.text = formattedTime
                progressBar.progress = percentage
                
                delay(1000L)
            }
        }
    }

    fun getPercentageOfDay(): Float {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance()
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)

        val totalSeconds = ((now.timeInMillis - midnight.timeInMillis) / 1000).toFloat()
        val secondsInDay = 24 * 60 * 60
        val percentage = (totalSeconds / secondsInDay) * 100
        return percentage.toFloat()
    }
}