package com.rooster.rooster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager

class AlarmHandler {
    fun setAlarm(context: Context, alarm: Alarm) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = alarm.calculatedTime
        val am = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmclockReceiver::class.java)
        intent.putExtra("message", "alarm time")
        intent.putExtra("alarm_id", alarm.id.toString())
        intent.action = "com.rooster.alarmmanager"
        val pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val formattedDate = fullDateFormat.format(calendar.time)

        Log.d("SET INTENT", "Setting alarm at $formattedDate")

        var triggerTime = calendar.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        } else {
            // Remove 1 Minutes to ring on time
            triggerTime -= (60*1000)
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }
    }

    fun setNextIntent(context: Context?, alarmId: String) {
        val alarmDbHelper = context?.let { AlarmDbHelper(it) }
        Log.e("SetNextIntent", "Intent Alarm: $alarmId")
        var alarm = alarmDbHelper?.getAlarm(alarmId.toLong())
        if (alarm != null) {
            // Disable alarm if no repeats
            if (alarm.monday || alarm.tuesday || alarm.wednesday ||
                alarm.thursday || alarm.friday || alarm.saturday || alarm.sunday) {

            } else {
                alarm.enabled = false
                if (alarmDbHelper != null) {
                    alarmDbHelper.updateAlarm(alarm)
                }
            }
        }
    }




    fun unsetAlarm(context: Context, alarm: Alarm) {
        unsetAlarmById(context, alarm.id)
    }

    fun unsetAlarmById(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.e("Unset alarm", "Unset Alarm: " + id.toString())
        val intent = Intent(context, AlarmActivity::class.java)
        intent.putExtra("message", "alarm time")
        intent.putExtra("alarm_id", id.toString())
        intent.action = "com.rooster.alarmmanager"
        am.cancel(PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        ))
    }

    fun ring(context: Context) {
// Wake Phone
        val powerManager: PowerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION") val wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE or PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "rooster:wakelock")
        wakeLock.acquire()

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val gentlePattern = createWaveformVibrationPattern(longArrayOf(0, 1000, 2000, 3000), repeat = -1)

            var vibrationEffect1 = VibrationEffect.createWaveform(gentlePattern, 3)

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel()
            val soundUri = Uri.parse("android.resource://${context.packageName}/raw/alarmclock")
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
            // Start a loop that checks a SharedPreferences for a flag to stop the vibration
            Thread {
                vibrator.vibrate(vibrationEffect1)
                while (true) {
                    Log.w("Rooster Alarm", "Ring")
                    val sharedPreferences = context.getSharedPreferences("RoosterSharedPrefs", Context.MODE_PRIVATE)
                    val isAlarmStopped = sharedPreferences.getBoolean("StoppedAlarm", false)

                    if (isAlarmStopped) {
                        vibrator.cancel()
                        mediaPlayer.stop()
                        mediaPlayer.release()
                        wakeLock.release()
                        break
                    } else {
                        mediaPlayer.start()
                        Thread.sleep(500)
                        //vibrator.vibrate(vibrationEffect1)
                    }
                    Thread.sleep(1000)
                }
            }.start()
        }
    }

    private fun createWaveformVibrationPattern(timings: LongArray, repeat: Int = -1): LongArray {
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
}