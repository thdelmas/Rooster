package com.rooster.rooster

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.icu.util.Calendar
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity() : ComponentActivity(), LocationListener {
    private val coarseLocationPermissionRequestCode = 1001
    private var mondayButton: Button? = null
    private var tuesdayButton: Button? = null
    private var wednesdayButton: Button? = null
    private var thursdayButton: Button? = null
    private var fridayButton: Button? = null
    private var saturdayButton: Button? = null
    private var sundayButton: Button? = null
    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationListener = this
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setContentView(R.layout.activity_main)
        mondayButton = findViewById(R.id.mondayButton);
        tuesdayButton = findViewById(R.id.tuesdayButton);
        wednesdayButton = findViewById(R.id.wednesdayButton);
        thursdayButton = findViewById(R.id.thursdayButton);
        fridayButton = findViewById(R.id.fridayButton);
        saturdayButton = findViewById(R.id.saturdayButton);
        sundayButton = findViewById(R.id.sundayButton);

        loadDaysStates()
        Log.w("Rooster", "MainActivity Started")
        val savedCoordinatesWithSunrise = getSavedCoordinatesFromPrefs()
        if (savedCoordinatesWithSunrise != null) {
            val latitude = savedCoordinatesWithSunrise.first
            val longitude = savedCoordinatesWithSunrise.second
            val sunriseTimestamp = savedCoordinatesWithSunrise.third
            val coordinatesEditText = findViewById<EditText>(R.id.coordinatesEditText)
            val sunriseEditText = findViewById<EditText>(R.id.sunriseTimeEditText)
            val locationEditText = findViewById<EditText>(R.id.locationNameEditText)


            // Update the EditText field with the saved coordinates
            coordinatesEditText.setText("GPS Coordinates\nLa: $latitude\nLo: $longitude")
            if (sunriseTimestamp > 0) {
                // Convert the sunrise timestamp to a human-readable format
                val calendar = Calendar.getInstance()
                calendar.setTimeInMillis(sunriseTimestamp)
                val dateFormat = SimpleDateFormat("hh:mm a\n(EEE, MMM dd, yyyy)", Locale.getDefault())
                val formattedSunriseTime = dateFormat.format(calendar.time)
                sunriseEditText.setText("Sunrise\n$formattedSunriseTime")
                val locationName = getLocationNameFromPrefs()
                if (!locationName.isNullOrEmpty()) {
                    locationEditText.setText("Location\n$locationName")
                } else {
                    locationEditText.setText("Location\nNot available")
                }
            } else {
                sunriseEditText.setText("Sunrise\nNot available")
            }
            startMyService()
        }
    }

    fun onGetCoordinatesButtonClicked(v: View) {
        // Check for location permission and request if necessary
        Log.w("Rooster", "Requesting GPS")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                coarseLocationPermissionRequestCode
            )
            Toast.makeText(this, "Please enable GPS and try again", Toast.LENGTH_LONG).show()
        } else {
            // Request location updates every 10 seconds with high accuracy
            Log.w("Rooster", "Requesting GPS Updates")
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                3000,
                0f, // Minimum distance between updates (set to 0 for any movement)
                locationListener
            )
        }
    }

    private fun startMyService() {
        val serviceIntent = Intent(this, MyService::class.java)
        startService(serviceIntent)
    }
        private fun isCoarseLocationPermissionGranted(): Boolean {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Request fine location permission
        private fun requestCoarseLocationPermission() {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                coarseLocationPermissionRequestCode
            )
        }

    fun refreshSunrise(v: View) {
        val sharedPrefs = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val storedSunriseTime = sharedPrefs.getLong("sunriseTimestamp", 0)
        val storedPlaceName = sharedPrefs.getString("locationName", "")
        val sunriseEditText = findViewById<EditText>(R.id.sunriseTimeEditText)
        val locationEditText = findViewById<EditText>(R.id.locationNameEditText)
        if (storedSunriseTime > 0) {
            val calendar = Calendar.getInstance()
            calendar.setTimeInMillis(storedSunriseTime)
            val dateFormat = SimpleDateFormat("hh:mm a\n(EEE, MMM dd, yyyy)", Locale.getDefault())
            val formattedSunriseTime = dateFormat.format(calendar.time)
            sunriseEditText.setText("Sunrise\n$formattedSunriseTime")
        }
        if (storedPlaceName != "") {
            locationEditText.setText("Location\n$storedPlaceName")
        }

    }
    private fun saveCoordinatesToPrefs(latitude: Double, longitude: Double) {
        val sharedPrefs: SharedPreferences =
            getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()

        val sunriseEditText = findViewById<EditText>(R.id.sunriseTimeEditText)
        val locationEditText = findViewById<EditText>(R.id.locationNameEditText)
        val coordinatesEditText = findViewById<EditText>(R.id.coordinatesEditText)

        editor.putFloat("latitude", latitude.toFloat())
        editor.putFloat("longitude", longitude.toFloat())

        editor.putLong("sunriseTimestamp", 0)
        editor.putString("locationName", "Unknown")
        editor.apply()

        coordinatesEditText.setText("GPS Coordinates\nLa: $latitude\nLo: $longitude")
        sunriseEditText.setText("Sunrise: Unknown")
        locationEditText.setText("Location: Unknown")
    }

    private fun getSavedCoordinatesFromPrefs(): Triple<Double, Double, Long>? {
        val sharedPrefs: SharedPreferences =
            getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val latitude = sharedPrefs.getFloat("latitude", 0.0F).toDouble()
        val longitude = sharedPrefs.getFloat("longitude", 0.0F).toDouble()
        val sunriseTimestamp = sharedPrefs.getLong("sunriseTimestamp", 0L)

        // Check if coordinates and sunrise time are valid
        if (latitude == 0.0 && longitude == 0.0) {
            return null
        }
        return Triple(latitude, longitude, sunriseTimestamp)
    }

    private fun getLocationNameFromPrefs(): String? {
        val sharedPrefs: SharedPreferences =
            getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("locationName", null)
    }

    fun onDaysClicked(view: View) {
        if (view is Button) {
            val day = when (view.id) {
                R.id.mondayButton -> "Monday"
                R.id.tuesdayButton -> "Tuesday"
                R.id.wednesdayButton -> "Wednesday"
                R.id.thursdayButton -> "Thursday"
                R.id.fridayButton -> "Friday"
                R.id.saturdayButton -> "Saturday"
                R.id.sundayButton -> "Sunday"
                else -> return
            }
            val isChecked = !view.isSelected
            saveDaysState(day, isChecked)
            view.isSelected = isChecked
        }
    }

    private fun saveDaysState(day: String, isSelected: Boolean) {
        val sharedPrefs: SharedPreferences = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.putBoolean(day, isSelected)
        editor.apply()
        loadDaysStates()
    }

    private fun loadDaysStates() {
        val sharedPrefs: SharedPreferences = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        setButtonState(mondayButton, sharedPrefs.getBoolean("Monday", false))
        setButtonState(tuesdayButton, sharedPrefs.getBoolean("Tuesday", false))
        setButtonState(wednesdayButton, sharedPrefs.getBoolean("Wednesday", false))
        setButtonState(thursdayButton, sharedPrefs.getBoolean("Thursday", false))
        setButtonState(fridayButton, sharedPrefs.getBoolean("Friday", false))
        setButtonState(saturdayButton, sharedPrefs.getBoolean("Saturday", false))
        setButtonState(sundayButton, sharedPrefs.getBoolean("Sunday", false))
    }

    private fun setButtonState(button: Button?, isSelected: Boolean) {
        button?.isSelected = isSelected
        val textColor: Int
        val bgDrawable: Int

        if (isSelected) {
            textColor = Color.parseColor("#000000")
            bgDrawable = R.drawable.rounded_button_selected
        } else {
            textColor = Color.parseColor("#ff9853")
            bgDrawable = R.drawable.rounded_button
        }
        button?.setTextColor(textColor)
        button?.setBackgroundResource(bgDrawable)
    }



    override fun onLocationChanged(location: Location) {
        // Handle the new location update here
        Log.w("Rooster Location", "Received GPS")
        val latitude = location.latitude
        val longitude = location.longitude
        saveCoordinatesToPrefs(latitude, longitude)
        locationManager.removeUpdates(locationListener)
        // Restart My service here
        val serviceIntent = Intent(this, MyService::class.java)
        stopService(serviceIntent)
        startService(serviceIntent)
    }

    override fun onProviderEnabled(provider: String) {
        // Called when the provider (e.g., GPS) is enabled
    }

    override fun onProviderDisabled(provider: String) {
        // Called when the provider (e.g., GPS) is disabled
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        // Called when the status of the provider changes
    }
    override fun onDestroy() {
        super.onDestroy()
    }
}