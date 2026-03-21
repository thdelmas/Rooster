package com.rooster.rooster.widget

import android.content.Context
import com.rooster.rooster.data.local.AlarmDatabase
import com.rooster.rooster.data.local.entity.AlarmEntity
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import java.util.*

/**
 * Handles data fetching and time calculations for the Solar Ring Widget.
 * Separates data concerns from rendering logic.
 */
object SolarRingDataProvider {

    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    private const val DEGREES_PER_HOUR = 15f

    suspend fun getAstronomyData(context: Context): AstronomyDataEntity? {
        return try {
            val database = buildDatabase(context)
            val astronomyData = database.astronomyDao().getAstronomyData()
            database.close()

            if (astronomyData != null) return astronomyData

            getAstronomyDataFromPrefs(context)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getNextAlarm(context: Context): AlarmEntity? {
        return try {
            val database = buildDatabase(context)
            val enabledAlarms = database.alarmDao().getEnabledAlarms()
            database.close()

            val currentTime = System.currentTimeMillis()
            enabledAlarms.firstOrNull { it.calculatedTime > currentTime }
                ?: enabledAlarms.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Normalize an astronomy time to today's date, preserving hour/minute.
     */
    fun normalizeTime(originalTime: Long, todayStart: Long): Long {
        if (originalTime <= 0) return 0
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = originalTime
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        calendar.timeInMillis = todayStart
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Normalize all astronomy event times to today's date.
     */
    fun normalizeAstronomyData(data: AstronomyDataEntity, todayStart: Long): AstronomyDataEntity {
        return data.copy(
            sunrise = normalizeTime(data.sunrise, todayStart),
            sunset = normalizeTime(data.sunset, todayStart),
            solarNoon = normalizeTime(data.solarNoon, todayStart),
            civilDawn = normalizeTime(data.civilDawn, todayStart),
            civilDusk = normalizeTime(data.civilDusk, todayStart),
            nauticalDawn = normalizeTime(data.nauticalDawn, todayStart),
            nauticalDusk = normalizeTime(data.nauticalDusk, todayStart),
            astroDawn = normalizeTime(data.astroDawn, todayStart),
            astroDusk = normalizeTime(data.astroDusk, todayStart)
        )
    }

    /**
     * Calculate the angle on the ring for a given time, relative to solar noon.
     * Returns degrees where -90 = 12 o'clock (top of ring).
     */
    fun timeToAngle(time: Long, solarNoon: Long): Float {
        if (solarNoon <= 0) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val hourOffset = hour - 12
            val minuteOffset = minute / 60f
            return (hourOffset + minuteOffset) * DEGREES_PER_HOUR - 90f
        }

        var timeDiff = time - solarNoon
        if (timeDiff > DAY_IN_MILLIS / 2) {
            timeDiff -= DAY_IN_MILLIS
        } else if (timeDiff < -DAY_IN_MILLIS / 2) {
            timeDiff += DAY_IN_MILLIS
        }

        val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
        return -90f + hoursFromSolarNoon * DEGREES_PER_HOUR
    }

    /**
     * Convert a time to a gradient position (0.0 to 1.0), relative to solar noon at 0.0.
     */
    fun timeToPosition(time: Long, solarNoon: Long): Float {
        if (solarNoon <= 0) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val hourOffset = hour - 12
            val minuteOffset = minute / 60f
            val angleFromSolarNoon = (hourOffset + minuteOffset) * DEGREES_PER_HOUR
            var position = angleFromSolarNoon / 360f
            while (position < 0) position += 1f
            while (position >= 1) position -= 1f
            return position
        }

        var timeDiff = time - solarNoon
        if (timeDiff > DAY_IN_MILLIS / 2) {
            timeDiff -= DAY_IN_MILLIS
        } else if (timeDiff < -DAY_IN_MILLIS / 2) {
            timeDiff += DAY_IN_MILLIS
        }

        val hoursFromSolarNoon = timeDiff / (60f * 60f * 1000f)
        val angleFromSolarNoon = hoursFromSolarNoon * DEGREES_PER_HOUR
        var position = angleFromSolarNoon / 360f
        while (position < 0) position += 1f
        while (position >= 1) position -= 1f
        return position
    }

    fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun buildDatabase(context: Context): AlarmDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME
        )
            .addMigrations(
                AlarmDatabase.MIGRATION_1_2,
                AlarmDatabase.MIGRATION_2_3,
                AlarmDatabase.MIGRATION_3_4,
                AlarmDatabase.MIGRATION_4_5,
                AlarmDatabase.MIGRATION_5_6
            )
            .build()
    }

    private fun getAstronomyDataFromPrefs(context: Context): AstronomyDataEntity? {
        val prefs = context.getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        val sunrise = prefs.getLong("sunrise", 0)
        val sunset = prefs.getLong("sunset", 0)

        if (sunrise <= 0 || sunset <= 0) return null

        return AstronomyDataEntity(
            id = 1,
            latitude = prefs.getFloat("latitude", 0f),
            longitude = prefs.getFloat("longitude", 0f),
            sunrise = sunrise,
            sunset = sunset,
            solarNoon = prefs.getLong("solarNoon", 0),
            civilDawn = prefs.getLong("civilDawn", 0),
            civilDusk = prefs.getLong("civilDusk", 0),
            nauticalDawn = prefs.getLong("nauticalDawn", 0),
            nauticalDusk = prefs.getLong("nauticalDusk", 0),
            astroDawn = prefs.getLong("astroDawn", 0),
            astroDusk = prefs.getLong("astroDusk", 0),
            lastUpdated = System.currentTimeMillis(),
            dayLength = sunset - sunrise
        )
    }
}
