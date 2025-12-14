package com.rooster.rooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rooster.rooster.RoosterApplication
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import com.rooster.rooster.util.AlarmNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver to handle alarm snooze action
 * Uses ScheduleAlarmUseCase for reliable alarm scheduling
 */
class SnoozeReceiver : BroadcastReceiver() {
    
    // Use application-level scope for background operations
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        const val ACTION_SNOOZE = "com.rooster.rooster.ACTION_SNOOZE"
        const val SNOOZE_DURATION_MINUTES = 10
        private const val TAG = "SnoozeReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return
        
        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) {
            Log.e(TAG, "Invalid alarm ID")
            return
        }
        
        Log.i(TAG, "Snoozing alarm $alarmId for $SNOOZE_DURATION_MINUTES minutes")
        
        // Use coroutine scope to handle async operations
        receiverScope.launch {
            try {
                val application = context.applicationContext as? RoosterApplication
                if (application == null) {
                    Log.e(TAG, "RoosterApplication not available")
                    return@launch
                }
                
                // Get dependencies from Application
                val scheduleAlarmUseCase = application.provideScheduleAlarmUseCase()
                val alarmRepository = application.provideAlarmRepository()
                
                // Get alarm from repository
                val alarm = alarmRepository.getAlarmById(alarmId)
                
                if (alarm != null) {
                    // Calculate snooze time
                    val snoozeTime = System.currentTimeMillis() + (SNOOZE_DURATION_MINUTES * 60 * 1000)
                    
                    // Update calculated time in database
                    alarmRepository.updateCalculatedTime(alarmId, snoozeTime)
                    
                    // Reschedule the alarm using ScheduleAlarmUseCase for consistency
                    val updatedAlarm = alarm.copy(calculatedTime = snoozeTime)
                    val result = scheduleAlarmUseCase.scheduleAlarm(updatedAlarm)
                    
                    result.fold(
                        onSuccess = {
                            // Cancel the notification
                            AlarmNotificationHelper.cancelNotification(context, alarmId)
                            Log.i(TAG, "Alarm snoozed successfully and rescheduled")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error scheduling snoozed alarm", e)
                        }
                    )
                } else {
                    Log.e(TAG, "Alarm not found: $alarmId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing alarm", e)
            }
        }
    }
}
