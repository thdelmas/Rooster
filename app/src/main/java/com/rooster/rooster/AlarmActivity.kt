package com.rooster.rooster

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import com.rooster.rooster.presentation.viewmodel.AlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    // Assume AlarmDbHelper is a helper class for database operations
    private lateinit var alarmDbHelper: AlarmDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Initialize AlarmDbHelper
        alarmDbHelper = AlarmDbHelper(this)

        alarmId = intent.getStringExtra("alarm_id")?.toLong() ?: -1 // Handle the null case
        Log.e("AlarmActivity", "Alarm id: $alarmId")

        alarmIsRunning = true
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        currentAlarm = alarmDbHelper.getAlarm(alarmId)
        if (currentAlarm != null) {
            maxSnoozeCount = currentAlarm!!.snoozeCount
            setupSnoozeButton()
            alarmRing(currentAlarm!!.ringtoneUri, currentAlarm!!)
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
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "rooster:wakelock"
        ).apply { acquire(10 * 60 * 1000L /*10 minutes*/) }
    }

    private fun playRingtone(ringtoneUri: String, alarm: Alarm) {
        // Handle vibration
        if (alarm.vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
            val pattern = longArrayOf(0, 1000, 1000) // Vibrate for 1s, pause 1s, repeat
            vibrator?.vibrate(pattern, 0) // 0 means repeat indefinitely
        }
        
        val uri = when {
            ringtoneUri.isEmpty() || ringtoneUri.equals("default", ignoreCase = true) || ringtoneUri == "Default" -> 
                Uri.parse("android.resource://$packageName/raw/alarmclock")
            else -> Uri.parse(ringtoneUri)
        }

        try {
            // Calculate initial volume
            val targetVolume = alarm.volume / 100f
            currentVolume = if (alarm.gradualVolume) 0.1f else targetVolume
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(applicationContext, uri)
                setOnErrorListener { _, what, extra ->
                    Log.e("AlarmActivity", "MediaPlayer error - What: $what, Extra: $extra")
                    true
                }
                setOnPreparedListener { 
                    Log.i("AlarmActivity", "MediaPlayer prepared, starting playback")
                    setVolume(currentVolume, currentVolume)
                    start()
                    
                    // Start gradual volume increase if enabled
                    if (alarm.gradualVolume) {
                        startGradualVolumeIncrease(targetVolume)
                    }
                }
                isLooping = true
                prepareAsync() // Use async preparation for better performance
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error initializing MediaPlayer", e)
        }
    }
    
    private fun startGradualVolumeIncrease(targetVolume: Float) {
        CoroutineScope(Dispatchers.Main).launch {
            val steps = 30 // Increase over 30 seconds
            val increment = (targetVolume - currentVolume) / steps
            
            repeat(steps) {
                if (alarmIsRunning && currentVolume < targetVolume) {
                    currentVolume += increment
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                    delay(1000L)
                }
            }
            
            // Ensure we reach target volume
            if (alarmIsRunning) {
                currentVolume = targetVolume
                mediaPlayer?.setVolume(currentVolume, currentVolume)
            }
        }
    }
    
    private fun snoozeAlarm() {
        if (snoozeCount >= maxSnoozeCount) {
            return
        }
        
        snoozeCount++
        val snoozeDuration = currentAlarm?.snoozeDuration ?: 10
        
        Log.i("AlarmActivity", "Snoozing alarm for $snoozeDuration minutes (count: $snoozeCount/$maxSnoozeCount)")
        
        // Stop alarm temporarily
        alarmIsRunning = false
        releaseResources()
        
        // Schedule snooze
        currentAlarm?.let { alarm ->
            val handler = Handler(mainLooper)
            handler.postDelayed({
                // Re-trigger alarm after snooze
                val intent = Intent(this, AlarmActivity::class.java)
                intent.putExtra("alarm_id", alarmId.toString())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }, snoozeDuration * 60 * 1000L)
        }
        
        finish()
    }

    // Remaining methods including refreshCycle, getPercentageOfDay, stopAlarm, onResume, onPause, releaseResources...

    private fun releaseResources() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
        
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
        
        vibrator?.cancel()
        vibrator = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
        Log.i("AlarmActivity", "Activity destroyed")
    }

    fun stopAlarm(view: View?) {
        alarmIsRunning = false
        isVibrating = false
        
        // Dismiss the alarm through ViewModel
        val alarm = alarmDbHelper.getAlarm(alarmId)
        if (alarm != null) {
            viewModel.dismissAlarm(alarm)
        }
        
        releaseResources()
        finish()
    }
    
    private fun refreshCycle() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_cycle)
        val progressText = findViewById<TextView>(R.id.progress_text)

        CoroutineScope(Dispatchers.Main).launch {
            while (alarmIsRunning) {
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