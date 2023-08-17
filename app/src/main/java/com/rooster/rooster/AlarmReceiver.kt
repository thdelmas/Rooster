package com.rooster.rooster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import java.io.IOException

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmName = intent.getStringExtra("label")
        Log.w("Rooster", "Received alarm: $alarmName")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Rooster:Sunrise Alarm"
        )

        try {
            wakeLock.acquire(5000)

            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayKey = getDayKey(dayOfWeek) // Function to get the corresponding key for the current day

            val sharedPrefs = context.getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
            val isDayEnabled = sharedPrefs.getBoolean(dayKey, false)

            if (isDayEnabled) {
                val soundUri = Uri.parse("android.resource://${context.packageName}/raw/alarmclock")
                synchronized(this) {
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, soundUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        prepare()
                    }

                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.release()
                    }
                }
            } else {
                Log.w(TAG, "Alarm not enabled for today")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error playing sound", e)
        } finally {
            wakeLock.release()
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    private fun getDayKey(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> ""
        }
    }

}