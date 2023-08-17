import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.rooster.myapplication.AlarmHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

class OpenWeatherAPI(private val context: Context) {
    private val apiKey = "76fd89af987189f1a3f1aa84bc06fff1" // Replace with your actual API key
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)

    fun fetchSunriseTime(latitude: Double, longitude: Double) {
        val currentTimeMillis = System.currentTimeMillis()
        val apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"
        var response = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                response = URL(apiUrl).readText()
                val placeName = parsePlaceName(response)
                val sunriseTime = parseSunriseTime(response) ?: run {
                    Log.e("Rooster OpenWeather","Sunrise time is not in the response.")
                    return@launch
                }
                if (sunriseTime > currentTimeMillis) {
                    setSunriseAlarm(sunriseTime) // Convert to milliseconds
                    saveOpenWeatherToPrefs(sunriseTime, placeName)
                    Log.d("Rooster OpenWeather", "Sun will rise at $sunriseTime in $placeName")
                } else {
                    Log.d("OpenWeatherAPI", "Sunrise time is in the past for $placeName.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Rooster OpenWeather","Error fetching data for latitude=$latitude, longitude=$longitude. Response body: $response")
            }
        }
    }

    private fun parsePlaceName(response: String): String {
        return try {
            val jsonResponse = JSONObject(response)
            val name = jsonResponse.optString("name", "Unknown")
            name
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    private fun parseSunriseTime(response: String): Long? {
        return try {
            val jsonResponse = JSONObject(response)
            val sysObject = jsonResponse.optJSONObject("sys")
            val sunriseTimestamp = sysObject?.optLong("sunrise") ?: 0
            sunriseTimestamp * 1000
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setSunriseAlarm(timeInMillis: Long) {
        val alarmManagerHelper = AlarmHelper(context)
        alarmManagerHelper.setSunriseAlarm(timeInMillis)
    }

    private fun saveOpenWeatherToPrefs(sunriseTime: Long, locationName: String) {
        val editor = sharedPreferences.edit()

        editor.putLong("sunriseTimestamp", sunriseTime)
        editor.putString("locationName", locationName)
        editor.apply()
    }
}
