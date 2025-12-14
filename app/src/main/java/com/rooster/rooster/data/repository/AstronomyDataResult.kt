package com.rooster.rooster.data.repository

import com.rooster.rooster.data.local.entity.AstronomyDataEntity

/**
 * Result type for astronomy data operations that includes freshness information
 */
sealed class AstronomyDataResult {
    /**
     * Fresh data successfully fetched from API
     */
    data class Fresh(val data: AstronomyDataEntity) : AstronomyDataResult()
    
    /**
     * Cached data being used (may be stale)
     * @param data The cached astronomy data
     * @param isStale Whether the data is considered stale (older than validity period)
     * @param ageMs Age of the data in milliseconds
     */
    data class Cached(val data: AstronomyDataEntity, val isStale: Boolean, val ageMs: Long) : AstronomyDataResult()
    
    /**
     * Failed to fetch data and no cached data available
     */
    data class Failure(val exception: Throwable) : AstronomyDataResult()
    
    /**
     * Get the astronomy data if available
     */
    fun getDataOrNull(): AstronomyDataEntity? {
        return when (this) {
            is Fresh -> data
            is Cached -> data
            is Failure -> null
        }
    }
    
    /**
     * Check if data is available (fresh or cached)
     */
    fun isSuccess(): Boolean {
        return this is Fresh || this is Cached
    }
    
    /**
     * Check if data is stale
     */
    fun isDataStale(): Boolean {
        return when (this) {
            is Fresh -> false
            is Cached -> isStale
            is Failure -> false
        }
    }
}
