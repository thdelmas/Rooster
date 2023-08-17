package com.rooster.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    wakeLock.release()
                }
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
}