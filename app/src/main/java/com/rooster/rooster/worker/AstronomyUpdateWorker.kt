package com.rooster.rooster.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rooster.rooster.data.repository.AstronomyRepository
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
    private val astronomyRepository: AstronomyRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val TAG = "AstronomyUpdateWorker"
        const val WORK_NAME = "astronomy_update_work"
    }
    
    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting astronomy data update (attempt ${runAttemptCount + 1})")
        
        val latitude = sharedPreferences.getFloat("latitude", 0f)
        val longitude = sharedPreferences.getFloat("longitude", 0f)
        
        Log.d(TAG, "Location from SharedPreferences: lat=$latitude, lng=$longitude")
        
        if (latitude == 0f && longitude == 0f) {
            Log.w(TAG, "No location available, skipping astronomy update")
            return Result.retry()
        }
        
        return try {
            val result = astronomyRepository.fetchAndCacheAstronomyData(latitude, longitude)
            
            if (result.isSuccess) {
                val astronomyData = result.getOrNull()!!
                
                // Also update SharedPreferences for backward compatibility
                saveToSharedPreferences(astronomyData)
                
                Log.i(TAG, "Astronomy data updated successfully")
                Result.success()
            } else {
                Log.w(TAG, "Failed to fetch astronomy data: ${result.exceptionOrNull()?.message}")
                
                // Check if we have valid cached data
                val cachedData = astronomyRepository.getAstronomyData(forceRefresh = false)
                if (cachedData != null) {
                    Log.i(TAG, "Using cached astronomy data")
                    saveToSharedPreferences(cachedData)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating astronomy data", e)
            
            // Try to use cached data on error
            try {
                val cachedData = astronomyRepository.getAstronomyData(forceRefresh = false)
                if (cachedData != null) {
                    Log.i(TAG, "Using cached astronomy data after error")
                    saveToSharedPreferences(cachedData)
                    return Result.success()
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "Error loading cached data", cacheError)
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
            
            Log.d(TAG, "Astronomy data saved to SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to SharedPreferences", e)
        }
    }
}

