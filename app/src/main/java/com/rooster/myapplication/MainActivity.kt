package com.rooster.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity() : ComponentActivity() {
    private val coarseLocationPermissionRequestCode = 1001
    private var mondayCheckBox: CheckBox? = null
    private var tuesdayCheckBox: CheckBox? = null
    private var wednesdayCheckBox: CheckBox? = null
    private var thursdayCheckBox: CheckBox? = null
    private var fridayCheckBox: CheckBox? = null
    private var saturdayCheckBox: CheckBox? = null
    private var sundayCheckBox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mondayCheckBox = findViewById(R.id.mondayCheckBox);
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox);
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox);
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox);
        fridayCheckBox = findViewById(R.id.fridayCheckBox);
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox);
        sundayCheckBox = findViewById(R.id.sundayCheckBox);

        loadCheckboxStates()
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
            coordinatesEditText.setText("Coordinates:\n\nLat: $latitude\nLon: $longitude")

            if (sunriseTimestamp > 0) {
                // Convert the sunrise timestamp to a human-readable format
                val calendar = Calendar.getInstance()
                calendar.setTimeInMillis(sunriseTimestamp)
                val dateFormat = SimpleDateFormat("hh:mm a\n(EEE, MMM dd, yyyy)", Locale.getDefault())
                val formattedSunriseTime = dateFormat.format(calendar.time)
                sunriseEditText.setText("Sunrise:\n\n$formattedSunriseTime")
                val locationName = getLocationNameFromPrefs()
                if (!locationName.isNullOrEmpty()) {
                    locationEditText.setText("Location Name:\n$locationName")
                } else {
                    locationEditText.setText("Location Name: Not available")
                }
            } else {
                sunriseEditText.setText("Sunrise: Not available")
            }

            startMyService()
        }
    }

    private fun startMyService() {
        val serviceIntent = Intent(this, MyService::class.java)
        startService(serviceIntent)
    }
        // Check if fine location permission is granted
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

        fun getCoordinates(view: View) {
            var coordinatesEditText = findViewById<EditText>(R.id.coordinatesEditText)
            if (isCoarseLocationPermissionGranted()) {
                // Get the current location of the device
                try {
                    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                    val location =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    // Display the latitude and longitude to the user
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Toast.makeText(
                            this,
                            "Your coordinates are: $latitude, $longitude",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.w("Rooster GPS", "New coordinates: $latitude, $longitude")
                        coordinatesEditText.setText("Coordinates:\nLat: $latitude\nLon: $longitude")
                        // Save coordinates to SharedPreferences
                        saveCoordinatesToPrefs(latitude, longitude)
                        startMyService()
                    } else {
                        Toast.makeText(this, "Unable to get your coordinates", Toast.LENGTH_SHORT)
                            .show()
                        Log.e("Rooster GPS", "Unable to get your coordinates")
                    }
                } catch (e: Exception) {
                    // Handle the exception
                    Toast.makeText(this, "Unable to get your coordinates", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("Rooster GPS", "Unable to get your coordinates")
                }
            } else {
                // Permission is not granted, request it
                requestCoarseLocationPermission()
            }
        }

    private fun saveCoordinatesToPrefs(latitude: Double, longitude: Double) {
        val sunriseEditText = findViewById<EditText>(R.id.sunriseTimeEditText)
        val locationEditText = findViewById<EditText>(R.id.locationNameEditText)
        val sharedPrefs: SharedPreferences =
            getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.putFloat("latitude", latitude.toFloat())
        editor.putFloat("longitude", longitude.toFloat())
        editor.putLong("sunriseTimestamp", 0)
        editor.putString("locationName", "Unknown")
        editor.apply()
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

    fun onCheckboxClicked(view: View) {
        Log.e("AAAAAA", "HHHHHHH")
        if (view is CheckBox) {
            val isChecked = view.isChecked
            when (view.id) {
                R.id.mondayCheckBox -> saveCheckboxState("Monday", isChecked)
                R.id.tuesdayCheckBox -> saveCheckboxState("Tuesday", isChecked)
                R.id.wednesdayCheckBox -> saveCheckboxState("Wednesday", isChecked)
                R.id.thursdayCheckBox -> saveCheckboxState("Thursday", isChecked)
                R.id.fridayCheckBox -> saveCheckboxState("Friday", isChecked)
                R.id.saturdayCheckBox -> saveCheckboxState("Saturday", isChecked)
                R.id.sundayCheckBox -> saveCheckboxState("Sunday", isChecked)
            }
        }
    }

    private fun saveCheckboxState(day: String, isChecked: Boolean) {
        val sharedPrefs: SharedPreferences = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        Log.w("Rooster", "Change $day activation")
        editor.putBoolean(day, isChecked)
        editor.apply()
    }

    private fun loadCheckboxStates() {
        val sharedPrefs: SharedPreferences = getSharedPreferences("RoosterPrefs", Context.MODE_PRIVATE)
        mondayCheckBox?.isChecked = sharedPrefs.getBoolean("Monday", false)
        tuesdayCheckBox?.isChecked = sharedPrefs.getBoolean("Tuesday", false)
        wednesdayCheckBox?.isChecked = sharedPrefs.getBoolean("Wednesday", false)
        thursdayCheckBox?.isChecked = sharedPrefs.getBoolean("Thursday", false)
        fridayCheckBox?.isChecked = sharedPrefs.getBoolean("Friday", false)
        saturdayCheckBox?.isChecked = sharedPrefs.getBoolean("Saturday", false)
        sundayCheckBox?.isChecked = sharedPrefs.getBoolean("Sunday", false)
    }
}