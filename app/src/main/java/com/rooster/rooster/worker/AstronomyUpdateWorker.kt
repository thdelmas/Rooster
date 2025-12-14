package com.rooster.rooster.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rooster.rooster.R
import com.rooster.rooster.data.repository.AstronomyDataResult
import com.rooster.rooster.data.repository.AstronomyRepository
import com.rooster.rooster.data.repository.LocationRepository
import com.rooster.rooster.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker to fetch astronomy data (sunrise, sunset, etc.)
 * Uses AstronomyRepository for caching and offline support
 */
@HiltWorker
class AstronomyUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sharedPreferences: SharedPreferences,
    private val astronomyRepository: AstronomyRepository,
    private val locationRepository: LocationRepository
) : CoroutineWorker(context, workerParams) {
    
    private val workerContext: Context = context
    
    companion object {
        const val TAG = "AstronomyUpdateWorker"
        const val WORK_NAME = "astronomy_update_work"
    }
    
    override suspend fun doWork(): Result {
        Logger.i(TAG, "Starting astronomy data update (attempt ${runAttemptCount + 1})")
        
        // Try to get location from Room database first
        var location = locationRepository.getLocation()
        var latitude = location?.latitude ?: 0f
        var longitude = location?.longitude ?: 0f
        
        // Fallback to SharedPreferences if not in database (for migration period)
        if (latitude == 0f && longitude == 0f) {
            Logger.d(TAG, "No location in database, checking SharedPreferences")
            latitude = sharedPreferences.getFloat("latitude", 0f)
            longitude = sharedPreferences.getFloat("longitude", 0f)
            
            // If found in SharedPreferences, migrate to database
            if (latitude != 0f || longitude != 0f) {
                Logger.d(TAG, "Migrating location from SharedPreferences to database")
                val altitude = sharedPreferences.getFloat("altitude", 0f)
                locationRepository.saveLocation(latitude, longitude, altitude)
            }
        }
        
        Logger.d(TAG, "Location: lat=$latitude, lng=$longitude")
        
        if (latitude == 0f && longitude == 0f) {
            Logger.w(TAG, "No location available, skipping astronomy update")
            return Result.retry()
        }
        
        return try {
            val result = astronomyRepository.fetchAndCacheAstronomyData(latitude, longitude)
            
            when (result) {
                is AstronomyDataResult.Fresh -> {
                    // Fresh data successfully fetched
                    saveToSharedPreferences(result.data)
                    Logger.i(TAG, "Astronomy data updated successfully (fresh)")
                    Result.success()
                }
                
                is AstronomyDataResult.Cached -> {
                    // Using cached data (may be stale)
                    saveToSharedPreferences(result.data)
                    
                    if (result.isStale) {
                        val ageHours = result.ageMs / (1000 * 60 * 60)
                        Logger.w(TAG, "Using stale cached astronomy data (age: ${ageHours}h)")
                        
                        // Show notification to user about stale data
                        showStaleDataNotification(result.ageMs)
                    } else {
                        Logger.i(TAG, "Using cached astronomy data (still valid, age: ${result.ageMs}ms)")
                    }
                    
                    Result.success()
                }
                
                is AstronomyDataResult.Failure -> {
                    Logger.e(TAG, "Failed to fetch astronomy data: ${result.exception.message}")
                    
                    // Last resort: try to get any cached data
                    val cachedData = astronomyRepository.getAstronomyData(forceRefresh = false)
                    if (cachedData != null) {
                        Logger.i(TAG, "Using cached astronomy data as last resort")
                        saveToSharedPreferences(cachedData)
                        showStaleDataNotification(System.currentTimeMillis() - cachedData.lastUpdated)
                        Result.success()
                    } else {
                        Logger.e(TAG, "No cached data available, will retry")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating astronomy data", e)
            
            // Try to use cached data on error
            try {
                val cachedData = astronomyRepository.getAstronomyData(forceRefresh = false)
                if (cachedData != null) {
                    Logger.i(TAG, "Using cached astronomy data after error")
                    saveToSharedPreferences(cachedData)
                    val age = System.currentTimeMillis() - cachedData.lastUpdated
                    if (age > com.rooster.rooster.util.AppConstants.ASTRONOMY_DATA_VALIDITY_MS) {
                        showStaleDataNotification(age)
                    }
                    return Result.success()
                }
            } catch (cacheError: Exception) {
                Logger.e(TAG, "Error loading cached data", cacheError)
            }
            
            Result.failure()
        }
    }
    
    /**
     * Save astronomy data to SharedPreferences for backward compatibility
     */
    private fun saveToSharedPreferences(data: com.rooster.rooster.data.local.entity.AstronomyDataEntity) {
        try {
            sharedPreferences.edit()
                .putLong("sunrise", data.sunrise)
                .putLong("sunset", data.sunset)
                .putLong("solarNoon", data.solarNoon)
                .putLong("civilDawn", data.civilDawn)
                .putLong("civilDusk", data.civilDusk)
                .putLong("nauticalDawn", data.nauticalDawn)
                .putLong("nauticalDusk", data.nauticalDusk)
                .putLong("astroDawn", data.astroDawn)
                .putLong("astroDusk", data.astroDusk)
                .putLong("dayLength", data.dayLength)
                .putLong("astronomy_last_updated", data.lastUpdated)
                .apply()
            
            Logger.d(TAG, "Astronomy data saved to SharedPreferences")
        } catch (e: Exception) {
            Logger.e(TAG, "Error saving to SharedPreferences", e)
        }
    }
    
    /**
     * Show notification to user when using stale astronomy data
     */
    private fun showStaleDataNotification(ageMs: Long) {
        try {
            val ageHours = ageMs / (1000 * 60 * 60)
            val ageMinutes = (ageMs % (1000 * 60 * 60)) / (1000 * 60)
            
            val message = if (ageHours > 0) {
                "Using cached astronomy data (${ageHours}h old). Alarms may be inaccurate."
            } else {
                "Using cached astronomy data (${ageMinutes}m old). Alarms may be inaccurate."
            }
            
            val notificationManager = NotificationManagerCompat.from(workerContext)
            
            // Create a simple notification channel if needed (for API 26+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channelId = "astronomy_data_warning"
                val channelName = "Astronomy Data Warnings"
                val importance = android.app.NotificationManager.IMPORTANCE_LOW
                val channel = android.app.NotificationChannel(channelId, channelName, importance)
                channel.description = "Notifications about stale astronomy data"
                val systemNotificationManager = workerContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                systemNotificationManager.createNotificationChannel(channel)
            }
            
            val notification = NotificationCompat.Builder(workerContext, "astronomy_data_warning")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Stale Astronomy Data")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(1001, notification)
            Logger.d(TAG, "Stale data notification shown: $message")
        } catch (e: Exception) {
            Logger.e(TAG, "Error showing stale data notification", e)
        }
    }
}

