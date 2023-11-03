package com.rooster.rooster

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat

class LocationUpdateService : Service() {

    private val handler = Handler()
    private var locationManager: LocationManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delay = 3 * 60 * 60 * 1000
        //val delay = 1000
        getLastKnownPosition()
        Log.i("Rooster Location Service", "Started")
        // Post a runnable to get the last known position every 3 hours.
        handler.postDelayed({
            Log.i("Rooster Location Service", "Running")
            getLastKnownPosition()
        }, delay.toLong())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("Rooster Location Service", "Bind")
        TODO("Not yet implemented")
    }

    private fun getLastKnownPosition() {
        // Check for location permission before requesting updates.
        if (isLocationPermissionGranted()) {
            var lastLocataion = locationManager?.getLastKnownLocation("NETWORK")
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0, 0f, networkLocationListener
            )
        } else {
            // Handle the case where permission is not granted.
            Log.e("GPS Update", "Location permission not granted")
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        // Check if the location permission is granted.
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle the network location update here.
            Log.e("Network Location Update", location.toString())

            // Store the altitude, longitude, and latitude in shared prefs.
            val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putFloat("altitude", location.altitude.toFloat())
                .putFloat("longitude", location.longitude.toFloat())
                .putFloat("latitude", location.latitude.toFloat())
                .apply()

            val intent = Intent(applicationContext, AstronomyUpdateService::class.java)
            intent.putExtra("syncData", true)
            startService(intent)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    // Handle the permission request result in onRequestPermissionsResult method.


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}