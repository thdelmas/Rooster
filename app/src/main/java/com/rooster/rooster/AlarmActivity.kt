package com.rooster.rooster

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import java.util.Calendar
import java.util.Date
import kotlin.concurrent.fixedRateTimer

class AlarmActivity : FragmentActivity() {

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarm")
    }

  private var alarmIsRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("Alarm", "Activity Start")
        alarmIsRunning = true
        super.setShowWhenLocked(true)
        super.setTurnScreenOn(true)
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        setContentView(R.layout.activity_alarm)

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Check if the seek bar is at 95%
                if (progress >= 95) {
                    stopAlarm(seekBar)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        refreshCycle()
        alarmRing()
        //TODO Release wake lock
        val alarmHandler = AlarmHandler()
        alarmHandler.setNextAlarm(applicationContext)
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

    private fun alarmRing() {
        // Wake Phone
        val powerManager: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION") val wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE or PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "rooster:wakelock")
        wakeLock.acquire()

        val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val gentlePattern = createWaveformVibrationPattern(longArrayOf(0, 1000, 2000, 3000), repeat = -1)

            var vibrationEffect1 = VibrationEffect.createWaveform(gentlePattern, 3)

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel()
            val soundUri = Uri.parse("android.resource://${applicationContext.packageName}/raw/alarmclock")
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepare()
            }
            // Start a loop that checks a SharedPreferences for a flag to stop the vibration
            Thread {
                vibrator.vibrate(vibrationEffect1)
                while (true) {
                    Log.w("Rooster Alarm", "Ring")
                    if (alarmIsRunning) {
                        mediaPlayer.start()
                        Thread.sleep(500)
                    } else {
                        vibrator.cancel()
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        wakeLock.release()
                        break
                    }
                    Thread.sleep(1000)
                }
            }.start()
        }
    }

    fun createWaveformVibrationPattern(timings: LongArray, repeat: Int = -1): LongArray {
        var pattern = LongArray(timings.size + 1)
        pattern[0] = 0L
        for (i in 1 until pattern.size) {
            pattern[i] = pattern[i - 1] + timings[i - 1]
        }

        if (repeat != -1) {
            val repeatIndex = pattern[repeat]
            val repeatPattern = pattern.copyOfRange(repeatIndex.toInt(), pattern.size)
            pattern = pattern.copyOfRange(0, repeatIndex.toInt()) + repeatPattern
        }

        return pattern
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

    fun stopAlarm(view: View) {
         val mediaPlayer = MediaPlayer()
         val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        alarmIsRunning = false
        wakeLock.release()
        mediaPlayer.stop()
        vibrator.cancel()
        supportFragmentManager.popBackStackImmediate()
        finish()
    }

    override fun onResume() {
        super.onResume()
        wakeLock.acquire()

        if (!alarmIsRunning) {
            //alarmIsRunning = true
        }
    }

    override fun onPause() {
         val mediaPlayer = MediaPlayer()
         val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        super.onPause()
        alarmIsRunning = false
        mediaPlayer.stop()
        vibrator.cancel()
    }
}