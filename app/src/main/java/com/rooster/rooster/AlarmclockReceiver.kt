package com.rooster.rooster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling alarm triggers and boot completion
 * Uses ScheduleAlarmUseCase for reliable alarm scheduling
 */
class AlarmclockReceiver : BroadcastReceiver() {
    
    // Use application-level scope for background operations
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AlarmclockReceiver", "Received broadcast: ${intent?.action}")
        
        if (intent != null && "com.rooster.alarmmanager" == intent.action) {
            // Safe null handling - validate alarm_id before use
            val alarmIdStr = intent.getStringExtra("alarm_id")
            val alarmId = alarmIdStr?.toLongOrNull()
            
            if (alarmId == null || alarmId <= 0) {
                Log.e("AlarmclockReceiver", "Invalid or missing alarm_id: $alarmIdStr")
                return
            }
            
            Log.i("AlarmclockReceiver", "Processing alarm id: $alarmId")

            // Create a notification channel.
            val notificationChannel =
                NotificationChannel("ALARM_CHANNEL", "Alarm", NotificationManager.IMPORTANCE_MAX)
            notificationChannel.description = "Alarm notifications"
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)


            val alarmActivityIntent = Intent(context, AlarmActivity::class.java)
            alarmActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            alarmActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            alarmActivityIntent.putExtra("alarm_id", alarmId.toString())
            val alarmActivityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                alarmActivityIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Create a notification builder.
            val notificationBuilder = NotificationCompat.Builder(context, notificationChannel.id)
                .setContentTitle("Rooster")
                .setContentText("Click here to stop the alarm")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setFullScreenIntent(alarmActivityPendingIntent, true)

            // Build the notification.
            val notification = notificationBuilder.build()

            // Show the notification.
            notificationManager.notify(1, notification)
            context.applicationContext.startActivity(alarmActivityIntent)
            
            // Schedule next alarm immediately using ScheduleAlarmUseCase
            // This ensures reliable scheduling even if app is killed
            receiverScope.launch {
                try {
                    // Get ScheduleAlarmUseCase from Hilt
                    val scheduleAlarmUseCase = (context.applicationContext as? RoosterApplication)
                        ?.provideScheduleAlarmUseCase()
                    
                    if (scheduleAlarmUseCase != null) {
                        val result = scheduleAlarmUseCase.scheduleNextAlarm()
                        result.fold(
                            onSuccess = { alarm ->
                                if (alarm != null) {
                                    Log.i("AlarmclockReceiver", "Next alarm scheduled successfully: ${alarm.label}")
                                } else {
                                    Log.i("AlarmclockReceiver", "No enabled alarms to schedule")
                                }
                            },
                            onFailure = { e ->
                                Log.e("AlarmclockReceiver", "Error scheduling next alarm", e)
                            }
                        )
                    } else {
                        Log.w("AlarmclockReceiver", "ScheduleAlarmUseCase not available, using fallback AlarmHandler")
                        // Fallback to AlarmHandler if Hilt is not available
                        AlarmHandler().setNextAlarm(context)
                    }
                } catch (e: Exception) {
                    Log.e("AlarmclockReceiver", "Error scheduling next alarm", e)
                }
            }
        } else if (intent != null && "android.intent.action.BOOT_COMPLETED" == intent.action) {
            Log.i("AlarmclockReceiver", "Boot completed, validating and rescheduling alarms")
            // Use coroutine scope for async operations
            receiverScope.launch {
                try {
                    val app = context.applicationContext as? RoosterApplication
                    val scheduleAlarmUseCase = app?.provideScheduleAlarmUseCase()
                    val alarmRepository = app?.provideAlarmRepository()
                    
                    if (scheduleAlarmUseCase != null && alarmRepository != null) {
                        // Explicitly validate all alarms before scheduling
                        val currentTime = System.currentTimeMillis()
                        val enabledAlarms = alarmRepository.getEnabledAlarms()
                        
                        Log.i("AlarmclockReceiver", "Found ${enabledAlarms.size} enabled alarm(s) to validate")
                        
                        var validAlarmsCount = 0
                        var invalidAlarmsCount = 0
                        var pastAlarmsCount = 0
                        
                        // Validate each alarm
                        for (alarm in enabledAlarms) {
                            try {
                                // Validate alarm ID
                                if (alarm.id <= 0) {
                                    Log.w("AlarmclockReceiver", "Invalid alarm ID: ${alarm.id} for alarm '${alarm.label}', skipping")
                                    invalidAlarmsCount++
                                    continue
                                }
                                
                                // Validate alarm label
                                if (alarm.label.isBlank()) {
                                    Log.w("AlarmclockReceiver", "Alarm ID ${alarm.id} has blank label, skipping")
                                    invalidAlarmsCount++
                                    continue
                                }
                                
                                // Check if calculated time is in the past
                                val calculatedTime = alarm.calculatedTime
                                if (calculatedTime > 0 && calculatedTime <= currentTime) {
                                    Log.w("AlarmclockReceiver", "Alarm '${alarm.label}' (ID: ${alarm.id}) has calculated time in the past: $calculatedTime (current: $currentTime), will recalculate")
                                    pastAlarmsCount++
                                    // The time will be recalculated in scheduleNextAlarm()
                                }
                                
                                // Validate calculated time is reasonable (not too far in the past)
                                if (calculatedTime > 0 && calculatedTime < currentTime - 86400000) { // More than 24 hours in the past
                                    Log.w("AlarmclockReceiver", "Alarm '${alarm.label}' (ID: ${alarm.id}) has calculated time more than 24h in the past, will recalculate")
                                }
                                
                                validAlarmsCount++
                                Log.d("AlarmclockReceiver", "Alarm '${alarm.label}' (ID: ${alarm.id}) validated successfully")
                            } catch (e: Exception) {
                                Log.e("AlarmclockReceiver", "Error validating alarm '${alarm.label}' (ID: ${alarm.id})", e)
                                invalidAlarmsCount++
                            }
                        }
                        
                        Log.i("AlarmclockReceiver", "Validation complete: $validAlarmsCount valid, $invalidAlarmsCount invalid, $pastAlarmsCount with past times")
                        
                        // Now reschedule all alarms (this will recalculate times and schedule the next one)
                        val result = scheduleAlarmUseCase.rescheduleAllAlarms()
                        result.fold(
                            onSuccess = {
                                Log.i("AlarmclockReceiver", "Alarms validated and rescheduled after boot successfully")
                            },
                            onFailure = { e ->
                                Log.e("AlarmclockReceiver", "Error rescheduling alarms after boot", e)
                            }
                        )
                    } else {
                        Log.w("AlarmclockReceiver", "ScheduleAlarmUseCase or AlarmRepository not available, using fallback AlarmHandler")
                        // Fallback to AlarmHandler if Hilt is not available
                        // Note: AlarmHandler also validates alarms (skips disabled and past alarms)
                        AlarmHandler().setNextAlarm(context)
                    }
                } catch (e: Exception) {
                    Log.e("AlarmclockReceiver", "Error validating/rescheduling alarms after boot", e)
                }
            }
        }
    }
}