package com.rooster.rooster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AstronomyUpdateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var hasRunOnce = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("AstronomyUpdateService", "Service created")

        // Initial data retrieval
        serviceScope.launch {
            if (!hasRunOnce) {
                retrieveSunCourse()
                hasRunOnce = true
            }
            
            // Schedule periodic updates every 23 hours
            while (isActive) {
                delay(23 * 60 * 60 * 1000L) // 23 hours
                retrieveSunCourse()
            }
        }
    }

    private suspend fun retrieveSunCourse() {
        Log.i("AstronomyUpdateService", "Retrieving sun course data")
        
        // Retry up to 3 times with 60 second delays
        repeat(3) { attempt ->
            try {
                val apiResponse = getSunriseSunset()
                
                if (apiResponse != null) {
                    Log.i("AstronomyUpdateService", "API response received")
                    val results = parseResponse(apiResponse)
                    saveData(results)
                    return // Success, exit the function
                }
                
                Log.w("AstronomyUpdateService", "API response was null, attempt ${attempt + 1}/3")
            } catch (e: Exception) {
                Log.e("AstronomyUpdateService", "Error retrieving sun course, attempt ${attempt + 1}/3", e)
            }
            
            if (attempt < 2) {
                delay(60000) // Wait 60 seconds before retry
            }
        }
        
        Log.e("AstronomyUpdateService", "Failed to retrieve sun course data after 3 attempts")
    }

    private fun saveData(results: Map<String, String>) {
        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        try {
            // Save all astronomy data with proper null checks
            var sunriseMillis: Long? = null
            var sunsetMillis: Long? = null
            
            results["astronomical_twilight_begin"]?.let { 
                editor.putLong("astroDawn", parseDateTime(it))
            }
            results["nautical_twilight_begin"]?.let { 
                editor.putLong("nauticalDawn", parseDateTime(it))
            }
            results["civil_twilight_begin"]?.let { 
                editor.putLong("civilDawn", parseDateTime(it))
            }
            results["astronomical_twilight_end"]?.let { 
                editor.putLong("astroDusk", parseDateTime(it))
            }
            results["nautical_twilight_end"]?.let { 
                editor.putLong("nauticalDusk", parseDateTime(it))
            }
            results["civil_twilight_end"]?.let { 
                editor.putLong("civilDusk", parseDateTime(it))
            }
            results["sunrise"]?.let { 
                sunriseMillis = parseDateTime(it)
                editor.putLong("sunrise", sunriseMillis!!)
            }
            results["sunset"]?.let { 
                sunsetMillis = parseDateTime(it)
                editor.putLong("sunset", sunsetMillis!!)
            }
            results["solar_noon"]?.let { 
                editor.putLong("solarNoon", parseDateTime(it))
            }
            
            // Get day length from API, or calculate from sunrise/sunset as fallback
            val dayLengthMillis = try {
                results["day_length"]?.let { 
                    it.toLong() * 1000
                } ?: if (sunriseMillis != null && sunsetMillis != null) {
                    sunsetMillis!! - sunriseMillis!!
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w("AstronomyUpdateService", "Could not parse day_length, calculating from sunrise/sunset", e)
                if (sunriseMillis != null && sunsetMillis != null) {
                    sunsetMillis!! - sunriseMillis!!
                } else {
                    null
                }
            }
            
            dayLengthMillis?.let {
                editor.putLong("dayLength", it)
            }
            
            editor.apply()
            Log.i("AstronomyUpdateService", "Astronomy data saved successfully")
        } catch (e: Exception) {
            Log.e("AstronomyUpdateService", "Error saving astronomy data", e)
        }
    }

    private fun parseDateTime(dateTimeString: String): Long {
        val formattedDateTimeString = dateTimeString.replace("\"", "")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault())
        val dateTime = dateFormat.parse(formattedDateTimeString)
        return dateTime?.time ?: 0L
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i("AstronomyUpdateService", "Service destroyed")
    }

    private suspend fun getSunriseSunset(): String? = withContext(Dispatchers.IO) {
        val sharedPrefs = applicationContext.getSharedPreferences("rooster_prefs", MODE_PRIVATE)
        val lat = sharedPrefs.getFloat("latitude", 0F)
        val lng = sharedPrefs.getFloat("longitude", 0F)
        
        if (lat == 0F && lng == 0F) {
            Log.w("AstronomyUpdateService", "No location data available")
            return@withContext null
        }
        
        val latStr = "%.4f".format(lat)
        val lngStr = "%.4f".format(lng)
        
        var connection: HttpsURLConnection? = null
        try {
            val url = URL("https://api.sunrise-sunset.org/json?lat=$latStr&lng=$lngStr&date=today&formatted=0")
            connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000
            
            val response = connection.inputStream.reader().readText()
            Log.d("AstronomyUpdateService", "API call successful for location: $latStr, $lngStr")
            return@withContext response
        } catch (e: Exception) {
            Log.e("AstronomyUpdateService", "Error fetching sunrise/sunset data", e)
            return@withContext null
        } finally {
            connection?.disconnect()
        }
    }

    fun parseResponse(response: String): Map<String, String> {
        val results = HashMap<String, String>()
        val regex = Regex("""\s*"([^"]+)":\s*("(.*?)"|(\d+))""")
        for (match in regex.findAll(response)) {
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            results[key] = value
        }
        return results
    }
}
