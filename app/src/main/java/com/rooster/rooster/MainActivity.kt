package com.rooster.rooster

import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.util.Calendar
import java.util.Date
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.datatransport.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices


class MainActivity() : ComponentActivity() {
    private val REQUEST_CODE_PERMISSIONS = 4
    val coarseLocationPermissionRequestCode = 1
    val notificationPermissionRequestCode = 2
    val fullScreenIntentPermissionRequestCode = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermissions()
        linkButtons()
        refreshCycle()
    }

    private fun getPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(applicationContext)) {
                // Overlay permission is not granted, show a pop-up to request it
                showOverlayPermissionPopup()
            }
        }

        requestFullScreenIntentPermission(this) { granted ->
            if (granted) {
                // Full screen intent permission is granted, so show the PopTime dialog
                Log.e("Rooster", "Full Screen Permission Granted")
            } else {
                // Full screen intent permission is not granted
                Log.e("Rooster", "Full Screen Not Permission Granted")
            }
        }
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.USE_FULL_SCREEN_INTENT,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.SCHEDULE_EXACT_ALARM,
            Manifest.permission.FOREGROUND_SERVICE
            )

        val grantedPermissions = permissionsToRequest.filter {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (grantedPermissions.size < permissionsToRequest.size) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.filter { !grantedPermissions.contains(it) }.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
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


    private fun linkButtons() {
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val settingsActivity = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(settingsActivity);
        }
        val alarmsButton = findViewById<ImageButton>(R.id.alarmButton)
        alarmsButton.setOnClickListener {
            val alarmsListActivity = Intent(applicationContext, AlarmListActivity::class.java)
            startActivity(alarmsListActivity);
        }
    }

    fun getPercentageOfDay(): Float {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance()
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)

        val totalSeconds = ((now.timeInMillis - midnight.timeInMillis) / 1000).toFloat()
        val secondsInDay = 24 * 60 * 60
        val percentage = (totalSeconds / secondsInDay) * 100
        return percentage.toFloat()
    }
    private fun refreshCycle() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_cycle)
        val progressText = findViewById<TextView>(R.id.progress_text)
        val handler = Handler()
        val delayMillis = 1000
        val maxProgress = 100

        val updateRunnable = object : Runnable {
            var i = 0

            override fun run() {
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("HH:mm")
                val formattedTime = sdf.format(Date(currentTime))
                val percentage = getPercentageOfDay().toLong()
                if (percentage <= maxProgress) {
                    progressText.text = formattedTime
                    progressBar.progress = percentage.toInt()
                    handler.postDelayed(this, delayMillis.toLong())
                } else {
                    // Reset progress bar and text
                    progressBar.progress = 0
                    progressText.text = "00:00"
                    // Start the loop again
                    handler.postDelayed(this, delayMillis.toLong())
                }
            }
        }

        handler.post(updateRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.e("Rooster", "Permission Callback")
        // Get the fused location provider.
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location updates.
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000 // milliseconds
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val intentLocationService = Intent(this, LocationUpdateService::class.java)
        startService(intentLocationService)
        val intentAstronomyService = Intent(this, AstronomyUpdateService::class.java)
        startService(intentAstronomyService)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.e("Rooster", "Location Callback")
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putFloat("altitude", location.altitude.toFloat())
                .putFloat("longitude", location.longitude.toFloat())
                .putFloat("latitude", location.latitude.toFloat())
                .apply()
        }
    }

    private fun showOverlayPermissionPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Overlay Permission Required")
        builder.setMessage("This app need permissions to:\n\n  - Display alarms\n\n  - Stop alarms")
        builder.setPositiveButton("Open Settings") { dialog, which ->
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + applicationContext.packageName)
            )
            startActivityForResult(intent, 101)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            // Handle cancel button click if needed
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}