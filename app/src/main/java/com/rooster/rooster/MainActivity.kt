package com.rooster.rooster

import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import com.rooster.rooster.util.Logger
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
import com.rooster.rooster.presentation.viewmodel.MainViewModel
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity() : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private val REQUEST_CODE_PERMISSIONS = 4
    val coarseLocationPermissionRequestCode = 1
    val notificationPermissionRequestCode = 2
    val fullScreenIntentPermissionRequestCode = 3
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("MainActivity", "onCreate started")
        
        // Check critical permissions first - app cannot function without them
        if (!checkCriticalPermissions()) {
            // App will exit after showing dialog
            return
        }
        
        // USE FIXED MAIN LAYOUT
        setContentView(R.layout.activity_main)
        Logger.d("MainActivity", "Layout set")
        
        getPermissions()
        linkButtons()
        refreshCycle()
        animateViews()
        Logger.d("MainActivity", "onCreate completed")
    }
    
    /**
     * Check if critical permissions are granted. If not, show dialog and exit app.
     * @return true if critical permissions are granted, false otherwise (app will exit)
     */
    private fun checkCriticalPermissions(): Boolean {
        if (!PermissionHelper.areCriticalPermissionsGranted(this)) {
            val missingPermissions = PermissionHelper.getMissingCriticalPermissions(this)
            val permissionNames = missingPermissions.joinToString(", ") { 
                PermissionHelper.getPermissionName(it) 
            }
            
            Logger.e("MainActivity", "CRITICAL: Missing required permissions: $permissionNames")
            Logger.e("MainActivity", "App cannot function without these permissions. Exiting.")
            
            showCriticalPermissionDialog(permissionNames)
            return false
        }
        
        Logger.i("MainActivity", "All critical permissions granted")
        return true
    }
    
    /**
     * Show dialog explaining that app cannot work without critical permissions
     */
    private fun showCriticalPermissionDialog(missingPermissions: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Required Permission Missing")
        builder.setMessage(
            "Rooster cannot function without the following permission:\n\n" +
            "• $missingPermissions\n\n" +
            "This permission is required for alarms to work properly. " +
            "Without it, your alarms will not fire.\n\n" +
            "Please grant this permission in settings, then restart the app."
        )
        builder.setPositiveButton("Open Settings") { dialog, _ ->
            // Open exact alarm settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionHelper.openExactAlarmSettings(this)
            } else {
                PermissionHelper.openAppSettings(this)
            }
            dialog.dismiss()
            // Exit app after a short delay to allow user to see settings
            handler.postDelayed({
                finishAffinity()
                System.exit(0)
            }, 500)
        }
        builder.setNegativeButton("Exit App") { dialog, _ ->
            dialog.dismiss()
            finishAffinity()
            System.exit(0)
        }
        builder.setCancelable(false)
        builder.setOnDismissListener {
            // Ensure app exits even if dialog is dismissed
            finishAffinity()
            System.exit(0)
        }
        builder.show()
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
        
        // Check location permission and show rationale if needed
        if (!PermissionHelper.isLocationPermissionGranted(this)) {
            if (shouldShowLocationRationale()) {
                showLocationPermissionRationale()
            } else {
                PermissionHelper.requestAllPermissions(this)
            }
        } else {
            // Permission already granted, request location updates
            requestLocationUpdatesIfPermitted()
            // Request other permissions if needed
            PermissionHelper.requestAllPermissions(this)
        }
    }
    
    private fun shouldShowLocationRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    private fun showLocationPermissionRationale() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
        builder.setMessage("Rooster needs location access to:\n\n  • Calculate sunrise and sunset times\n  • Provide accurate astronomy-based alarms\n  • Adjust alarms based on your location\n\nYour location data is only used locally and never shared.")
        builder.setPositiveButton("Grant Permission") { dialog, _ ->
            PermissionHelper.requestAllPermissions(this)
            dialog.dismiss()
        }
        builder.setNegativeButton("Skip") { dialog, _ ->
            dialog.dismiss()
            Logger.w("MainActivity", "User skipped location permission")
        }
        builder.setCancelable(false)
        builder.show()
    }
    
    private fun requestLocationUpdatesIfPermitted() {
        // Verify permission is granted before requesting location updates
        if (!PermissionHelper.isLocationPermissionGranted(this)) {
            Logger.w("MainActivity", "Location permission not granted, cannot request location updates")
            return
        }
        
        try {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 10000 // milliseconds
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Logger.i("MainActivity", "Location updates requested successfully")
        } catch (e: SecurityException) {
            Logger.e("MainActivity", "SecurityException when requesting location updates", e)
            Toast.makeText(this, "Location permission error. Please grant location permission in settings.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Logger.e("MainActivity", "Error requesting location updates", e)
        }
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

        val totalSeconds = ((now.timeInMillis - midnight.timeInMillis) / com.rooster.rooster.util.AppConstants.MILLIS_PER_SECOND).toFloat()
        val percentage = (totalSeconds / com.rooster.rooster.util.AppConstants.SECONDS_PER_DAY) * 100
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
        Logger.i("MainActivity", "Permission callback for request code: $requestCode")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (grantResults.isEmpty()) {
            Logger.w("MainActivity", "Permission request cancelled or empty results")
            return
        }
        
        // Check if location permissions were requested and granted
        val locationPermissionsRequested = permissions.any { permission ->
            permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
            permission == Manifest.permission.ACCESS_FINE_LOCATION
        }
        
        if (locationPermissionsRequested) {
            // Verify location permissions were actually granted by checking grantResults
            val locationPermissionIndices = permissions.mapIndexedNotNull { index, permission ->
                if (permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
                    permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    index
                } else {
                    null
                }
            }
            
            val locationPermissionsGranted = locationPermissionIndices.isNotEmpty() &&
                locationPermissionIndices.all { index ->
                    index < grantResults.size && grantResults[index] == PackageManager.PERMISSION_GRANTED
                }
            
            // Double-check with system permission check for security
            val hasCoarseLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val systemPermissionGranted = hasCoarseLocation || hasFineLocation
            
            if (locationPermissionsGranted && systemPermissionGranted) {
                Logger.i("MainActivity", "Location permissions granted, scheduling updates")
                // Schedule WorkManager tasks and trigger immediate updates
                WorkManagerHelper.scheduleLocationUpdates(this)
                WorkManagerHelper.scheduleAstronomyUpdates(this)
                WorkManagerHelper.triggerLocationUpdate(this)
                
                // Request location updates with proper permission check
                requestLocationUpdatesIfPermitted()
            } else {
                Logger.w("MainActivity", "Location permissions denied, location features may not work")
                // Check if user permanently denied (should not show rationale)
                val permanentlyDenied = !shouldShowLocationRationale() && !systemPermissionGranted
                
                if (permanentlyDenied) {
                    // Show dialog to guide user to settings
                    showLocationPermissionDeniedDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for astronomy-based alarms.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showLocationPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
        builder.setMessage("Location permission was denied. To use astronomy-based alarms, please grant location permission in app settings.")
        builder.setPositiveButton("Open Settings") { dialog, _ ->
            PermissionHelper.openAppSettings(this)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)
        builder.show()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Logger.i("MainActivity", "Location Callback")
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            location?.let {
                // Save location through ViewModel (which uses Repository)
                viewModel.saveLocation(
                    it.latitude.toFloat(),
                    it.longitude.toFloat(),
                    it.altitude.toFloat()
                )
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
        
        // Check critical permissions again in case user granted them in settings
        if (!PermissionHelper.areCriticalPermissionsGranted(this)) {
            // If still missing, show dialog again
            val missingPermissions = PermissionHelper.getMissingCriticalPermissions(this)
            val permissionNames = missingPermissions.joinToString(", ") { 
                PermissionHelper.getPermissionName(it) 
            }
            showCriticalPermissionDialog(permissionNames)
            return
        }
        
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