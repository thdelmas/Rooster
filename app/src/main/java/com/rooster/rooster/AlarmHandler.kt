package com.rooster.rooster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
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
import java.util.Locale

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
            intent,PendingIntent.FLAG_MUTABLE
        ))
    }

    fun setNextAlarm(context: Context) {
        Log.i(TAG, "Setting Next Alarm")
        val alarmDbHelper = AlarmDbHelper(context)
        val alarms = alarmDbHelper.getAllAlarms()

        val currentTime = Calendar.getInstance()
        val currentMillis = currentTime.timeInMillis

        var closestAlarm: Alarm? = null
        var timeDifference: Long = Long.MAX_VALUE
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val today: String = dayFormat.format(currentTime.time)

        for (alarm in alarms) {
            if (alarm.enabled &&
                (alarm.getDayEnabled(today) ||
                        (!alarm.monday && !alarm.tuesday && !alarm.wednesday && !alarm.thursday && !alarm.friday && !alarm.saturday && !alarm.sunday))) {
                val alarmTime = Calendar.getInstance()
                alarmTime.timeInMillis = alarm.calculatedTime
                val alarmMillis = alarmTime.timeInMillis

                // Calculate the time difference between current time and alarm time
                val diff = alarmMillis - currentMillis

                // If the alarm time is in the future and closer than the previous closest alarm, update closestAlarm and timeDifference
                if (diff > 0 && diff < timeDifference) {
                    closestAlarm = alarm
                    timeDifference = diff
                }
            }
        }

        closestAlarm?.let {
            setAlarm(context, it)
        }
    }
}