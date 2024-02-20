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

        Log.i(TAG, "Current Time: $currentTime")

        for (alarm in alarms) {
            if (!alarm.enabled) continue // Skip disabled alarms

            // Update the calculatedTime for each alarm
            alarmDbHelper.calculateTime(alarm) // Assuming this updates alarm.calculatedTime and logs the time

            val alarmTime = Calendar.getInstance()
            alarmTime.timeInMillis = alarm.calculatedTime
            val alarmMillis = alarm.calculatedTime

            // Calculate the difference between current time and the alarm time
            var diff = alarmMillis - currentMillis

            if (diff < 0) {
                // If the calculated time is in the past, calculate for the next occurrence
                continue // Or adjust logic to calculate for the next valid day
            }

            // Update closestAlarm if this alarm is closer than the previously found closest alarm
            if (diff < timeDifference) {
                closestAlarm = alarm
                timeDifference = diff
            }
        }

        closestAlarm?.let {
            Log.i(TAG, "Closest Alarm Set: ${it.label}")
            setAlarm(context, it) // Assuming setAlarm is a method to actually set the alarm
        }
    }


    fun dayOfWeek(index: Int): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return days[index % days.size] // Use modulo to safely wrap around the array
    }

}