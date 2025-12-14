package com.rooster.rooster.data.repository

import android.content.Context
import com.rooster.rooster.data.local.dao.AstronomyDao
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.NetworkConnectivityHelper
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for astronomy data with offline caching and robust offline handling
 */
@Singleton
class AstronomyRepository @Inject constructor(
    private val astronomyDao: AstronomyDao,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AstronomyRepository"
        private const val API_URL = "https://api.sunrise-sunset.org/json"
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
        
        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && cachedData != null && isDataValid(cachedData)) {
            Logger.d(TAG, "Returning cached astronomy data")
            return cachedData
        }
        
        // If data is stale or force refresh, but we still have old data, return it
        // The caller should use fetchAndCacheAstronomyData to get fresh data
        if (cachedData != null) {
            val age = System.currentTimeMillis() - cachedData.lastUpdated
            Logger.d(TAG, "Returning stale cached astronomy data (age: ${age}ms)")
        } else {
            Logger.d(TAG, "No cached astronomy data available")
        }
        
        return cachedData
    }
    
    /**
     * Fetch fresh astronomy data from API and cache it, with robust offline fallback
     * Returns AstronomyDataResult which indicates data freshness
     */
    suspend fun fetchAndCacheAstronomyData(latitude: Float, longitude: Float): AstronomyDataResult {
        // Check for cached data first
        val cachedData = astronomyDao.getAstronomyData()
        
        // Check network connectivity
        val isNetworkAvailable = NetworkConnectivityHelper.isNetworkAvailable(context)
        
        if (!isNetworkAvailable) {
            Logger.w(TAG, "Network unavailable, using cached data if available")
            return if (cachedData != null) {
                val age = System.currentTimeMillis() - cachedData.lastUpdated
                val isStale = !isDataValid(cachedData)
                Logger.i(TAG, "Using cached data (offline mode, age: ${age}ms, stale: $isStale)")
                AstronomyDataResult.Cached(cachedData, isStale, age)
            } else {
                Logger.e(TAG, "No network and no cached data available")
                AstronomyDataResult.Failure(Exception("Network unavailable and no cached data"))
            }
        }
        
        // Network is available, try to fetch fresh data
        return try {
            Logger.i(TAG, "Fetching astronomy data for location: $latitude, $longitude")
            
            val jsonData = fetchFromApi(latitude, longitude)
            
            if (jsonData != null) {
                val astronomyData = parseAstronomyData(jsonData, latitude, longitude)
                
                // Validate parsed data before caching
                if (validateAstronomyData(astronomyData)) {
                    astronomyDao.insertAstronomyData(astronomyData)
                    Logger.i(TAG, "Astronomy data cached successfully")
                    AstronomyDataResult.Fresh(astronomyData)
                } else {
                    Logger.w(TAG, "Fetched data failed validation, using cached data if available")
                    fallbackToCached(cachedData)
                }
            } else {
                Logger.w(TAG, "Failed to fetch astronomy data, falling back to cached data")
                fallbackToCached(cachedData)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching astronomy data", e)
            
            // Check if it's a network error
            if (NetworkConnectivityHelper.isNetworkError(e)) {
                Logger.w(TAG, "Network error detected, using cached data if available")
                fallbackToCached(cachedData)
            } else {
                // Other error, still try cached data as fallback
                fallbackToCached(cachedData, e)
            }
        }
    }
    
    /**
     * Fallback to cached data when fetch fails
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
                originalException ?: Exception("Failed to fetch astronomy data and no cache available")
            )
        }
    }
    
    /**
     * Legacy method for backward compatibility - returns Result<AstronomyDataEntity>
     * @deprecated Use fetchAndCacheAstronomyData() which returns AstronomyDataResult
     */
    @Deprecated("Use fetchAndCacheAstronomyData() which returns AstronomyDataResult with freshness info")
    suspend fun fetchAndCacheAstronomyDataLegacy(latitude: Float, longitude: Float): Result<AstronomyDataEntity> {
        val result = fetchAndCacheAstronomyData(latitude, longitude)
        return when (result) {
            is AstronomyDataResult.Fresh -> Result.success(result.data)
            is AstronomyDataResult.Cached -> Result.success(result.data)
            is AstronomyDataResult.Failure -> Result.failure(result.exception)
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
        // Check that all required fields are valid
        val now = System.currentTimeMillis()
        
        // Basic validation: times should be reasonable
        val validations: List<Boolean> = listOf(
            (data.sunrise > 0L),
            (data.sunset > 0L),
            (data.sunrise < data.sunset), // Sunrise should be before sunset
            (data.solarNoon > 0L),
            (data.civilDawn > 0L),
            (data.civilDusk > 0L),
            (data.civilDawn < data.sunrise), // Dawn before sunrise
            (data.sunset < data.civilDusk), // Sunset before dusk
            (data.dayLength > 0L),
            // Times should be within reasonable range (not too far in past/future)
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
            if (isStale) {
                Logger.d(TAG, "Returning stale cached data (age: ${age}ms)")
            } else {
                Logger.d(TAG, "Returning valid cached data (age: ${age}ms)")
            }
            AstronomyDataResult.Cached(cachedData, isStale, age)
        } else {
            Logger.d(TAG, "No cached astronomy data available")
            AstronomyDataResult.Failure(Exception("No cached astronomy data available"))
        }
    }
    
    /**
     * Fetch astronomy data from API with retries
     */
    private suspend fun fetchFromApi(latitude: Float, longitude: Float, maxRetries: Int = 3): JSONObject? {
        repeat(maxRetries) { attempt ->
            try {
                val urlString = "$API_URL?lat=$latitude&lng=$longitude&formatted=0"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    connection.disconnect()
                    
                    if (jsonObject.getString("status") == "OK") {
                        return jsonObject.getJSONObject("results")
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Logger.e(TAG, "Attempt ${attempt + 1} failed", e)
                
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(2000L * (attempt + 1))
                }
            }
        }
        
        return null
    }
    
    /**
     * Parse JSON data into AstronomyDataEntity
     */
    private fun parseAstronomyData(data: JSONObject, latitude: Float, longitude: Float): AstronomyDataEntity {
        val sunrise = parseTimeToMillis(data.getString("sunrise"))
        val sunset = parseTimeToMillis(data.getString("sunset"))
        
        // Get day length from API, or calculate from sunrise/sunset as fallback
        val dayLength = try {
            data.getLong("day_length") * AppConstants.MILLIS_PER_SECOND // Convert seconds to milliseconds
        } catch (e: Exception) {
            Logger.w(TAG, "Could not get day_length from API, calculating from sunrise/sunset", e)
            sunset - sunrise
        }
        
        return AstronomyDataEntity(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sunrise = sunrise,
            sunset = sunset,
            solarNoon = parseTimeToMillis(data.getString("solar_noon")),
            civilDawn = parseTimeToMillis(data.getString("civil_twilight_begin")),
            civilDusk = parseTimeToMillis(data.getString("civil_twilight_end")),
            nauticalDawn = parseTimeToMillis(data.getString("nautical_twilight_begin")),
            nauticalDusk = parseTimeToMillis(data.getString("nautical_twilight_end")),
            astroDawn = parseTimeToMillis(data.getString("astronomical_twilight_begin")),
            astroDusk = parseTimeToMillis(data.getString("astronomical_twilight_end")),
            lastUpdated = System.currentTimeMillis(),
            dayLength = dayLength
        )
    }
    
    /**
     * Parse ISO 8601 time string to milliseconds
     */
    private fun parseTimeToMillis(isoTime: String): Long {
        return try {
            java.time.Instant.parse(isoTime).toEpochMilli()
        } catch (e: Exception) {
            Logger.e(TAG, "Error parsing time: $isoTime", e)
            0L
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
