package com.rooster.rooster

import android.content.Context
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
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class AlarmActivity : FragmentActivity() {
    private var alarmId: Long = 0
    private var alarmIsRunning = false
    private var isVibrating = false

    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

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

        val alarm = alarmDbHelper.getAlarm(alarmId)
        if (alarm != null) {
            alarmRing(alarm.ringtoneUri)
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

    private fun alarmRing(ringtoneUri: String) {
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
            playRingtone(ringtoneUri)
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

    private fun playRingtone(ringtoneUri: String) {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator?.let {
            it.cancel()
            val uri = if (ringtoneUri == "default") Uri.parse("android.resource://${packageName}/raw/alarmclock") else Uri.parse(ringtoneUri)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                try {
                    setDataSource(applicationContext, uri)
                    prepare() // Prepare the MediaPlayer asynchronously
                    setOnErrorListener { _, what, extra ->
                        Log.e("MediaPlayer Error", "What: $what, Extra: $extra")
                        true
                    }
                    setOnPreparedListener { start() }
                    isLooping = true
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error setting data source", e)
                }
            }
        }
    }

    // Remaining methods including refreshCycle, getPercentageOfDay, stopAlarm, onResume, onPause, releaseResources...

    private fun releaseResources() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        vibrator?.cancel()
    }

    fun stopAlarm(view: View?) {
        alarmIsRunning = false
        isVibrating = false
        releaseResources()
        finish()
    }

    private fun refreshCycle() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_cycle)
        val progressText = findViewById<TextView>(R.id.progress_text)
        val handler = Handler()
        val delayMillis = 1000
        val maxProgress = 100

        val updateRunnable = object : Runnable {
            var i = 0

            override fun run() {
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("HH:mm")
                val formattedTime = sdf.format(Date(currentTime))
                val percentage = getPercentageOfDay().toLong()
                if (percentage <= maxProgress) {
                    progressText.text = formattedTime
                    progressBar.progress = percentage.toInt()
                    handler.postDelayed(this, delayMillis.toLong())
                } else {
                    // Reset progress bar and text
                    progressBar.progress = 0
                    progressText.text = "00:00"
                    // Start the loop again
                    handler.postDelayed(this, delayMillis.toLong())
                }
            }
        }

        handler.post(updateRunnable)
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