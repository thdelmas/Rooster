package com.rooster.rooster.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Helper class to schedule WorkManager tasks
 */
object WorkManagerHelper {
    
    /**
     * Schedule periodic location updates
     */
    fun scheduleLocationUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            3, TimeUnit.HOURS, // Repeat every 3 hours (optimized for battery)
            1, TimeUnit.HOURS  // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LocationUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Trigger immediate location update
     */
    fun triggerLocationUpdate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<LocationUpdateWorker>()
            .addTag("location_update_immediate")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "location_update_immediate",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Trigger immediate astronomy update
     */
    fun triggerAstronomyUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<AstronomyUpdateWorker>()
            .setConstraints(constraints)
            .addTag("astronomy_update_immediate")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "astronomy_update_immediate",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Schedule periodic astronomy data updates
     */
    fun scheduleAstronomyUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AstronomyUpdateWorker>(
            6, TimeUnit.HOURS, // Repeat every 6 hours
            1, TimeUnit.HOURS  // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AstronomyUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Cancel all scheduled work
     */
    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
    
    /**
     * Cancel location updates
     */
    fun cancelLocationUpdates(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(LocationUpdateWorker.WORK_NAME)
    }
    
    /**
     * Cancel astronomy updates
     */
    fun cancelAstronomyUpdates(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AstronomyUpdateWorker.WORK_NAME)
    }
}
