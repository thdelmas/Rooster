package com.rooster.rooster

import OpenWeatherAPI
import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.core.app.NotificationCompat

class MyService : Service() {

    private val SERVICE_RUN_INTERVAL: Long = (((4 * 60) + 20) * 60) * 1000 // ((Hours * 60) + Minutes) * 60 = Seconds
    //private val SERVICE_RUN_INTERVAL: Long = (((0 * 60) + 2) * 60) * 1000 // ((Hours * 60) + Minutes) * 60 = Seconds
    private val NOTIFICATION_ID = 1
    private var placeNameTextView: TextView? = null
    private var sunriseTimeTextView: TextView? = null
    private val serviceHandler = Handler(Looper.getMainLooper())
    private val serviceRunnable = object : Runnable {
        override fun run() {
            logSharedPrefs()
            serviceHandler.postDelayed(this, SERVICE_RUN_INTERVAL) // Run every 300 seconds (5 minutes)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.w("Rooster Service", "Service Started")
        startForeground(NOTIFICATION_ID, createNotification())
        startServiceExecution()
    }

    private fun startServiceExecution() {
        Log.d("Rooster Service", "Running Loop")
        serviceHandler.postDelayed(serviceRunnable, 4200) // Start after 4.2 Seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopServiceExecution()
    }

    private fun stopServiceExecution() {
        serviceHandler.removeCallbacks(serviceRunnable) // Stop the 5-minute service execution
    }

    private fun logSharedPrefs() {
        val sharedPrefs = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val latitude = sharedPrefs.getFloat("latitude", 0.0F).toDouble()
        val longitude = sharedPrefs.getFloat("longitude", 0.0F).toDouble()
        val storedSunriseTime = sharedPrefs.getLong("sunriseTimestamp", 0)
        val currentTimeMillis = System.currentTimeMillis()
        Log.d("Rooster Service", "Latitude: $latitude, Longitude: $longitude")
        Log.d("Rooster Service", "SunTime: $storedSunriseTime")
        Log.d("Rooster Service", "NowTime: $currentTimeMillis")


        if (storedSunriseTime < currentTimeMillis) {
            // Get the sunrise time
            Log.w("Rooster OpenWeatherAPI", "Fetching OpenWeatherData")
            val openWeatherAPI = OpenWeatherAPI(this)
            openWeatherAPI.fetchSunriseTime(latitude, longitude)
        } else {
            Log.w("Rooster OpenWeatherAPI", "Stored sunrise time is already in the future.")
            Log.d("Rooster OpenWeatherAPI", "Back to sleep")
        }
    }

    private fun createNotification(): Notification {
        val channelId = "MyServiceChannel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("My Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "My Service Channel", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        return notificationBuilder.build()
    }
}
