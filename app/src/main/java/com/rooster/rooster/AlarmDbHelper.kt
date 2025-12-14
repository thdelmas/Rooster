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

class AlarmDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_VERSION = 5 // Increment database version for new features
        const val DATABASE_NAME = "alarm_db"
    }

    private val alarmHandler = AlarmHandler()
    val context = context

    /**
     * Safely reads a boolean value from a cursor, handling both INTEGER (0/1) and potential BOOLEAN types.
     * SQLite stores BOOLEAN as INTEGER internally, but this provides defensive handling for edge cases.
     * 
     * @param cursor The database cursor
     * @param columnIndex The index of the column to read
     * @return true if the value is non-zero, false otherwise
     */
    private fun getBooleanSafe(cursor: android.database.Cursor, columnIndex: Int): Boolean {
        return when {
            cursor.isNull(columnIndex) -> false
            else -> {
                val value = cursor.getInt(columnIndex)
                value != 0
            }
        }
    }

    /**
     * Safely reads a boolean value from a cursor by column name.
     * 
     * @param cursor The database cursor
     * @param columnName The name of the column to read
     * @return true if the value is non-zero, false otherwise
     */
    private fun getBooleanSafe(cursor: android.database.Cursor, columnName: String): Boolean {
        val columnIndex = cursor.getColumnIndexOrThrow(columnName)
        return getBooleanSafe(cursor, columnIndex)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Use INTEGER for boolean values to match Room database schema
        // SQLite doesn't have a native BOOLEAN type, so INTEGER (0/1) is used
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
    enabled INTEGER NOT NULL DEFAULT 0,
    monday INTEGER NOT NULL DEFAULT 0,
    tuesday INTEGER NOT NULL DEFAULT 0,
    wednesday INTEGER NOT NULL DEFAULT 0,
    thursday INTEGER NOT NULL DEFAULT 0,
    friday INTEGER NOT NULL DEFAULT 0,
    saturday INTEGER NOT NULL DEFAULT 0,
    sunday INTEGER NOT NULL DEFAULT 0,
    vibrate INTEGER NOT NULL DEFAULT 1,
    snooze_enabled INTEGER NOT NULL DEFAULT 1,
    snooze_duration INTEGER NOT NULL DEFAULT 10,
    snooze_count INTEGER NOT NULL DEFAULT 3,
    volume INTEGER NOT NULL DEFAULT 80,
    gradual_volume INTEGER NOT NULL DEFAULT 0
);"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT")
        }
        if (oldVersion < 3) {
            // Add any version 3 schema changes here if needed
            // For now, maintaining compatibility with existing version 3 databases
        }
        if (oldVersion < 4) {
            // Add new alarm feature columns
            // Note: SQLite doesn't have native BOOLEAN, uses INTEGER (0/1) internally
            // Using INTEGER explicitly for consistency with Room schema
            db.execSQL("ALTER TABLE alarms ADD COLUMN vibrate INTEGER DEFAULT 1")
            db.execSQL("ALTER TABLE alarms ADD COLUMN snooze_enabled INTEGER DEFAULT 1")
            db.execSQL("ALTER TABLE alarms ADD COLUMN snooze_duration INTEGER DEFAULT 10")
            db.execSQL("ALTER TABLE alarms ADD COLUMN snooze_count INTEGER DEFAULT 3")
            db.execSQL("ALTER TABLE alarms ADD COLUMN volume INTEGER DEFAULT 80")
            db.execSQL("ALTER TABLE alarms ADD COLUMN gradual_volume INTEGER DEFAULT 0")
        }
        if (oldVersion < 5) {
            // Migration 4→5: Ensure all boolean columns use INTEGER type and data consistency
            // This migration ensures compatibility with Room database schema
            // SQLite stores BOOLEAN as INTEGER internally, but we explicitly ensure INTEGER type
            // and normalize any existing data to 0/1 values
            
            try {
                // Check if columns exist and ensure they're INTEGER type
                // Since SQLite doesn't support ALTER COLUMN TYPE, we verify data consistency
                // by ensuring all boolean values are 0 or 1
                
                // Normalize boolean values to ensure they're 0 or 1 (not NULL or other values)
                db.execSQL("""
                    UPDATE alarms 
                    SET enabled = CASE WHEN enabled IS NULL OR enabled = 0 THEN 0 ELSE 1 END,
                        monday = CASE WHEN monday IS NULL OR monday = 0 THEN 0 ELSE 1 END,
                        tuesday = CASE WHEN tuesday IS NULL OR tuesday = 0 THEN 0 ELSE 1 END,
                        wednesday = CASE WHEN wednesday IS NULL OR wednesday = 0 THEN 0 ELSE 1 END,
                        thursday = CASE WHEN thursday IS NULL OR thursday = 0 THEN 0 ELSE 1 END,
                        friday = CASE WHEN friday IS NULL OR friday = 0 THEN 0 ELSE 1 END,
                        saturday = CASE WHEN saturday IS NULL OR saturday = 0 THEN 0 ELSE 1 END,
                        sunday = CASE WHEN sunday IS NULL OR sunday = 0 THEN 0 ELSE 1 END
                """.trimIndent())
                
                // Normalize enhancement fields if they exist
                val cursor = db.rawQuery("PRAGMA table_info(alarms)", null)
                val columnNames = mutableSetOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        val nameIndex = it.getColumnIndex("name")
                        if (nameIndex >= 0) {
                            columnNames.add(it.getString(nameIndex))
                        }
                    }
                }
                
                if (columnNames.contains("vibrate")) {
                    db.execSQL("""
                        UPDATE alarms 
                        SET vibrate = CASE WHEN vibrate IS NULL OR vibrate = 0 THEN 0 ELSE 1 END
                    """.trimIndent())
                }
                if (columnNames.contains("snooze_enabled")) {
                    db.execSQL("""
                        UPDATE alarms 
                        SET snooze_enabled = CASE WHEN snooze_enabled IS NULL OR snooze_enabled = 0 THEN 0 ELSE 1 END
                    """.trimIndent())
                }
                if (columnNames.contains("gradual_volume")) {
                    db.execSQL("""
                        UPDATE alarms 
                        SET gradual_volume = CASE WHEN gradual_volume IS NULL OR gradual_volume = 0 THEN 0 ELSE 1 END
                    """.trimIndent())
                }
                
                Log.d("AlarmDbHelper", "Migration 4→5 completed: Boolean values normalized to INTEGER (0/1)")
            } catch (e: Exception) {
                Log.e("AlarmDbHelper", "Error during migration 4→5: ${e.message}", e)
                // Don't throw - allow migration to continue
            }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle downgrade gracefully - in most cases for development,
        // we'll just maintain compatibility with the current schema
        Log.w("AlarmDbHelper", "Downgrading database from version $oldVersion to $newVersion")
        // No action needed as schema is compatible
    }

    fun insertAlarm(alarm: AlarmCreation) {
        val db = writableDatabase
        // Use INTEGER (0/1) for boolean values to match Room schema
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("ringtoneUri", alarm.ringtoneUri)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", if (alarm.enabled) 1 else 0)
            put("monday", 0)
            put("tuesday", 0)
            put("wednesday", 0)
            put("thursday", 0)
            put("friday", 0)
            put("saturday", 0)
            put("sunday", 0)
            put("vibrate", 1)
            put("snooze_enabled", 1)
            put("snooze_duration", 10)
            put("snooze_count", 3)
            put("volume", 80)
            put("gradual_volume", 0)
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

        return cursor.use {
            if (it.moveToFirst()) {
                Alarm(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    label = it.getString(it.getColumnIndexOrThrow("label")),
                    mode = it.getString(it.getColumnIndexOrThrow("mode")),
                    ringtoneUri = it.getString(it.getColumnIndexOrThrow("ringtoneUri")) ?: "Default",
                    relative1 = it.getString(it.getColumnIndexOrThrow("relative1")),
                    relative2 = it.getString(it.getColumnIndexOrThrow("relative2")),
                    time1 = it.getLong(it.getColumnIndexOrThrow("time1")),
                    time2 = it.getLong(it.getColumnIndexOrThrow("time2")),
                    calculatedTime = it.getLong(it.getColumnIndexOrThrow("calculated_time")),
                    enabled = getBooleanSafe(it, "enabled"),
                    monday = getBooleanSafe(it, "monday"),
                    tuesday = getBooleanSafe(it, "tuesday"),
                    wednesday = getBooleanSafe(it, "wednesday"),
                    thursday = getBooleanSafe(it, "thursday"),
                    friday = getBooleanSafe(it, "friday"),
                    saturday = getBooleanSafe(it, "saturday"),
                    sunday = getBooleanSafe(it, "sunday"),
                    vibrate = getBooleanSafe(it, "vibrate"),
                    snoozeEnabled = getBooleanSafe(it, "snooze_enabled"),
                    snoozeDuration = it.getInt(it.getColumnIndexOrThrow("snooze_duration")),
                    snoozeCount = it.getInt(it.getColumnIndexOrThrow("snooze_count")),
                    volume = it.getInt(it.getColumnIndexOrThrow("volume")),
                    gradualVolume = getBooleanSafe(it, "gradual_volume")
                )
            } else {
                null
            }
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
        // Use INTEGER (0/1) for boolean values to match Room schema
        val values = ContentValues().apply {
            put("label", alarm.label)
            put("mode", alarm.mode)
            put("ringtoneUri", alarm.ringtoneUri)
            put("relative1", alarm.relative1)
            put("relative2", alarm.relative2)
            put("time1", alarm.time1)
            put("time2", alarm.time2)
            put("calculated_time", alarm.calculatedTime)
            put("enabled", if (alarm.enabled) 1 else 0)
            put("monday", if (alarm.monday) 1 else 0)
            put("tuesday", if (alarm.tuesday) 1 else 0)
            put("wednesday", if (alarm.wednesday) 1 else 0)
            put("thursday", if (alarm.thursday) 1 else 0)
            put("friday", if (alarm.friday) 1 else 0)
            put("saturday", if (alarm.saturday) 1 else 0)
            put("sunday", if (alarm.sunday) 1 else 0)
            put("vibrate", if (alarm.vibrate) 1 else 0)
            put("snooze_enabled", if (alarm.snoozeEnabled) 1 else 0)
            put("snooze_duration", alarm.snoozeDuration)
            put("snooze_count", alarm.snoozeCount)
            put("volume", alarm.volume)
            put("gradual_volume", if (alarm.gradualVolume) 1 else 0)
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
        val cursor = db.query(
            "alarms", 
            arrayOf("id", "label", "mode", "ringtoneUri", "relative1", "relative2", 
                    "time1", "time2", "calculated_time", "enabled", "monday", "tuesday", 
                    "wednesday", "thursday", "friday", "saturday", "sunday", "vibrate",
                    "snooze_enabled", "snooze_duration", "snooze_count", "volume", "gradual_volume"), 
            null, null, null, null, null
        )
        
        return cursor.use {
            val alarms = mutableListOf<Alarm>()
            while (it.moveToNext()) {
                alarms.add(Alarm(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    label = it.getString(it.getColumnIndexOrThrow("label")),
                    mode = it.getString(it.getColumnIndexOrThrow("mode")),
                    ringtoneUri = it.getString(it.getColumnIndexOrThrow("ringtoneUri")) ?: "Default",
                    relative1 = it.getString(it.getColumnIndexOrThrow("relative1")),
                    relative2 = it.getString(it.getColumnIndexOrThrow("relative2")),
                    time1 = it.getLong(it.getColumnIndexOrThrow("time1")),
                    time2 = it.getLong(it.getColumnIndexOrThrow("time2")),
                    calculatedTime = it.getLong(it.getColumnIndexOrThrow("calculated_time")),
                    enabled = getBooleanSafe(it, "enabled"),
                    monday = getBooleanSafe(it, "monday"),
                    tuesday = getBooleanSafe(it, "tuesday"),
                    wednesday = getBooleanSafe(it, "wednesday"),
                    thursday = getBooleanSafe(it, "thursday"),
                    friday = getBooleanSafe(it, "friday"),
                    saturday = getBooleanSafe(it, "saturday"),
                    sunday = getBooleanSafe(it, "sunday"),
                    vibrate = getBooleanSafe(it, "vibrate"),
                    snoozeEnabled = getBooleanSafe(it, "snooze_enabled"),
                    snoozeDuration = it.getInt(it.getColumnIndexOrThrow("snooze_duration")),
                    snoozeCount = it.getInt(it.getColumnIndexOrThrow("snooze_count")),
                    volume = it.getInt(it.getColumnIndexOrThrow("volume")),
                    gradualVolume = getBooleanSafe(it, "gradual_volume")
                ))
            }
            alarms
        }
    }
}
