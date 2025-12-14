package com.rooster.rooster

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import android.content.SharedPreferences
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.data.repository.AstronomyRepository
import com.rooster.rooster.data.repository.LocationRepository
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    
    @Inject
    lateinit var alarmRepository: AlarmRepository
    
    @Inject
    lateinit var locationRepository: LocationRepository
    
    @Inject
    lateinit var astronomyRepository: AstronomyRepository
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        // Apply saved theme
        ThemeHelper.applyTheme(this)
        createNotificationChannels()
        migrateSharedPreferencesToRoom()
        scheduleBackgroundWork()
    }
    
    /**
     * Migrate location and astronomy data from SharedPreferences to Room database
     * This is a one-time migration that runs on app startup
     */
    private fun migrateSharedPreferencesToRoom() {
        applicationScope.launch {
            try {
                // Check if migration has already been done
                val migrationDone = sharedPreferences.getBoolean("migration_to_room_done", false)
                if (migrationDone) {
                    return@launch
                }
                
                // Migrate location data
                val latitude = sharedPreferences.getFloat("latitude", 0f)
                val longitude = sharedPreferences.getFloat("longitude", 0f)
                val altitude = sharedPreferences.getFloat("altitude", 0f)
                
                if (latitude != 0f || longitude != 0f) {
                    locationRepository.saveLocation(latitude, longitude, altitude)
                    android.util.Log.i("RoosterApplication", "Migrated location data to Room")
                }
                
                // Migrate astronomy data if it exists in SharedPreferences but not in Room
                val astronomyData = astronomyRepository.getAstronomyData(forceRefresh = false)
                if (astronomyData == null) {
                    val sunrise = sharedPreferences.getLong("sunrise", 0)
                    if (sunrise != 0L) {
                        // We have astronomy data in SharedPreferences, but it should already be in Room
                        // from AstronomyUpdateWorker. However, if it's missing, we can't fully migrate
                        // without location data, so we'll just mark migration as done
                        android.util.Log.i("RoosterApplication", "Astronomy data migration skipped (will be fetched on next update)")
                    }
                }
                
                // Mark migration as done
                sharedPreferences.edit().putBoolean("migration_to_room_done", true).apply()
                android.util.Log.i("RoosterApplication", "Migration to Room completed")
            } catch (e: Exception) {
                android.util.Log.e("RoosterApplication", "Error during migration", e)
            }
        }
    }
    
    /**
     * Get ScheduleAlarmUseCase for use in BroadcastReceivers
     * BroadcastReceivers cannot use Hilt directly, so we provide access through Application
     */
    fun provideScheduleAlarmUseCase(): ScheduleAlarmUseCase {
        return scheduleAlarmUseCase
    }
    
    /**
     * Get AlarmRepository for use in BroadcastReceivers
     * BroadcastReceivers cannot use Hilt directly, so we provide access through Application
     */
    fun provideAlarmRepository(): AlarmRepository {
        return alarmRepository
    }
    
    /**
     * Get LocationRepository for use in Services
     * Services cannot use Hilt directly in some cases, so we provide access through Application
     */
    fun provideLocationRepository(): LocationRepository {
        return locationRepository
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
