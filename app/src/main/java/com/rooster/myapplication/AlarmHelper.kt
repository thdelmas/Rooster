package com.rooster.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmHelper(private val context: Context) {

    fun setSunriseAlarm(sunriseTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = 0

        if (alarmManager.canScheduleExactAlarms()) {
            // Create and set the alarm only if the pendingIntent doesn't exist
            intent.putExtra("label", "Rooster")
            val newPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_MUTABLE)
            val triggerAtMillis = sunriseTimeMillis
            val calendar = Calendar.getInstance()
            calendar.setTimeInMillis(sunriseTimeMillis)
            val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy hh:mm a", Locale.getDefault())
            val formattedSunriseTime = dateFormat.format(calendar.time)

            Log.w("Rooster", "Setting alarm for $formattedSunriseTime")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, newPendingIntent)
        } else {
            Log.e("Rooster", "AlarmManager.canScheduleExactAlarms() returned false")
        }
    }
}
