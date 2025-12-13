package com.rooster.rooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rooster.rooster.AlarmHandler
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.util.AlarmNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver to handle alarm snooze action
 */
@AndroidEntryPoint
class SnoozeReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var alarmRepository: AlarmRepository
    
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = alarmRepository.getAlarmById(alarmId)
                
                if (alarm != null) {
                    // Calculate snooze time
                    val snoozeTime = System.currentTimeMillis() + (SNOOZE_DURATION_MINUTES * 60 * 1000)
                    
                    // Update calculated time
                    alarmRepository.updateCalculatedTime(alarmId, snoozeTime)
                    
                    // Reschedule the alarm
                    val updatedAlarm = alarm.copy(calculatedTime = snoozeTime)
                    val alarmHandler = AlarmHandler()
                    alarmHandler.setAlarm(context, updatedAlarm)
                    
                    // Cancel the notification
                    AlarmNotificationHelper.cancelNotification(context, alarmId)
                    
                    Log.i(TAG, "Alarm snoozed successfully")
                } else {
                    Log.e(TAG, "Alarm not found: $alarmId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing alarm", e)
            }
        }
    }
}
