package com.rooster.rooster.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rooster.rooster.AlarmActivity
import com.rooster.rooster.R
import com.rooster.rooster.RoosterApplication
import com.rooster.rooster.receiver.SnoozeReceiver

/**
 * Helper class for creating alarm notifications with snooze functionality
 */
object AlarmNotificationHelper {
    
    private const val SNOOZE_REQUEST_CODE = 100
    
    /**
     * Create a notification for an active alarm with snooze action
     */
    fun createAlarmNotification(
        context: Context,
        alarmId: Long,
        title: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Intent to open the alarm activity
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent for snooze action
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
        }
        
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE + alarmId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, RoosterApplication.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(activityPendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Snooze 10 min",
                snoozePendingIntent
            )
            .setFullScreenIntent(activityPendingIntent, true)
            .build()
        
        notificationManager.notify(alarmId.toInt(), notification)
    }
    
    /**
     * Cancel an alarm notification
     */
    fun cancelNotification(context: Context, alarmId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.toInt())
    }
}
