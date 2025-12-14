package com.rooster.rooster.data.local.dao

import androidx.room.*
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for astronomy data
 */
@Dao
interface AstronomyDao {
    
    @Query("SELECT * FROM astronomy_data WHERE id = 1")
    suspend fun getAstronomyData(): AstronomyDataEntity?
    
    @Query("SELECT * FROM astronomy_data WHERE id = 1")
    fun getAstronomyDataFlow(): Flow<AstronomyDataEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAstronomyData(data: AstronomyDataEntity)
    
    @Query("DELETE FROM astronomy_data")
    suspend fun deleteAll()
    
    /**
     * Check if astronomy data is stale (older than specified age)
     * Default maxAge is 6 hours (use AppConstants.ASTRONOMY_DATA_VALIDITY_MS)
     */
    @Query("SELECT ((:currentTime - lastUpdated) > :maxAge) FROM astronomy_data WHERE id = 1")
    suspend fun isDataStale(currentTime: Long, maxAge: Long): Boolean?
}
