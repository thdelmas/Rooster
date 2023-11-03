package com.rooster.rooster

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Calendar
import java.util.Date


class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        setContentView(R.layout.activity_settings)
        //linkButtons()
        updateValues()
    }

    private fun linkButtons() {
        val astroDawnSettings = findViewById<RelativeLayout>(R.id.astroDawnSetting)
        astroDawnSettings.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                pickTime(view, "astroDawn")
            }
        })
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
            val formattedTime = sdf.format(Date(timeInMillis))
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

        // Schedule the next update for one second from now
        val handler = android.os.Handler()
        handler.postDelayed({
            updateValues()
        }, 1000)
    }
}