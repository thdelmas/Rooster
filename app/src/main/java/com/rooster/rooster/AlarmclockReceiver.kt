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
                    // Get ScheduleAlarmUseCase from Hilt
                    val scheduleAlarmUseCase = (context.applicationContext as? RoosterApplication)
                        ?.provideScheduleAlarmUseCase()
                    
                    if (scheduleAlarmUseCase != null) {
                        // Validate and reschedule all alarms on boot
                        // ScheduleAlarmUseCase.scheduleNextAlarm() already validates:
                        // - Filters out disabled alarms
                        // - Filters out past alarms (calculatedTime <= currentTime)
                        // - Validates calculated times are in the future
                        val result = scheduleAlarmUseCase.rescheduleAllAlarms()
                        result.fold(
                            onSuccess = {
                                Log.i("AlarmclockReceiver", "Alarms validated and rescheduled after boot")
                            },
                            onFailure = { e ->
                                Log.e("AlarmclockReceiver", "Error validating/rescheduling alarms after boot", e)
                            }
                        )
                    } else {
                        Log.w("AlarmclockReceiver", "ScheduleAlarmUseCase not available, using fallback AlarmHandler")
                        // Fallback to AlarmHandler if Hilt is not available
                        // Note: AlarmHandler also validates alarms (skips disabled and past alarms)
                        AlarmHandler().setNextAlarm(context)
                    }
                } catch (e: Exception) {
                    Log.e("AlarmclockReceiver", "Error rescheduling alarms after boot", e)
                }
            }
        }
    }
}