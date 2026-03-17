package com.rooster.rooster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.SolarCalculator
import java.util.Calendar

/**
 * Legacy service for astronomy data updates.
 * Now uses on-device SolarCalculator instead of API calls.
 */
class AstronomyUpdateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var hasRunOnce = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("AstronomyUpdateService", "Service created")

        serviceScope.launch {
            if (!hasRunOnce) {
                updateSunCourse()
                hasRunOnce = true
            }

            while (isActive) {
                delay(AppConstants.ASTRONOMY_UPDATE_INTERVAL_MS)
                updateSunCourse()
            }
        }
    }

    private fun updateSunCourse() {
        Logger.i("AstronomyUpdateService", "Calculating sun course data")

        try {
            val sharedPrefs = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
            val lat = sharedPrefs.getFloat("latitude", 0F)
            val lng = sharedPrefs.getFloat("longitude", 0F)

            if (lat == 0F && lng == 0F) {
                Logger.w("AstronomyUpdateService", "No location data available")
                return
            }

            val data = SolarCalculator.calculate(lat, lng)

            sharedPrefs.edit()
                .putLong("sunrise", data.sunrise)
                .putLong("sunset", data.sunset)
                .putLong("solarNoon", data.solarNoon)
                .putLong("civilDawn", data.civilDawn)
                .putLong("civilDusk", data.civilDusk)
                .putLong("nauticalDawn", data.nauticalDawn)
                .putLong("nauticalDusk", data.nauticalDusk)
                .putLong("astroDawn", data.astroDawn)
                .putLong("astroDusk", data.astroDusk)
                .putLong("dayLength", data.dayLength)
                .apply()

            Logger.i("AstronomyUpdateService", "Astronomy data calculated and saved")
        } catch (e: Exception) {
            Logger.e("AstronomyUpdateService", "Error calculating sun course", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Logger.i("AstronomyUpdateService", "Service destroyed")
    }
}
