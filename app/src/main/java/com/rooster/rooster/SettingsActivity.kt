package com.rooster.rooster

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar


class SettingsActivity: AppCompatActivity() {

    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        setContentView(R.layout.activity_settings)
        linkButtons()
        updateValues()
    }

    private fun linkButtons() {
        val syncGPSButton = findViewById<TextView>(R.id.syncGpsTitle)
        syncGPSButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Log.e(TAG, "Manual Sync GPS")
                getLastKnownPosition()
            }
        })
    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001 // You can use any integer value

    private fun getLastKnownPosition() {
        // Check for location permission before requesting updates.
        if (isLocationPermissionGranted()) {
            // Permission is granted
            requestLocationUpdates()
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Override onRequestPermissionsResult to handle the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location updates
                requestLocationUpdates()
            } else {
                // Permission denied, handle this case
                Log.e("GPS Update", "Location permission denied")
            }
        }
    }

    // Method to start location updates
    private fun requestLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0, 0f, networkLocationListener
        )
    }


    fun pickTime(view: View, tgt: String) {
        // Request full screen intent permission
        requestFullScreenIntentPermission(this) { granted ->
            if (granted) {
                // Full screen intent permission is granted, so show the PopTime dialog
                Log.e("Rooster", "Full Screen Permission Granted")
                val popTime = PopTime(tgt)
                val fm = supportFragmentManager
                popTime.show(fm, "Select time")
            } else {
                // Full screen intent permission is not granted
                Log.e("Rooster", "Full Screen Not Permission Granted")
            }
        }
    }

    fun requestFullScreenIntentPermission(activity: Activity, callback: (Boolean) -> Unit) {
        // Check if full screen intent permission is granted
        val granted = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.USE_FULL_SCREEN_INTENT
        ) == PackageManager.PERMISSION_GRANTED

        // If full screen intent permission is not granted, request it
        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.USE_FULL_SCREEN_INTENT),
                0
            )
        } else {
            // Full screen intent permission is already granted
            callback(true)
        }
    }

    fun setTime(hour: Int, minute: Int, tgt: String) {
        val sharedPrefs = applicationContext.getSharedPreferences("RoosterPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val calender = Calendar.getInstance()
        calender.set(Calendar.HOUR_OF_DAY,hour)
        calender.set(Calendar.MINUTE,minute)
        calender.set(Calendar.SECOND,0)
        editor.putLong(tgt, calender.timeInMillis)
        val formattedTime = SimpleDateFormat("HH:mm").format(calender.time)
        Log.e("HOUR", calender.timeInMillis.toString())
        editor.apply()
    }

    private fun updateValues() {
        val sdf = SimpleDateFormat("HH:mm")
        val sharedPrefs = applicationContext.getSharedPreferences("rooster_prefs", MODE_PRIVATE)
        val astroSteps = arrayOf(
            "astroDawn",
            "nauticalDawn",
            "civilDawn",
            "sunrise",
            "sunset",
            "civilDusk",
            "nauticalDusk",
            "astroDusk",
            "solarNoon")

        var timeInMillis = 0L
        sdf.timeZone = TimeZone.getDefault()
        for (i in astroSteps) {
            val tvId = getResources().getIdentifier("$i"+"Value", "id", getPackageName());
            timeInMillis = sharedPrefs.getLong(i, 0)
            val formattedTime = getFormattedTime(timeInMillis)
            val tv = findViewById<TextView>(tvId)
            tv.text = formattedTime
        }

        var dayLength = sharedPrefs.getLong("dayLength", 0)
        val tv = findViewById<TextView>(R.id.dayLengthValue)
        dayLength = dayLength / 1000
        var dlHours = dayLength / (60 * 60)
        var dlMinutes = (dayLength / 60) % 60
        var dlHoursFmt = dlHours.toString()
        if (dlHours < 10) {
            dlHoursFmt = "0" + dlHoursFmt
        }
        var dlMinutesFmt = dlMinutes.toString()
        if (dlMinutes < 10) {
            dlMinutesFmt = "0" + dlMinutesFmt
        }
        tv.text = dlHoursFmt + ":" + dlMinutesFmt

        val coordinates = arrayOf(
            "altitude",
            "latitude",
            "longitude")
        for (i in coordinates) {
            val coordinate = sharedPrefs.getFloat(i, 0F)
            val tvId = getResources().getIdentifier("$i"+"Value", "id", getPackageName());
            val tv = findViewById<TextView>(tvId)
            tv.text = coordinate.toString()
        }
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
            updateValues()
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
    fun getFormattedTime(timeInSec: Long): CharSequence? {
        val fullDateFormat = SimpleDateFormat("HH:mm")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInSec // Convert seconds to milliseconds

        // Use the default time zone of the device
        val defaultTimeZone = TimeZone.getDefault()
        fullDateFormat.timeZone = defaultTimeZone

        // Consider daylight saving time (DST)
        if (defaultTimeZone.inDaylightTime(calendar.time)) {
            val dstOffsetInMillis = defaultTimeZone.dstSavings
            calendar.add(Calendar.MILLISECOND, dstOffsetInMillis)
        }

        return fullDateFormat.format(calendar.time)
    }

    fun redirectToGitHub(v: View?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thdelmas/Rooster"))
        startActivity(intent)
    }

    // Function to redirect to LinkedIn
    fun redirectToLinkedIn(v: View?) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.linkedin.com/in/th%C3%A9ophile-delmas-92275b16b/")
        )
        startActivity(intent)
    }

    // Function to redirect to Email
    fun redirectToEmail(v: View?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.setData(Uri.parse("mailto:contact@theophile.world"))
        startActivity(intent)
    }
}