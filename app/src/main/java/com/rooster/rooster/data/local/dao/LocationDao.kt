package com.rooster.rooster.data.local.dao

import androidx.room.*
import com.rooster.rooster.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for location data
 */
@Dao
interface LocationDao {
    
    @Query("SELECT * FROM location_data WHERE id = 1")
    suspend fun getLocation(): LocationEntity?
    
    @Query("SELECT * FROM location_data WHERE id = 1")
    fun getLocationFlow(): Flow<LocationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)
    
    @Query("DELETE FROM location_data")
    suspend fun deleteAll()
    
    /**
     * Check if location data is stale (older than specified age)
     */
    @Query("SELECT ((:currentTime - lastUpdated) > :maxAge) FROM location_data WHERE id = 1")
    suspend fun isLocationStale(currentTime: Long, maxAge: Long = 24 * 60 * 60 * 1000): Boolean? // Default 24 hours
}
