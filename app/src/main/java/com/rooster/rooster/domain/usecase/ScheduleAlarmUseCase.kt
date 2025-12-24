package com.rooster.rooster.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import com.rooster.rooster.Alarm
import com.rooster.rooster.util.Logger
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
                Logger.w(TAG, "Alarm '${alarm.label}' (ID: ${alarm.id}) is disabled, skipping")
                return@withContext Result.failure(IllegalArgumentException("Alarm is disabled"))
            }
            
            // Calculate the alarm time (will fetch fresh astronomy data if needed)
            val calculatedTime = calculateAlarmTimeUseCase.execute(alarm)
            
            // Validate the calculated time
            if (calculatedTime <= System.currentTimeMillis()) {
                Logger.e(TAG, "Calculated time is in the past for alarm '${alarm.label}' (ID: ${alarm.id})")
                return@withContext Result.failure(IllegalStateException("Calculated time is in the past"))
            }
            
            // Update the alarm's calculated time in database
            val updatedAlarm = alarm.copy(calculatedTime = calculatedTime)
            alarmRepository.updateAlarm(updatedAlarm)
            
            // Schedule with AlarmManager
            scheduleWithAlarmManager(updatedAlarm)
            
            Result.success(Unit)
        } catch (e: SecurityException) {
            // Permission denied - this is critical, log it prominently
            Logger.e(TAG, "SECURITY EXCEPTION: Cannot schedule alarm '${alarm.label}' (ID: ${alarm.id}) - exact alarm permission not granted", e)
            Result.failure(e)
        } catch (e: Exception) {
            Logger.e(TAG, "Error scheduling alarm '${alarm.label}' (ID: ${alarm.id})", e)
            Result.failure(e)
        }
    }
    
    /**
     * Schedule an alarm with a specific time (used for snooze)
     * This bypasses time calculation and uses the provided time directly
     */
    suspend fun scheduleAlarmWithTime(alarm: Alarm, triggerTime: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate the trigger time
            if (triggerTime <= System.currentTimeMillis()) {
                Logger.e(TAG, "Trigger time is in the past for alarm '${alarm.label}' (ID: ${alarm.id})")
                return@withContext Result.failure(IllegalStateException("Trigger time is in the past"))
            }
            
            // Update the alarm's calculated time in database
            val updatedAlarm = alarm.copy(calculatedTime = triggerTime)
            alarmRepository.updateCalculatedTime(alarm.id, triggerTime)
            
            // Schedule with AlarmManager using the provided time
            scheduleWithAlarmManager(updatedAlarm)
            
            Result.success(Unit)
        } catch (e: SecurityException) {
            // Permission denied - this is critical, log it prominently
            Logger.e(TAG, "SECURITY EXCEPTION: Cannot schedule alarm '${alarm.label}' (ID: ${alarm.id}) with specific time - exact alarm permission not granted", e)
            Result.failure(e)
        } catch (e: Exception) {
            Logger.e(TAG, "Error scheduling alarm '${alarm.label}' (ID: ${alarm.id}) with specific time", e)
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
            Logger.e(TAG, "Error cancelling alarm '${alarm.label}' (ID: ${alarm.id})", e)
            Result.failure(e)
        }
    }
    
    /**
     * Schedule the next enabled alarm
     * This finds all enabled alarms, calculates their times, and schedules the closest one
     */
    suspend fun scheduleNextAlarm(): Result<Alarm?> = withContext(Dispatchers.IO) {
        try {
            Logger.i(TAG, "Scheduling next alarm")
            
            val enabledAlarms = alarmRepository.getEnabledAlarms()
            
            if (enabledAlarms.isEmpty()) {
                Logger.w(TAG, "No enabled alarms found")
                return@withContext Result.success(null)
            }
            
            val currentTime = System.currentTimeMillis()
            var closestAlarm: Alarm? = null
            var minTimeDifference = Long.MAX_VALUE
            
            for (alarm in enabledAlarms) {
                val calculatedTime = calculateAlarmTimeUseCase.execute(alarm)
                val diff = calculatedTime - currentTime
                
                if (diff <= 0) {
                    Logger.d(TAG, "Alarm '${alarm.label}' (ID: ${alarm.id}) is in the past, skipping")
                    continue
                }
                
                Logger.d(TAG, "Alarm '${alarm.label}' scheduled in ${diff / 1000 / 60} minutes")
                
                if (diff < minTimeDifference) {
                    closestAlarm = alarm.copy(calculatedTime = calculatedTime)
                    minTimeDifference = diff
                }
            }
            
            if (closestAlarm != null) {
                // Update the calculated time in database
                alarmRepository.updateCalculatedTime(closestAlarm.id, closestAlarm.calculatedTime)
                
                // Schedule with AlarmManager
                try {
                    scheduleWithAlarmManager(closestAlarm)
                    val minutesUntil = minTimeDifference / 1000 / 60
                    Logger.i(TAG, "Closest alarm set: '${closestAlarm.label}' (ID: ${closestAlarm.id}) in $minutesUntil minutes")
                    Result.success(closestAlarm)
                } catch (e: SecurityException) {
                    // Permission denied - this is critical
                    Logger.e(TAG, "SECURITY EXCEPTION: Cannot schedule closest alarm '${closestAlarm.label}' (ID: ${closestAlarm.id}) - exact alarm permission not granted", e)
                    Result.failure(e)
                }
            } else {
                Logger.w(TAG, "No valid alarms found to schedule")
                Result.success(null)
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, "SECURITY EXCEPTION: Error scheduling next alarm - exact alarm permission not granted", e)
            Result.failure(e)
        } catch (e: Exception) {
            Logger.e(TAG, "Error scheduling next alarm", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reschedule all enabled alarms
     */
    suspend fun rescheduleAllAlarms(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Logger.i(TAG, "Rescheduling all alarms")
            scheduleNextAlarm()
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Error rescheduling all alarms", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify that an alarm is properly scheduled and enabled
     */
    suspend fun verifyScheduledAlarm(alarmId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm == null) {
                Logger.w(TAG, "Alarm $alarmId not found in database")
                return@withContext Result.success(false)
            }
            
            if (!alarm.enabled) {
                Logger.w(TAG, "Alarm $alarmId is disabled")
                return@withContext Result.success(false)
            }
            
            val currentTime = System.currentTimeMillis()
            if (alarm.calculatedTime <= currentTime) {
                Logger.w(TAG, "Alarm $alarmId calculated time is in the past: ${alarm.calculatedTime} vs $currentTime")
                return@withContext Result.success(false)
            }
            
            val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val scheduledTime = fullDateFormat.format(Calendar.getInstance().apply { timeInMillis = alarm.calculatedTime }.time)
            val minutesUntil = (alarm.calculatedTime - currentTime) / 1000 / 60
            
            Logger.i(TAG, "Alarm $alarmId verification: enabled=${alarm.enabled}, scheduled for $scheduledTime (in $minutesUntil minutes)")
            Result.success(true)
        } catch (e: Exception) {
            Logger.e(TAG, "Error verifying alarm $alarmId", e)
            Result.failure(e)
        }
    }
    
    private fun scheduleWithAlarmManager(alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (alarmManager == null) {
            Logger.e(TAG, "AlarmManager is null")
            throw IllegalStateException("AlarmManager is not available")
        }
        
        // CRITICAL: Check exact alarm permission before scheduling (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val errorMsg = "CRITICAL: Exact alarm permission NOT granted! Alarm '${alarm.label}' (ID: ${alarm.id}) will NOT fire!"
                Logger.e(TAG, errorMsg)
                Logger.e(TAG, "User must grant SCHEDULE_EXACT_ALARM permission in settings")
                throw SecurityException("Exact alarm permission not granted: $errorMsg")
            } else {
                Logger.i(TAG, "Exact alarm permission verified: GRANTED")
            }
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
        
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = fullDateFormat.format(calendar.time)
        val currentTime = System.currentTimeMillis()
        val minutesUntil = (alarm.calculatedTime - currentTime) / 1000 / 60
        
        Logger.i(TAG, "Setting alarm '${alarm.label}' (ID: ${alarm.id}) at $formattedDate")
        Logger.d(TAG, "Current time: ${fullDateFormat.format(Calendar.getInstance().time)}")
        Logger.d(TAG, "Alarm will fire in $minutesUntil minutes (${alarm.calculatedTime - currentTime} ms)")
        
        val triggerTime = calendar.timeInMillis
        
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Logger.i(TAG, "Alarm scheduled successfully using setExactAndAllowWhileIdle")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Logger.i(TAG, "Alarm scheduled successfully using setExact")
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Logger.i(TAG, "Alarm scheduled successfully using set")
                }
            }
            
            // Log successful scheduling with details
            Logger.i(TAG, "Alarm '${alarm.label}' (ID: ${alarm.id}) successfully scheduled for $formattedDate")
        } catch (e: SecurityException) {
            val errorMsg = "SECURITY EXCEPTION: Permission denied for exact alarm. Alarm '${alarm.label}' (ID: ${alarm.id}) will NOT fire!"
            Logger.e(TAG, errorMsg, e)
            throw SecurityException(errorMsg, e)
        } catch (e: Exception) {
            val errorMsg = "UNEXPECTED ERROR scheduling alarm '${alarm.label}' (ID: ${alarm.id})"
            Logger.e(TAG, errorMsg, e)
            throw IllegalStateException(errorMsg, e)
        }
    }
    
    private fun cancelWithAlarmManager(alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        
        if (alarmManager == null) {
            Logger.e(TAG, "AlarmManager is null")
            throw IllegalStateException("AlarmManager is not available")
        }
        
        Logger.d(TAG, "Cancelling alarm with ID: $alarmId")
        
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
