package com.rooster.rooster.data.repository

import android.location.Location
import com.rooster.rooster.data.local.dao.LocationDao
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for location data with Room database storage
 */
@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {
    
    companion object {
        private const val TAG = "LocationRepository"
    }
    
    /**
     * Get location data as Flow (reactive updates)
     */
    fun getLocationFlow(): Flow<LocationEntity?> {
        return locationDao.getLocationFlow()
    }
    
    /**
     * Get current location from database
     */
    suspend fun getLocation(): LocationEntity? {
        return locationDao.getLocation()
    }
    
    /**
     * Save location to database
     */
    suspend fun saveLocation(location: Location) {
        val locationEntity = LocationEntity(
            id = 1,
            latitude = location.latitude.toFloat(),
            longitude = location.longitude.toFloat(),
            altitude = location.altitude.toFloat(),
            lastUpdated = System.currentTimeMillis()
        )
        
        locationDao.insertLocation(locationEntity)
        Logger.i(TAG, "Location saved: ${location.latitude}, ${location.longitude}")
    }
    
    /**
     * Save location from coordinates
     */
    suspend fun saveLocation(latitude: Float, longitude: Float, altitude: Float = 0f) {
        val locationEntity = LocationEntity(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            lastUpdated = System.currentTimeMillis()
        )
        
        locationDao.insertLocation(locationEntity)
        Logger.i(TAG, "Location saved: $latitude, $longitude")
    }
    
    /**
     * Check if location data is stale
     */
    suspend fun isLocationStale(maxAge: Long = AppConstants.LOCATION_VALIDITY_MS): Boolean {
        return locationDao.isLocationStale(System.currentTimeMillis(), maxAge) ?: true
    }
    
    /**
     * Clear location data
     */
    suspend fun clearLocation() {
        locationDao.deleteAll()
        Logger.i(TAG, "Location data cleared")
    }
}
