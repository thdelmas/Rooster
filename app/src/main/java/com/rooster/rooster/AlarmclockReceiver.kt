package com.rooster.rooster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.os.postDelayed

class AlarmclockReceiver : BroadcastReceiver() {
    private var alarmHandler = AlarmHandler()
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("RECEIVER", "RECEIVED WELL")
        if (intent != null && "com.rooster.alarmmanager" == intent.action) {
            val alarmId = intent.getStringExtra("alarm_id")!!
                .toLong() // -1 is a default value if "alarm_id" is not found
            Log.e("Alarmclock Reciever", "Alarm id: $alarmId")

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
            val handler = Handler()
            val delay = 30 * 1000L
            handler.postDelayed({
                alarmHandler.setNextAlarm(context)
            }, delay)
        } else if (intent != null && "android.intent.action.BOOT_COMPLETED" == intent.action) {
            alarmHandler.setNextAlarm(context)
        }
    }
}