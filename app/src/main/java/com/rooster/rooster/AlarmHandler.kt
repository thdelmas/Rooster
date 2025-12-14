package com.rooster.rooster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import com.rooster.rooster.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            Logger.e("AlarmHandler", "AlarmManager is null")
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

        Logger.d("AlarmHandler", "Setting alarm '${alarm.label}' (ID: ${alarm.id}) at $formattedDate")

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
            Logger.e("AlarmHandler", "Permission denied for exact alarm", e)
        }
    }

    fun unsetAlarm(context: Context, alarm: Alarm) {
        unsetAlarmById(context, alarm.id)
    }

    fun unsetAlarmById(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (am == null) {
            Logger.e("AlarmHandler", "AlarmManager is null")
            return
        }
        
        Logger.d("AlarmHandler", "Unsetting alarm with ID: $id")
        
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

    /**
     * @deprecated Use ScheduleAlarmUseCase.scheduleNextAlarm() instead.
     * This method is kept for backward compatibility but should not be used in new code.
     */
    @Deprecated("Use ScheduleAlarmUseCase.scheduleNextAlarm() instead.")
    fun setNextAlarm(context: Context) {
        Logger.w(TAG, "setNextAlarm() is deprecated. Use ScheduleAlarmUseCase.scheduleNextAlarm() instead.")
        
        // Try to use ScheduleAlarmUseCase if available
        val scheduleAlarmUseCase = (context.applicationContext as? RoosterApplication)
            ?.provideScheduleAlarmUseCase()
        
        if (scheduleAlarmUseCase != null) {
            // Use the modern approach with coroutines
            CoroutineScope(Dispatchers.IO).launch {
                val result = scheduleAlarmUseCase.scheduleNextAlarm()
                result.fold(
                    onSuccess = { alarm ->
                        if (alarm != null) {
                            Logger.i(TAG, "Next alarm scheduled successfully: ${alarm.label}")
                        } else {
                            Logger.i(TAG, "No enabled alarms to schedule")
                        }
                    },
                    onFailure = { e ->
                        Logger.e(TAG, "Error scheduling next alarm", e)
                        // Fallback to legacy implementation
                        setNextAlarmLegacy(context)
                    }
                )
            }
        } else {
            // Fallback to legacy implementation if Hilt is not available
            setNextAlarmLegacy(context)
        }
    }
    
    /**
     * Legacy implementation - should not be called in properly configured app
     * @deprecated This will be removed once all code is migrated to Room
     */
    @Deprecated("Use ScheduleAlarmUseCase instead")
    private fun setNextAlarmLegacy(context: Context) {
        Logger.e(TAG, "setNextAlarmLegacy() called - this should not happen in a properly configured app")
        Logger.e(TAG, "ScheduleAlarmUseCase should be available through Hilt. Falling back is not supported.")
        // This method should never be called if Hilt is properly configured
        // If it is called, it means ScheduleAlarmUseCase was not available, which is a configuration error
        // We cannot use AlarmDbHelper here as it's being removed
        // The best we can do is log an error
    }


    fun dayOfWeek(index: Int): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return days[index % days.size] // Use modulo to safely wrap around the array
    }

}
