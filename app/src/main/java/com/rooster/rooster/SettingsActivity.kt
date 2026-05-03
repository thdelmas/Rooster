package com.rooster.rooster

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rooster.rooster.presentation.compose.AstronomyTimes
import com.rooster.rooster.presentation.compose.CoordinatesUi
import com.rooster.rooster.presentation.compose.SettingsScreen
import com.rooster.rooster.presentation.viewmodel.SettingsViewModel
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.SupportPromptDialog
import com.rooster.rooster.util.ThemeHelper
import com.rooster.rooster.worker.AstronomyUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private var locationManager: LocationManager? = null
    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)

    private val coordinatesState = MutableStateFlow(CoordinatesUi(null, null, null))
    private val astronomyState = MutableStateFlow(emptyAstronomy())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshState()

        setContent {
            RoosterTheme {
                val coordinates by coordinatesState.asStateFlow().collectAsState()
                val astronomy by astronomyState.asStateFlow().collectAsState()

                SettingsScreen(
                    coordinates = coordinates,
                    astronomy = astronomy,
                    onNavigateBack = ::handleBack,
                    onSyncGps = ::startGpsSync,
                    onOpenCredits = ::openCredits,
                    onOpenSupport = ::openSupport,
                    onOpenLinkedIn = ::openLinkedIn,
                    onOpenGitHub = ::openGitHub,
                    onOpenEmail = ::openEmail,
                    onDynamicColorsChanged = ::onDynamicColorsChanged,
                )
            }
        }
    }

    private fun handleBack() {
        @Suppress("DEPRECATION")
        onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun openCredits() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        startActivity(Intent(this, CreditsActivity::class.java))
    }

    private fun openSupport() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        SupportPromptDialog.showFromSettings(this)
    }

    private fun openLinkedIn() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.linkedin.com/in/th%C3%A9ophile-delmas-92275b16b/"),
            ),
        )
    }

    private fun openGitHub() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thdelmas/Rooster")))
    }

    private fun openEmail() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        startActivity(
            Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:contact@theophile.world") },
        )
    }

    private fun onDynamicColorsChanged(enabled: Boolean) {
        ThemeHelper.setDynamicColorsEnabled(this, enabled)
        recreate()
    }

    private fun startGpsSync() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        if (isLocationPermissionGranted()) {
            requestLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Logger.i("SettingsActivity", "Location permission granted")
            requestLocationUpdates()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Logger.w("SettingsActivity", "Location permission denied")
        }
    }

    private fun requestLocationUpdates() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0f,
                    networkLocationListener,
                )
            }
        } catch (e: SecurityException) {
            Logger.e("SettingsActivity", "Security exception requesting location updates", e)
        }
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Logger.i("SettingsActivity", "Location updated: ${location.latitude}, ${location.longitude}")
            viewModel.saveLocation(location)
            activityScope.launch(Dispatchers.Main) {
                try {
                    val workRequest = OneTimeWorkRequestBuilder<AstronomyUpdateWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                } catch (e: Exception) {
                    Logger.e("SettingsActivity", "Error triggering astronomy update", e)
                    getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE).edit().apply {
                        putFloat("altitude", location.altitude.toFloat())
                        putFloat("longitude", location.longitude.toFloat())
                        putFloat("latitude", location.latitude.toFloat())
                        apply()
                    }
                }
                refreshState()
            }
            locationManager?.removeUpdates(this)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Logger.d("SettingsActivity", "Provider status changed: $provider, status: $status")
        }
    }

    private fun refreshState() {
        astronomyState.value = readAstronomy()
        activityScope.launch(Dispatchers.IO) {
            val location = runCatching { viewModel.getLocation() }.getOrNull()
            if (location != null) {
                coordinatesState.value = CoordinatesUi(
                    altitude = location.altitude.toDouble(),
                    latitude = location.latitude.toDouble(),
                    longitude = location.longitude.toDouble(),
                )
            } else {
                val prefs = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
                coordinatesState.value = CoordinatesUi(
                    altitude = prefs.getFloat("altitude", 0f).toDouble(),
                    latitude = prefs.getFloat("latitude", 0f).toDouble(),
                    longitude = prefs.getFloat("longitude", 0f).toDouble(),
                )
            }
        }
    }

    private fun readAstronomy(): AstronomyTimes {
        val prefs = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        return AstronomyTimes(
            astroDawn = prefs.getLong("astroDawn", 0),
            nauticalDawn = prefs.getLong("nauticalDawn", 0),
            civilDawn = prefs.getLong("civilDawn", 0),
            sunrise = prefs.getLong("sunrise", 0),
            solarNoon = prefs.getLong("solarNoon", 0),
            sunset = prefs.getLong("sunset", 0),
            civilDusk = prefs.getLong("civilDusk", 0),
            nauticalDusk = prefs.getLong("nauticalDusk", 0),
            astroDusk = prefs.getLong("astroDusk", 0),
            dayLengthMillis = prefs.getLong("dayLength", 0),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(networkLocationListener)
        activityJob.cancel()
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun emptyAstronomy() = AstronomyTimes(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
}
