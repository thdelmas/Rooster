package com.rooster.rooster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Legacy AlarmHandler class for backward compatibility
 * Maintains the original simple implementation for existing code
 * 
 * For new code, prefer using ScheduleAlarmUseCase with dependency injection.
 */
class AlarmHandler {
    fun setAlarm(context: Context, alarm: Alarm) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = alarm.calculatedTime
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (am == null) {
            Log.e("AlarmHandler", "AlarmManager is null")
            return
        }

        val intent = Intent(context, AlarmclockReceiver::class.java).apply {
            putExtra("message", "alarm time")
            putExtra("alarm_id", alarm.id.toString())
            action = "com.rooster.alarmmanager"
        }
        
        val pi = PendingIntent.getBroadcast(
            context, 
            alarm.id.toInt(), // Use alarm ID for unique PendingIntent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = fullDateFormat.format(calendar.time)

        Log.d("AlarmHandler", "Setting alarm '${alarm.label}' (ID: ${alarm.id}) at $formattedDate")

        val triggerTime = calendar.timeInMillis
        
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                }
                else -> {
                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                }
            }
        } catch (e: SecurityException) {
            Log.e("AlarmHandler", "Permission denied for exact alarm", e)
        }
    }

    fun unsetAlarm(context: Context, alarm: Alarm) {
        unsetAlarmById(context, alarm.id)
    }

    fun unsetAlarmById(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (am == null) {
            Log.e("AlarmHandler", "AlarmManager is null")
            return
        }
        
        Log.d("AlarmHandler", "Unsetting alarm with ID: $id")
        
        val intent = Intent(context, AlarmclockReceiver::class.java).apply {
            putExtra("message", "alarm time")
            putExtra("alarm_id", id.toString())
            action = "com.rooster.alarmmanager"
        }
        
        val pi = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        am.cancel(pi)
        pi.cancel()
    }

    fun setNextAlarm(context: Context) {
        Log.i(TAG, "Setting Next Alarm")
        val alarmDbHelper = AlarmDbHelper(context)
        val alarms = alarmDbHelper.getAllAlarms()

        val currentTime = Calendar.getInstance()
        val currentMillis = currentTime.timeInMillis

        var closestAlarm: Alarm? = null
        var timeDifference: Long = Long.MAX_VALUE

        Log.i(TAG, "Current Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(currentTime.time)}")

        for (alarm in alarms) {
            if (!alarm.enabled) {
                Log.d(TAG, "Skipping disabled alarm: ${alarm.label}")
                continue
            }

            // Update the calculatedTime for each alarm
            alarmDbHelper.calculateTime(alarm)

            val alarmMillis = alarm.calculatedTime
            val diff = alarmMillis - currentMillis

            if (diff <= 0) {
                Log.d(TAG, "Alarm '${alarm.label}' is in the past, skipping")
                continue
            }

            Log.d(TAG, "Alarm '${alarm.label}' scheduled in ${diff / 1000 / 60} minutes")

            // Update closestAlarm if this alarm is closer than the previously found closest alarm
            if (diff < timeDifference) {
                closestAlarm = alarm
                timeDifference = diff
            }
        }

        closestAlarm?.let {
            val minutesUntil = timeDifference / 1000 / 60
            Log.i(TAG, "Closest Alarm Set: '${it.label}' in $minutesUntil minutes")
            setAlarm(context, it)
        } ?: Log.w(TAG, "No enabled alarms found")
    }


    fun dayOfWeek(index: Int): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return days[index % days.size] // Use modulo to safely wrap around the array
    }

}
