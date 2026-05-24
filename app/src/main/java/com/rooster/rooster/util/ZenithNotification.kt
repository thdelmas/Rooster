package com.rooster.rooster.util

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rooster.rooster.R
import com.rooster.rooster.RoosterApplication

/**
 * Silent visual marker of the zenith window (-432s to +432s around solar
 * noon). Posted on a LOW-importance channel so it carries no sound and no
 * vibration of its own — the gong and its paired vibration handle the
 * sensory moment at noon itself.
 */
object ZenithNotification {

    const val NOTIFICATION_ID = 7432

    fun show(context: Context) {
        val notification = NotificationCompat.Builder(context, RoosterApplication.ZENITH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zenith approaching")
            .setContentText("The sun is near its apex")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setOngoing(false)
            .setAutoCancel(true)
            .setTimeoutAfter(AppConstants.ZENITH_WINDOW_TOTAL_MS)
            .build()

        val manager = NotificationManagerCompat.from(context)
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (granted) {
            runCatching { manager.notify(NOTIFICATION_ID, notification) }
                .onFailure { Logger.e(TAG, "Failed to post zenith notification", it) }
        } else {
            Logger.w(TAG, "POST_NOTIFICATIONS not granted — skipping")
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }

    private const val TAG = "ZenithNotification"
}
