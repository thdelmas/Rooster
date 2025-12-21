package com.rooster.rooster

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rooster.rooster.presentation.viewmodel.SettingsViewModel
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.ThemeHelper
import com.rooster.rooster.worker.AstronomyUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    
    private var locationManager: LocationManager? = null
    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
        setContentView(R.layout.activity_settings)
        
        // Setup toolbar navigation
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        linkButtons()
        setupThemeSettings()
        updateValues()
    }

    private fun linkButtons() {
        val syncGPSButton = findViewById<TextView>(R.id.syncGpsTitle)
        syncGPSButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            Log.i("SettingsActivity", "Manual Sync GPS")
            getLastKnownPosition()
        }
    }
    
    private fun setupThemeSettings() {
        // Theme mode selector
        val themeModeSetting = findViewById<LinearLayout>(R.id.themeModeSetting)
        val themeModeValue = findViewById<TextView>(R.id.themeModeValue)
        
        val currentTheme = ThemeHelper.getThemeMode(this)
        themeModeValue.text = ThemeHelper.getThemeModeName(currentTheme)
        
        themeModeSetting.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            showThemeDialog()
        }
        
        // Dynamic colors switch
        val dynamicColorsSwitch = findViewById<SwitchMaterial>(R.id.dynamicColorsSwitch)
        val dynamicColorsSetting = findViewById<LinearLayout>(R.id.dynamicColorsSetting)
        
        // Hide dynamic colors option if not supported
        if (!ThemeHelper.supportsDynamicColors()) {
            dynamicColorsSetting.visibility = View.GONE
        } else {
            dynamicColorsSwitch.isChecked = ThemeHelper.isDynamicColorsEnabled(this)
            dynamicColorsSwitch.setOnCheckedChangeListener { view, isChecked ->
                HapticFeedbackHelper.performToggleFeedback(view)
                ThemeHelper.setDynamicColorsEnabled(this, isChecked)
                // Restart activity to apply changes
                recreate()
            }
        }
    }
    
    private fun showThemeDialog() {
        val themes = arrayOf("Auto (System)", "Light", "Dark")
        val currentTheme = ThemeHelper.getThemeMode(this)
        
        AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                HapticFeedbackHelper.performSuccessFeedback(this)
                ThemeHelper.setThemeMode(this, which)
                val themeModeValue = findViewById<TextView>(R.id.themeModeValue)
                themeModeValue.text = ThemeHelper.getThemeModeName(which)
                dialog.dismiss()
                // Restart activity to apply theme
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("SettingsActivity", "Location permission granted")
                requestLocationUpdates()
            } else {
                Log.w("SettingsActivity", "Location permission denied")
            }
        }
    }

    // Method to start location updates
    private fun requestLocationUpdates() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0, 0f, networkLocationListener
                )
            }
        } catch (e: SecurityException) {
            Log.e("SettingsActivity", "Security exception requesting location updates", e)
        }
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
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        
        sharedPrefs.edit().apply {
            putLong(tgt, calendar.timeInMillis)
            apply()
        }
        
        val formattedTime = SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.time)
        Log.d("SettingsActivity", "Time set: $formattedTime (${calendar.timeInMillis})")
    }

    private fun updateValues() {
        val sdf = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
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
            "solarNoon"
        )

        sdf.timeZone = TimeZone.getDefault()
        for (step in astroSteps) {
            val tvId = resources.getIdentifier("${step}Value", "id", packageName)
            val timeInMillis = sharedPrefs.getLong(step, 0)
            val formattedTime = getFormattedTime(timeInMillis)
            val tv = findViewById<TextView>(tvId)
            tv?.text = formattedTime
        }

        val dayLength = sharedPrefs.getLong("dayLength", 0) / 1000
        val tv = findViewById<TextView>(R.id.dayLengthValue)
        val dlHours = dayLength / (60 * 60)
        val dlMinutes = (dayLength / 60) % 60
        tv?.text = String.format("%02d:%02d", dlHours, dlMinutes)

        // Read location from ViewModel (which uses Repository)
        activityScope.launch(Dispatchers.IO) {
            try {
                val location = viewModel.getLocation()
                launch(Dispatchers.Main) {
                    if (location != null) {
                        val coordinates = mapOf(
                            "altitude" to location.altitude,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        )
                        for ((coord, value) in coordinates) {
                            val tvId = resources.getIdentifier("${coord}Value", "id", packageName)
                            val coordTv = findViewById<TextView>(tvId)
                            coordTv?.text = String.format("%.4f", value)
                        }
                    } else {
                        // Fallback to SharedPreferences if no location in database
                        val coordinates = arrayOf("altitude", "latitude", "longitude")
                        for (coord in coordinates) {
                            val coordinate = sharedPrefs.getFloat(coord, 0F)
                            val tvId = resources.getIdentifier("${coord}Value", "id", packageName)
                            val coordTv = findViewById<TextView>(tvId)
                            coordTv?.text = String.format("%.4f", coordinate)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error reading location from database", e)
                // Fallback to SharedPreferences on error
                launch(Dispatchers.Main) {
                    val coordinates = arrayOf("altitude", "latitude", "longitude")
                    for (coord in coordinates) {
                        val coordinate = sharedPrefs.getFloat(coord, 0F)
                        val tvId = resources.getIdentifier("${coord}Value", "id", packageName)
                        val coordTv = findViewById<TextView>(tvId)
                        coordTv?.text = String.format("%.4f", coordinate)
                    }
                }
            }
        }
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.i("SettingsActivity", "Location updated: ${location.latitude}, ${location.longitude}")

            // Store the location through ViewModel (which uses Repository)
            viewModel.saveLocation(location)
            
            // Trigger astronomy data update using WorkManager
            activityScope.launch(Dispatchers.Main) {
                try {
                    val workRequest = OneTimeWorkRequestBuilder<AstronomyUpdateWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                    
                    // Update UI
                    updateValues()
                } catch (e: Exception) {
                    Log.e("SettingsActivity", "Error triggering astronomy update", e)
                    // Fallback to SharedPreferences if update fails
                    getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE).edit().apply {
                        putFloat("altitude", location.altitude.toFloat())
                        putFloat("longitude", location.longitude.toFloat())
                        putFloat("latitude", location.latitude.toFloat())
                        apply()
                    }
                    updateValues()
                }
            }
            
            // Remove location updates after successful update
            locationManager?.removeUpdates(this)
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d("SettingsActivity", "Provider status changed: $provider, status: $status")
        }
    }
    fun getFormattedTime(timeInSec: Long): String {
        if (timeInSec == 0L) return "--:--"
        
        val fullDateFormat = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInSec

        val defaultTimeZone = TimeZone.getDefault()
        fullDateFormat.timeZone = defaultTimeZone

        // Consider daylight saving time (DST)
        if (defaultTimeZone.inDaylightTime(calendar.time)) {
            val dstOffsetInMillis = defaultTimeZone.dstSavings
            calendar.add(Calendar.MILLISECOND, dstOffsetInMillis)
        }

        return fullDateFormat.format(calendar.time)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(networkLocationListener)
        activityJob.cancel()
    }

    fun redirectToGitHub(v: View?) {
        v?.let { HapticFeedbackHelper.performClick(it) }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thdelmas/Rooster"))
        startActivity(intent)
    }

    // Function to redirect to LinkedIn
    fun redirectToLinkedIn(v: View?) {
        v?.let { HapticFeedbackHelper.performClick(it) }
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.linkedin.com/in/th%C3%A9ophile-delmas-92275b16b/")
        )
        startActivity(intent)
    }

    // Function to redirect to Email
    fun redirectToEmail(v: View?) {
        v?.let { HapticFeedbackHelper.performClick(it) }
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.setData(Uri.parse("mailto:contact@theophile.world"))
        startActivity(intent)
    }
}