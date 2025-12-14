package com.rooster.rooster

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import com.rooster.rooster.util.ThemeHelper
import com.rooster.rooster.worker.WorkManagerHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RoosterApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var scheduleAlarmUseCase: ScheduleAlarmUseCase
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        // Apply saved theme
        ThemeHelper.applyTheme(this)
        createNotificationChannels()
        scheduleBackgroundWork()
    }
    
    /**
     * Get ScheduleAlarmUseCase for use in BroadcastReceivers
     * BroadcastReceivers cannot use Hilt directly, so we provide access through Application
     */
    fun provideScheduleAlarmUseCase(): ScheduleAlarmUseCase {
        return scheduleAlarmUseCase
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alarm notification channel
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active alarms"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Background work notification channel
            val workChannel = NotificationChannel(
                WORK_CHANNEL_ID,
                "Background Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for background location and astronomy updates"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(workChannel)
        }
    }
    
    private fun scheduleBackgroundWork() {
        // Schedule periodic updates
        WorkManagerHelper.scheduleLocationUpdates(this)
        WorkManagerHelper.scheduleAstronomyUpdates(this)
        
        // Trigger immediate updates on app start to ensure fresh data
        WorkManagerHelper.triggerLocationUpdate(this)
    }
    
    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val WORK_CHANNEL_ID = "work_channel"
    }
}
