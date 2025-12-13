package com.rooster.rooster.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmclockReceiver
import com.rooster.rooster.data.repository.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for scheduling alarms with the Android AlarmManager
 */
class ScheduleAlarmUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val calculateAlarmTimeUseCase: CalculateAlarmTimeUseCase
) {
    
    companion object {
        private const val TAG = "ScheduleAlarmUseCase"
    }
    
    /**
     * Schedule a single alarm
     */
    suspend fun scheduleAlarm(alarm: Alarm): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!alarm.enabled) {
                Log.w(TAG, "Alarm '${alarm.label}' (ID: ${alarm.id}) is disabled, skipping")
                return@withContext Result.failure(IllegalArgumentException("Alarm is disabled"))
            }
            
            // Calculate the alarm time
            val calculatedTime = calculateAlarmTimeUseCase.execute(alarm)
            
            // Validate the calculated time
            if (calculatedTime <= System.currentTimeMillis()) {
                Log.e(TAG, "Calculated time is in the past for alarm '${alarm.label}' (ID: ${alarm.id})")
                return@withContext Result.failure(IllegalStateException("Calculated time is in the past"))
            }
            
            // Update the alarm's calculated time in database
            val updatedAlarm = alarm.copy(calculatedTime = calculatedTime)
            alarmRepository.updateAlarm(updatedAlarm)
            
            // Schedule with AlarmManager
            scheduleWithAlarmManager(updatedAlarm)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm '${alarm.label}' (ID: ${alarm.id})", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a scheduled alarm
     */
    suspend fun cancelAlarm(alarm: Alarm): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            cancelWithAlarmManager(alarm.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm '${alarm.label}' (ID: ${alarm.id})", e)
            Result.failure(e)
        }
    }
    
    /**
     * Schedule the next enabled alarm
     * This finds all enabled alarms, calculates their times, and schedules the closest one
     */
    suspend fun scheduleNextAlarm(): Result<Alarm?> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Scheduling next alarm")
            
            val enabledAlarms = alarmRepository.getEnabledAlarms()
            
            if (enabledAlarms.isEmpty()) {
                Log.w(TAG, "No enabled alarms found")
                return@withContext Result.success(null)
            }
            
            val currentTime = System.currentTimeMillis()
            var closestAlarm: Alarm? = null
            var minTimeDifference = Long.MAX_VALUE
            
            for (alarm in enabledAlarms) {
                val calculatedTime = calculateAlarmTimeUseCase.execute(alarm)
                val diff = calculatedTime - currentTime
                
                if (diff <= 0) {
                    Log.d(TAG, "Alarm '${alarm.label}' (ID: ${alarm.id}) is in the past, skipping")
                    continue
                }
                
                Log.d(TAG, "Alarm '${alarm.label}' scheduled in ${diff / 1000 / 60} minutes")
                
                if (diff < minTimeDifference) {
                    closestAlarm = alarm.copy(calculatedTime = calculatedTime)
                    minTimeDifference = diff
                }
            }
            
            if (closestAlarm != null) {
                // Update the calculated time in database
                alarmRepository.updateCalculatedTime(closestAlarm.id, closestAlarm.calculatedTime)
                
                // Schedule with AlarmManager
                scheduleWithAlarmManager(closestAlarm)
                
                val minutesUntil = minTimeDifference / 1000 / 60
                Log.i(TAG, "Closest alarm set: '${closestAlarm.label}' (ID: ${closestAlarm.id}) in $minutesUntil minutes")
                
                Result.success(closestAlarm)
            } else {
                Log.w(TAG, "No valid alarms found to schedule")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next alarm", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reschedule all enabled alarms
     */
    suspend fun rescheduleAllAlarms(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Rescheduling all alarms")
            scheduleNextAlarm()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling all alarms", e)
            Result.failure(e)
        }
    }
    
    private fun scheduleWithAlarmManager(alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null")
            throw IllegalStateException("AlarmManager is not available")
        }
        
        val intent = Intent(context, AlarmclockReceiver::class.java).apply {
            putExtra("message", "alarm time")
            putExtra("alarm_id", alarm.id.toString())
            action = "com.rooster.alarmmanager"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = alarm.calculatedTime
        
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = fullDateFormat.format(calendar.time)
        
        Log.d(TAG, "Setting alarm '${alarm.label}' (ID: ${alarm.id}) at $formattedDate")
        
        val triggerTime = calendar.timeInMillis
        
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for exact alarm", e)
            throw e
        }
    }
    
    private fun cancelWithAlarmManager(alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null")
            throw IllegalStateException("AlarmManager is not available")
        }
        
        Log.d(TAG, "Cancelling alarm with ID: $alarmId")
        
        val intent = Intent(context, AlarmclockReceiver::class.java).apply {
            putExtra("message", "alarm time")
            putExtra("alarm_id", alarmId.toString())
            action = "com.rooster.alarmmanager"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
