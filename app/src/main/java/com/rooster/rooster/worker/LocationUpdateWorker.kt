package com.rooster.rooster.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rooster.rooster.data.repository.LocationRepository
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * WorkManager worker to update device location
 */
@HiltWorker
class LocationUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val TAG = "LocationUpdateWorker"
        const val WORK_NAME = "location_update_work"
    }
    
    override suspend fun doWork(): Result {
        Logger.i(TAG, "Starting location update")
        
        if (!isLocationPermissionGranted()) {
            Logger.w(TAG, "Location permission not granted")
            return Result.failure()
        }
        
        return try {
            val location = getCurrentLocation()
            
            if (location != null) {
                saveLocation(location)
                Logger.i(TAG, "Location updated successfully: ${location.latitude}, ${location.longitude}")
                
                // Trigger immediate astronomy data update after location update
                WorkManagerHelper.triggerAstronomyUpdate(applicationContext)
                
                Result.success()
            } else {
                Logger.w(TAG, "Failed to get current location")
                Result.retry()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating location", e)
            Result.failure()
        }
    }
    
    private suspend fun getCurrentLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        
        return try {
            // Try to get last known location first (fast)
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastLocation = fusedLocationClient.lastLocation.await()
                
                if (lastLocation != null && isLocationRecent(lastLocation)) {
                    Logger.d(TAG, "Using recent cached location")
                    return lastLocation
                }
                
                Logger.d(TAG, "Requesting current location...")
                
                // If last location is not recent or null, request current location
                val currentLocationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(AppConstants.LOCATION_ACCEPT_MAX_AGE_MS) // Accept location up to 1 hour old
                    .setDurationMillis(10 * AppConstants.MILLIS_PER_SECOND.toInt()) // Wait up to 10 seconds for a location
                    .build()
                
                val currentLocation = fusedLocationClient.getCurrentLocation(currentLocationRequest, null).await()
                
                if (currentLocation != null) {
                    Logger.d(TAG, "Got current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
                    return currentLocation
                } else {
                    Logger.w(TAG, "getCurrentLocation returned null, falling back to lastLocation")
                    return lastLocation // Return even old location if available
                }
            } else {
                Logger.w(TAG, "Location permission not granted")
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting current location", e)
            // Try to return last known location even if there's an exception
            try {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val fallbackLocation = fusedLocationClient.lastLocation.await()
                    if (fallbackLocation != null) {
                        Logger.i(TAG, "Using fallback location after error")
                        return fallbackLocation
                    }
                }
            } catch (fallbackException: Exception) {
                Logger.e(TAG, "Error getting fallback location", fallbackException)
            }
            null
        }
    }
    
    private fun isLocationRecent(location: Location): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < AppConstants.LOCATION_MAX_AGE_MS
    }
    
    private suspend fun saveLocation(location: Location) {
        locationRepository.saveLocation(location)
    }
    
    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
