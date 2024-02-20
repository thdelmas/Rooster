package com.rooster.rooster

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.Date
import java.util.Locale

class AlarmDbHelper(context: Context) : SQLiteOpenHelper(context, "alarm_db", null, 1) {
    companion object {
        const val DATABASE_VERSION = 2 // Increment database version
        const val DATABASE_NAME = "alarm_db"
    }

    private val alarmHandler = AlarmHandler()
    val context = context

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE alarms (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    label TEXT,
    mode TEXT,
    ringtoneUri TEXT,
    relative1 TEXT,
    relative2 TEXT,
    time1 INTEGER,
    time2 INTEGER,
    calculated_time INTEGER,
    enabled BOOLEAN,
    monday BOOLEAN,
    tuesday BOOLEAN,
    wednesday BOOLEAN,
    thursday BOOLEAN,
    friday BOOLEAN,
    saturday BOOLEAN,
    sunday BOOLEAN
);"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT")
        }
    }

    fun insertAlarm(alarm: AlarmCreation) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("ringtoneUri", alarm.ringtoneUri)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", alarm.enabled)
            put("monday", false)
            put("tuesday", false)
            put("wednesday", false)
            put("thursday", false)
            put("friday", false)
            put("saturday", false)
            put("sunday", false)
        }

        db.insert("alarms", null, values)
    }


    fun getAlarm(id: Long): Alarm? {
        val db = readableDatabase
        val cursor = db.query(
            "alarms",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            return Alarm(
                id = cursor.getLong(cursor.getColumnIndex("id")),
                label = cursor.getString(cursor.getColumnIndex("label")),
                mode = cursor.getString(cursor.getColumnIndex("mode")),
                ringtoneUri = cursor.getString(cursor.getColumnIndex("ringtoneUri")),
                relative1 = cursor.getString(cursor.getColumnIndex("relative1")),
                relative2 = cursor.getString(cursor.getColumnIndex("relative2")),
                time1 = cursor.getLong(cursor.getColumnIndex("time1")),
                time2 = cursor.getLong(cursor.getColumnIndex("time2")),
                calculatedTime = cursor.getLong(cursor.getColumnIndex("calculated_time")),
                enabled = cursor.getInt(cursor.getColumnIndex("enabled")) == 1,
                monday = cursor.getInt(cursor.getColumnIndex("monday")) == 1,
                tuesday = cursor.getInt(cursor.getColumnIndex("tuesday")) == 1,
                wednesday = cursor.getInt(cursor.getColumnIndex("wednesday")) == 1,
                thursday = cursor.getInt(cursor.getColumnIndex("thursday")) == 1,
                friday = cursor.getInt(cursor.getColumnIndex("friday")) == 1,
                saturday = cursor.getInt(cursor.getColumnIndex("saturday")) == 1,
                sunday = cursor.getInt(cursor.getColumnIndex("sunday")) == 1,
            )
        } else {
            return null
        }
    }


    fun updateAlarm(alarm: Alarm) {
        val db = writableDatabase
        // Calculate the alarm time
        if (alarm.relative1 != "Pick Time") {
            alarm.time1 = getRelativeTime(alarm.relative1)
        }
        if (alarm.relative2 != "Pick Time") {
            alarm.time2 = getRelativeTime(alarm.relative2)
        }
        alarm.calculatedTime = calculateTime(alarm)
        Log.e("Update Alarm", alarm.calculatedTime.toString())
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("ringtoneUri", alarm.ringtoneUri)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", alarm.enabled)
            put("monday", alarm.monday)
            put("tuesday", alarm.tuesday)
            put("wednesday", alarm.wednesday)
            put("thursday", alarm.thursday)
            put("friday", alarm.friday)
            put("saturday", alarm.saturday)
            put("sunday", alarm.sunday)
        }
        db.update("alarms", values, "id = ?", arrayOf(alarm.id.toString()))
        alarmHandler.setNextAlarm(context)
    }

    internal fun calculateTime(alarm: Alarm): Long {
        Log.d("Rooster", "---\nAlarm: ${alarm.label} - id: ${alarm.id}")
        alarm.calculatedTime = calculateTimeInner(alarm)
        alarm.calculatedTime = addDays(alarm, alarm.calculatedTime)
        val calendar = Calendar.getInstance()
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        calendar.timeInMillis = alarm.calculatedTime
        var formattedDate = fullDateFormat.format(calendar.time)
        Log.d("Rooster", "Next Iteration: $formattedDate\n---")
        return alarm.calculatedTime
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun calculateTimeInner(alarm: Alarm): Long {
        var calculatedTime = 0L
        if (alarm.mode == "At") {
            if (alarm.relative1 == "Pick Time") {
                calculatedTime = alarm.time1
                return calculatedTime
            } else {
                calculatedTime = getRelativeTime(alarm.relative1)
                return calculatedTime
            }
        }else if (alarm.mode == "Between") {
            var time1 = alarm.time1
            var time2 = alarm.time2
            if (alarm.relative1 != "Pick Time") {
                time1 = getRelativeTime(alarm.relative1)
            }
            if (alarm.relative2 != "Pick Time") {
                time2 = getRelativeTime(alarm.relative2)
            }

            val calendarNow = Calendar.getInstance()
            val calendar1 = Calendar.getInstance()
            val calendar2 = Calendar.getInstance()
            calendar1.timeInMillis = time1
            calendar2.timeInMillis = time2


            // Ensure times are for today, adjust if in the past
            if (calendar1.before(calendarNow)) {
                calendar1.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
                calendar1.set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
                calendar1.set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH))            }
            if (calendar2.before(calendarNow)) {
                calendar2.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
                calendar2.set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
                calendar2.set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH))            }

            // Calculate the middle point based only on time
            val midTime = (calendar1.timeInMillis + calendar2.timeInMillis) / 2
            val calculatedCalendar = Calendar.getInstance()
            calculatedCalendar.timeInMillis = midTime


            if (calculatedCalendar.timeInMillis <= System.currentTimeMillis()) {
                calculatedCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            // Print the hours and minutes
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            Log.d("TIME1", "@ ${timeFormat.format(calendar1.time)}")
            Log.d("TIME2", "@ ${timeFormat.format(calendar2.time)}")
            Log.d("CALCULATED_TIME", "@ ${timeFormat.format(calculatedCalendar.time)}")

            return calculatedCalendar.timeInMillis
        }
        else if (alarm.mode == "After") {
            var time1 = getRelativeTime(alarm.relative2)
            var time2 = alarm.time1
            calculatedTime = (time1 + time2)
            return calculatedTime
        } else if (alarm.mode == "Before") {
            var time1 = getRelativeTime(alarm.relative2)
            var time2 = alarm.time1
            calculatedTime = (time1 - time2)
            return calculatedTime
        }
        return calculatedTime
    }

    private fun addDays(alarm: Alarm, calculatedTime: Long): Long {
        val currentDate = Calendar.getInstance()
        var alarmDate = Calendar.getInstance()
        alarmDate.timeInMillis = calculatedTime

        while (alarmDate.timeInMillis <= currentDate.timeInMillis) {
            alarmDate.add(Calendar.DAY_OF_MONTH, 1)
        }
        val alarmDayOfWeek = alarmDate.get(Calendar.DAY_OF_WEEK) - 1// Adjust to zero-based indexing and sunday as 0
        val weekdays = listOf(
            alarm.sunday,
            alarm.monday,
            alarm.tuesday,
            alarm.wednesday,
            alarm.thursday,
            alarm.friday,
            alarm.saturday
        )
        // Start searching from the current day and go up to 7 days (a full week)
        for (i in 0 until 7) {
            val dayToCheck = (alarmDayOfWeek + i) % 7 // Ensure it wraps around the days of the week
            if (weekdays[dayToCheck]) {
                // Calculate the difference in days between the current day and the day with a true value
                alarmDate.add(Calendar.DAY_OF_MONTH, i)
                // Calculate the time difference in milliseconds and add it to calculatedTime
                return alarmDate.timeInMillis
            }
        }
        // If no true value is found in the next 7 days, return the original calculatedTime
        return alarmDate.timeInMillis
    }


    fun getRelativeTime(relative1: String): Long {
        val sharedPrefs = context.getSharedPreferences("rooster_prefs",
            AppCompatActivity.MODE_PRIVATE
        )
        var timeInMillis = 0L
        when (relative1) {
            "Astronomical Dawn" -> timeInMillis = sharedPrefs.getLong("astroDawn", 0)
            "Nautical Dawn" -> timeInMillis = sharedPrefs.getLong("nauticalDawn", 0)
            "Civil Dawn" -> timeInMillis = sharedPrefs.getLong("civilDawn", 0)
            "Sunrise" -> timeInMillis = sharedPrefs.getLong("sunrise", 0)
            "Sunset" -> timeInMillis = sharedPrefs.getLong("sunset", 0)
            "Civil Dusk" -> timeInMillis = sharedPrefs.getLong("civilDusk", 0)
            "Nautical Dusk" -> timeInMillis = sharedPrefs.getLong("nauticalDusk", 0)
            "Astronomical Dusk" -> timeInMillis = sharedPrefs.getLong("astroDusk", 0)
            "Solar Noon" -> timeInMillis = sharedPrefs.getLong("solarNoon", 0)
        }
         // Calculate the time difference in milliseconds between local time and GMT+0.
            val fullDateFormat = SimpleDateFormat("HH:mm")
            var calendar = Calendar.getInstance()
            val timeZone = TimeZone.getTimeZone("GMT")
            fullDateFormat.timeZone = timeZone
            calendar.timeInMillis = timeInMillis
            if (timeZone.inDaylightTime(calendar.time)) {
                val dstOffsetInMillis = timeZone.dstSavings
                calendar.add(Calendar.MILLISECOND, dstOffsetInMillis)
            }

         // Add the time difference to the local time to get GMT+0 time.
        return calendar.timeInMillis
    }

    fun deleteAlarm(id: Long) {
        val db = writableDatabase
        db.delete("alarms", "id = ?", arrayOf(id.toString()))
        alarmHandler.unsetAlarmById(context, id)
    }

    fun getAllAlarms(): List<Alarm> {
        val db = readableDatabase
        val cursor = db.query("alarms", arrayOf("id", "label", "mode", "ringtoneUri", "relative1", "relative2", "time1", "time2", "calculated_time", "enabled", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"), null, null, null, null, null)
        val alarms = mutableListOf<Alarm>()
        while (cursor.moveToNext()) {
            alarms.add(Alarm(
                id = cursor.getLong(cursor.getColumnIndex("id")),
                label = cursor.getString(cursor.getColumnIndex("label")),
                mode = cursor.getString(cursor.getColumnIndex("mode")),
                ringtoneUri = cursor.getString(cursor.getColumnIndex("ringtoneUri")),
                relative1 = cursor.getString(cursor.getColumnIndex("relative1")),
                relative2 = cursor.getString(cursor.getColumnIndex("relative2")),
                time1 = cursor.getLong(cursor.getColumnIndex("time1")),
                time2 = cursor.getLong(cursor.getColumnIndex("time2")),
                calculatedTime = cursor.getLong(cursor.getColumnIndex("calculated_time")),
                enabled = cursor.getInt(cursor.getColumnIndex("enabled")) == 1,
                monday = cursor.getInt(cursor.getColumnIndex("monday")) == 1,
                tuesday = cursor.getInt(cursor.getColumnIndex("tuesday")) == 1,
                wednesday = cursor.getInt(cursor.getColumnIndex("wednesday")) == 1,
                thursday = cursor.getInt(cursor.getColumnIndex("thursday")) == 1,
                friday = cursor.getInt(cursor.getColumnIndex("friday")) == 1,
                saturday = cursor.getInt(cursor.getColumnIndex("saturday")) == 1,
                sunday = cursor.getInt(cursor.getColumnIndex("sunday")) == 1,
            ))
        }
        cursor.close()
        return alarms
    }
}