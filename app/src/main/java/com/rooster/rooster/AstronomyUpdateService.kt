package com.rooster.rooster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import java.net.URL
import java.util.Timer
import java.util.TimerTask
import javax.net.ssl.HttpsURLConnection

class AstronomyUpdateService : Service() {

    private val handlerThread = HandlerThread("AstronomyUpdateService")
    private val handler: Handler
    private val thread: Thread
    private var hasRunOnce = false

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        thread = Thread(Runnable {
            if (!hasRunOnce) {
                retrieveSunCourse()
                hasRunOnce = true
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Start the thread
        thread.start()

        // Schedule the retrieveSunCourse() function to run every 23 hours
        handler.postDelayed({ retrieveSunCourse() }, (23 * 60 * 60 * 1000).toLong())
    }

    private fun retrieveSunCourse() {
        Log.i("Rooster AstronomyUpdateService", "Running")
        var apiResponse: String? = null

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                apiResponse = getSunriseSunset()
                if (apiResponse == null) {
                    return
                }

                Log.i("Rooster AstronomyUpdateService", apiResponse!!)
                val results = parseResponse(apiResponse!!)
                saveData(results)

                timer.cancel()
            }
        }, 0, 60000)
    }

    private fun saveData(results: Map<String, String>) {
        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        val astroDawnTimestamp = results["astronomical_twilight_begin"]?.let { parseDateTime(it) }
        if (astroDawnTimestamp != null) {
            editor.putLong("astroDawn", astroDawnTimestamp)
        }
        val nauticalDawnTimestamp = results["nautical_twilight_begin"]?.let { parseDateTime(it) }
        if (nauticalDawnTimestamp != null) {
            editor.putLong("nauticalDawn", nauticalDawnTimestamp)
        }
        val civilDawnTimestamp = results["civil_twilight_begin"]?.let { parseDateTime(it) }
        if (civilDawnTimestamp != null) {
            editor.putLong("civilDawn", civilDawnTimestamp)
        }
        val astroDuskTimestamp = results["astronomical_twilight_end"]?.let { parseDateTime(it) }
        if (astroDuskTimestamp != null) {
            editor.putLong("astroDusk", astroDuskTimestamp)
        }
        val nauticalDuskTimestamp = results["nautical_twilight_end"]?.let { parseDateTime(it) }
        if (nauticalDuskTimestamp != null) {
            editor.putLong("nauticalDusk", nauticalDuskTimestamp)
        }
        val civilDuskTimestamp = results["civil_twilight_end"]?.let { parseDateTime(it) }
        if (civilDuskTimestamp != null) {
            editor.putLong("civilDusk", civilDuskTimestamp)
        }
        val sunrise = results["sunrise"]?.let { parseDateTime(it) }
        if (sunrise != null) {
            editor.putLong("sunrise", sunrise)
        }
        val sunset = results["sunset"]?.let { parseDateTime(it) }
        if (sunset != null) {
            editor.putLong("sunset", sunset)
        }
        val solarNoon = results["solar_noon"]?.let { parseDateTime(it) }
        if (solarNoon != null) {
            editor.putLong("solarNoon", solarNoon)
        }
        val dayLength = results["day_length"]
        if (dayLength != null) {
            var dlMillis = dayLength.toLong() * 1000
            editor.putLong("dayLength", dlMillis)
        }
        editor.apply()
        ///////////////////////////////////////////////////

        ///////////////////////////////////////////////////
    }

    private fun parseDateTime(dateTimeString: String): Long {
        val formattedDateTimeString = dateTimeString.replace("\"", "")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        val dateTime = dateFormat.parse(formattedDateTimeString)
        return dateTime.time
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }

    private fun getSunriseSunset(): String? {
        val sharedPrefs = applicationContext.getSharedPreferences("rooster_prefs", MODE_PRIVATE)
        var lat = sharedPrefs.getFloat("latitude", 0F)
        var lng = sharedPrefs.getFloat("longitude", 0F)
        if (lat == 0F && lng == 0F) {
            return null
        }
        var latStr = "%.4f".format(lat)
        var lngStr = "%.4f".format(lng)
        // Create a URL object to the sunrise-sunset API
        val url = URL("https://api.sunrise-sunset.org/json?lat=$latStr&lng=$lngStr&date=today&formatted=0")

        // Open a connection to the API
        val connection = url.openConnection() as HttpsURLConnection

        // Set the request method to GET
        connection.requestMethod = "GET"

        // Read the response from the API
        val response = connection.inputStream.reader().readText()

        // Close the connection
        connection.disconnect()

        return response
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
