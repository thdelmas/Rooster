package com.rooster.rooster.data.repository

import android.content.Context
import com.rooster.rooster.data.local.dao.AstronomyDao
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.SolarCalculator
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for astronomy data using on-device solar calculation.
 * No network dependency — all times computed locally from GPS coordinates.
 */
@Singleton
class AstronomyRepository @Inject constructor(
    private val astronomyDao: AstronomyDao,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AstronomyRepository"
    }

    /**
     * Get astronomy data as Flow (reactive updates)
     */
    fun getAstronomyDataFlow(): Flow<AstronomyDataEntity?> {
        return astronomyDao.getAstronomyDataFlow()
    }

    /**
     * Get astronomy data, preferring cached data if valid
     */
    suspend fun getAstronomyData(forceRefresh: Boolean = false): AstronomyDataEntity? {
        val cachedData = astronomyDao.getAstronomyData()

        if (!forceRefresh && cachedData != null && isDataValid(cachedData)) {
            Logger.d(TAG, "Returning cached astronomy data")
            return cachedData
        }

        if (cachedData != null) {
            val age = System.currentTimeMillis() - cachedData.lastUpdated
            Logger.d(TAG, "Returning stale cached astronomy data (age: ${age}ms)")
        } else {
            Logger.d(TAG, "No cached astronomy data available")
        }

        return cachedData
    }

    /**
     * Calculate astronomy data on-device and cache it.
     * No network required — uses NOAA solar equations.
     */
    suspend fun fetchAndCacheAstronomyData(latitude: Float, longitude: Float): AstronomyDataResult {
        return try {
            Logger.i(TAG, "Calculating astronomy data for location: $latitude, $longitude")

            val astronomyData = SolarCalculator.calculate(latitude, longitude)

            if (validateAstronomyData(astronomyData)) {
                astronomyDao.insertAstronomyData(astronomyData)
                Logger.i(TAG, "Astronomy data calculated and cached successfully")
                AstronomyDataResult.Fresh(astronomyData)
            } else {
                Logger.w(TAG, "Calculated data failed validation, using cached data if available")
                fallbackToCached(astronomyDao.getAstronomyData())
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error calculating astronomy data", e)
            fallbackToCached(astronomyDao.getAstronomyData(), e)
        }
    }

    /**
     * Fallback to cached data when calculation fails
     */
    private fun fallbackToCached(
        cachedData: AstronomyDataEntity?,
        originalException: Throwable? = null
    ): AstronomyDataResult {
        return if (cachedData != null) {
            val age = System.currentTimeMillis() - cachedData.lastUpdated
            val isStale = !isDataValid(cachedData)
            Logger.i(TAG, "Using cached data as fallback (age: ${age}ms, stale: $isStale)")
            AstronomyDataResult.Cached(cachedData, isStale, age)
        } else {
            Logger.e(TAG, "No cached data available for fallback")
            AstronomyDataResult.Failure(
                originalException ?: Exception("Failed to calculate astronomy data and no cache available")
            )
        }
    }

    /**
     * Check if cached data is still valid
     */
    fun isDataValid(data: AstronomyDataEntity): Boolean {
        val age = System.currentTimeMillis() - data.lastUpdated
        return age < AppConstants.ASTRONOMY_DATA_VALIDITY_MS
    }

    /**
     * Check if cached data is stale
     */
    fun isDataStale(data: AstronomyDataEntity): Boolean {
        return !isDataValid(data)
    }

    /**
     * Get the age of cached data in milliseconds
     */
    fun getDataAge(data: AstronomyDataEntity): Long {
        return System.currentTimeMillis() - data.lastUpdated
    }

    /**
     * Validate astronomy data before caching
     */
    private fun validateAstronomyData(data: AstronomyDataEntity): Boolean {
        val now = System.currentTimeMillis()

        val validations: List<Boolean> = listOf(
            (data.sunrise > 0L),
            (data.sunset > 0L),
            (data.sunrise < data.sunset),
            (data.solarNoon > 0L),
            (data.civilDawn > 0L),
            (data.civilDusk > 0L),
            (data.civilDawn < data.sunrise),
            (data.sunset < data.civilDusk),
            (data.dayLength > 0L),
            (data.sunrise > now - AppConstants.MILLIS_PER_YEAR),
            (data.sunrise < now + AppConstants.MILLIS_PER_YEAR),
            (data.latitude >= -90f && data.latitude <= 90f),
            (data.longitude >= -180f && data.longitude <= 180f)
        )

        val isValid = validations.all { it }

        if (!isValid) {
            Logger.w(TAG, "Astronomy data validation failed: sunrise=${data.sunrise}, sunset=${data.sunset}, " +
                    "latitude=${data.latitude}, longitude=${data.longitude}")
        }

        return isValid
    }

    /**
     * Get astronomy data with freshness information
     */
    suspend fun getAstronomyDataWithFreshness(): AstronomyDataResult {
        val cachedData = astronomyDao.getAstronomyData()

        return if (cachedData != null) {
            val age = System.currentTimeMillis() - cachedData.lastUpdated
            val isStale = !isDataValid(cachedData)
            AstronomyDataResult.Cached(cachedData, isStale, age)
        } else {
            AstronomyDataResult.Failure(Exception("No cached astronomy data available"))
        }
    }

    /**
     * Clear cached astronomy data
     */
    suspend fun clearCache() {
        astronomyDao.deleteAll()
        Logger.i(TAG, "Astronomy data cache cleared")
    }
}
