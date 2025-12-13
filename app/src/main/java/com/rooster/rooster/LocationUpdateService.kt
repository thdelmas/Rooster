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
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*

class LocationUpdateService : Service() {

    private var locationManager: LocationManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delay = 3 * 60 * 60 * 1000L // 3 hours
        
        getLastKnownPosition()
        Log.i("LocationUpdateService", "Started")
        
        // Use coroutines for periodic updates
        serviceScope.launch {
            while (isActive) {
                delay(delay)
                Log.i("LocationUpdateService", "Running periodic update")
                getLastKnownPosition()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("Rooster Location Service", "Bind")
        // This service is not designed to be bound, return null
        return null
    }

    private fun getLastKnownPosition() {
        // Check for location permission before requesting updates.
        if (!isLocationPermissionGranted()) {
            Log.e("LocationUpdateService", "Location permission not granted")
            return
        }
        
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            
            locationManager?.let { manager ->
                // Try to get last known location first
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val lastLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    lastLocation?.let { updateLocation(it) }
                }
                
                // Request new location updates
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0, 0f, networkLocationListener
                )
            }
        } catch (e: SecurityException) {
            Log.e("LocationUpdateService", "Security exception getting location", e)
        }
    }
    
    private fun updateLocation(location: Location) {
        Log.i("LocationUpdateService", "Location updated: ${location.latitude}, ${location.longitude}")
        
        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("altitude", location.altitude.toFloat())
            .putFloat("longitude", location.longitude.toFloat())
            .putFloat("latitude", location.latitude.toFloat())
            .apply()
    }

    private fun isLocationPermissionGranted(): Boolean {
        // Check if the location permission is granted.
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.i("LocationUpdateService", "Location changed: $location")
            updateLocation(location)

            // Trigger astronomy data update
            val intent = Intent(applicationContext, AstronomyUpdateService::class.java)
            intent.putExtra("syncData", true)
            startService(intent)
            
            // Remove location updates after successful update to save battery
            locationManager?.removeUpdates(this)
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d("LocationUpdateService", "Provider status changed: $provider, status: $status")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(networkLocationListener)
        serviceScope.cancel()
        Log.i("LocationUpdateService", "Service destroyed")
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}