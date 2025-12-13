package com.rooster.rooster

import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.rooster.rooster.util.PermissionHelper
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.AnimationHelper
import com.rooster.rooster.worker.WorkManagerHelper


class MainActivity() : ComponentActivity() {
    private val REQUEST_CODE_PERMISSIONS = 4
    val coarseLocationPermissionRequestCode = 1
    val notificationPermissionRequestCode = 2
    val fullScreenIntentPermissionRequestCode = 3
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")
        
        // USE FIXED MAIN LAYOUT
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "Layout set")
        
        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            it.postDelayed({
                val alarmsListActivity = Intent(this, AlarmListActivity::class.java)
                startActivity(alarmsListActivity)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }, 150)
        }
        
        getPermissions()
        linkButtons()
        refreshCycle()
        animateViews()
        Log.d("MainActivity", "onCreate completed")
    }

    private fun getPermissions() {
        // Check and request overlay permission
        if (!PermissionHelper.isOverlayPermissionGranted(this)) {
            showOverlayPermissionPopup()
        }
        
        // Check and request exact alarm permission (Android 12+)
        if (!PermissionHelper.isExactAlarmPermissionGranted(this)) {
            showExactAlarmPermissionDialog()
        }
        
        // Request all other required permissions
        PermissionHelper.requestAllPermissions(this)
    }

    private fun showExactAlarmPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exact Alarm Permission Required")
        builder.setMessage("Rooster needs permission to schedule exact alarms to wake you up at the precise time.\n\nThis ensures your alarm goes off exactly when you want it to.")
        builder.setPositiveButton("Grant Permission") { dialog, _ ->
            PermissionHelper.openExactAlarmSettings(this)
            dialog.dismiss()
        }
        builder.setNegativeButton("Skip") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }


    private fun linkButtons() {
        val settingsButton = findViewById<View>(R.id.settingsButton)
        settingsButton?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            it.postDelayed({
                val settingsActivity = Intent(this, SettingsActivity::class.java)
                startActivity(settingsActivity)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }, 150)
        }
        
        val alarmsButton = findViewById<View>(R.id.alarmButton)
        alarmsButton?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            it.postDelayed({
                val alarmsListActivity = Intent(this, AlarmListActivity::class.java)
                startActivity(alarmsListActivity)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }, 150)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsActivity = Intent(this, SettingsActivity::class.java)
                startActivity(settingsActivity)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        val delayMillis = 1000L
        val maxProgress = 100

        updateRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val formattedTime = sdf.format(Date(currentTime))
                val percentage = getPercentageOfDay().toLong()
                
                progressText.text = formattedTime
                progressBar.progress = percentage.toInt()
                
                handler.postDelayed(this, delayMillis)
            }
        }

        updateRunnable?.let { handler.post(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.e("Rooster", "Permission Callback")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Schedule WorkManager tasks and trigger immediate updates
        WorkManagerHelper.scheduleLocationUpdates(this)
        WorkManagerHelper.scheduleAstronomyUpdates(this)
        WorkManagerHelper.triggerLocationUpdate(this)
        
        // Get the fused location provider for initial location
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000 // milliseconds
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.e("Rooster", "Location Callback")
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
            location?.let {
                sharedPreferences.edit()
                    .putFloat("altitude", it.altitude.toFloat())
                    .putFloat("longitude", it.longitude.toFloat())
                    .putFloat("latitude", it.latitude.toFloat())
                    .apply()
            }
        }
    }

    private fun showOverlayPermissionPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Display Over Other Apps Permission")
        builder.setMessage("Rooster needs this permission to:\n\n  • Display alarm screen when device is locked\n  • Show alarm controls\n  • Ensure you can dismiss alarms")
        builder.setPositiveButton("Grant Permission") { dialog, _ ->
            PermissionHelper.openOverlaySettings(this)
            dialog.dismiss()
        }
        builder.setNegativeButton("Skip") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler to prevent memory leaks
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
    
    override fun onPause() {
        super.onPause()
        // Stop updates when app is in background
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume updates when app comes to foreground
        updateRunnable?.let { handler.post(it) }
    }
    
    private fun animateViews() {
        val timeCard = findViewById<View>(R.id.timeCard)
        val infoCard = findViewById<View>(R.id.infoCard)
        
        timeCard?.let {
            it.alpha = 0f
            it.translationY = 30f
            it.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        infoCard?.let {
            it.alpha = 0f
            it.translationY = 30f
            it.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(100)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}