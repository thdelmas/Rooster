package com.rooster.rooster.data.repository

import com.rooster.rooster.data.local.dao.AstronomyDao
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for astronomy data with offline caching
 */
@Singleton
class AstronomyRepository @Inject constructor(
    private val astronomyDao: AstronomyDao
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
            Logger.d(TAG, "Returning stale cached astronomy data (age: ${System.currentTimeMillis() - cachedData.lastUpdated}ms)")
        } else {
            Logger.d(TAG, "No cached astronomy data available")
        }
        
        return cachedData
    }
    
    /**
     * Fetch fresh astronomy data from API and cache it
     */
    suspend fun fetchAndCacheAstronomyData(latitude: Float, longitude: Float): Result<AstronomyDataEntity> {
        return try {
            Logger.i(TAG, "Fetching astronomy data for location: $latitude, $longitude")
            
            val jsonData = fetchFromApi(latitude, longitude)
                ?: return Result.failure(Exception("Failed to fetch astronomy data"))
            
            val astronomyData = parseAstronomyData(jsonData, latitude, longitude)
            astronomyDao.insertAstronomyData(astronomyData)
            
            Logger.i(TAG, "Astronomy data cached successfully")
            Result.success(astronomyData)
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching astronomy data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if cached data is still valid
     */
    private fun isDataValid(data: AstronomyDataEntity): Boolean {
        val age = System.currentTimeMillis() - data.lastUpdated
        return age < AppConstants.ASTRONOMY_DATA_VALIDITY_MS
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
