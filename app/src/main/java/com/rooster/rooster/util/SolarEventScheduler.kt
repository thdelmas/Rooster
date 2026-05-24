package com.rooster.rooster.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rooster.rooster.RoosterApplication
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.receiver.SolarEventReceiver
import java.util.Calendar

/**
 * Schedules AlarmManager broadcasts for upcoming solar events. Pulls
 * astronomy data from the repository (today's), and falls back to computing
 * tomorrow's directly if every event today has already passed. Each event
 * gets a stable request code so re-scheduling is idempotent.
 */
object SolarEventScheduler {

    private const val TAG = "SolarEventScheduler"

    private val EVENTS = listOf(
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN,
        AppConstants.SOLAR_EVENT_NAUTICAL_DAWN,
        AppConstants.SOLAR_EVENT_CIVIL_DAWN,
        AppConstants.SOLAR_EVENT_SUNRISE,
        AppConstants.SOLAR_EVENT_SOLAR_NOON,
        AppConstants.SOLAR_EVENT_SUNSET,
        AppConstants.SOLAR_EVENT_CIVIL_DUSK,
        AppConstants.SOLAR_EVENT_NAUTICAL_DUSK,
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK
    )

    /**
     * Cancel previously scheduled cues and schedule the next batch. Safe to
     * call repeatedly — re-scheduling overwrites the same PendingIntents.
     */
    suspend fun scheduleUpcoming(context: Context) {
        val app = context.applicationContext as? RoosterApplication
        val astronomyRepository = app?.provideAstronomyRepositoryOrNull()
        val locationRepository = app?.provideLocationRepositoryOrNull()

        // Always cancel first so events the user has just disabled go away.
        cancelAll(context)

        if (!SolarEventPrefs.isAnyEnabled(context)) {
            Logger.d(TAG, "All solar cues disabled — nothing to schedule")
            return
        }

        // When only the Zenith Sun Gong is on (no general sound/vibration), we
        // only need solar noon scheduled — skip the other eight events entirely.
        val gongOnlyMode = SolarEventPrefs.isZenithGongEnabled(context) &&
            !SolarEventPrefs.isSoundEnabled(context) &&
            !SolarEventPrefs.isVibrationEnabled(context)

        val today = astronomyRepository?.getAstronomyData(forceRefresh = false)
        val now = System.currentTimeMillis()
        val upcoming = mutableListOf<Pair<String, Long>>()

        if (today != null) {
            for ((event, time) in eventTimes(today)) {
                if (time <= now) continue
                if (gongOnlyMode && event != AppConstants.SOLAR_EVENT_SOLAR_NOON) continue
                upcoming += event to time
            }
        }

        if (upcoming.isEmpty()) {
            // All of today's events have passed (or no data at all). Compute
            // tomorrow's from the same coordinates so the cue chain doesn't
            // pause for hours waiting for the periodic worker.
            val location = locationRepository?.getLocation()
            val lat = location?.latitude ?: today?.latitude
            val lng = location?.longitude ?: today?.longitude
            if (lat != null && lng != null && (lat != 0f || lng != 0f)) {
                val tomorrow = SolarCalculator.calculate(
                    lat, lng,
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                )
                for ((event, time) in eventTimes(tomorrow)) {
                    if (time <= now) continue
                    if (gongOnlyMode && event != AppConstants.SOLAR_EVENT_SOLAR_NOON) continue
                    upcoming += event to time
                }
            }
        }

        if (upcoming.isEmpty()) {
            Logger.w(TAG, "No upcoming solar events to schedule (no astronomy data)")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Logger.e(TAG, "AlarmManager unavailable")
            return
        }

        for ((event, time) in upcoming) {
            schedule(context, alarmManager, event, time)
        }

        // If the zenith gong is enabled, also schedule the notification +
        // tactile signature for ZENITH_WINDOW_HALF_MS before solar noon.
        if (SolarEventPrefs.isZenithGongEnabled(context)) {
            val noonTime = upcoming.firstOrNull { it.first == AppConstants.SOLAR_EVENT_SOLAR_NOON }?.second
            if (noonTime != null) {
                val windowStart = noonTime - AppConstants.ZENITH_WINDOW_HALF_MS
                if (windowStart > now) {
                    scheduleZenithWindow(context, alarmManager, windowStart)
                }
            }
        }

        Logger.i(TAG, "Scheduled ${upcoming.size} upcoming solar event cue(s)")
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (event in EVENTS) {
            val pi = pendingIntentFor(context, event, mutableFlag = false) ?: continue
            alarmManager.cancel(pi)
            pi.cancel()
        }
        zenithWindowPendingIntent(context, mutableFlag = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        event: String,
        triggerAt: Long
    ) {
        val pi = pendingIntentFor(context, event, mutableFlag = true) ?: return

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                // Permission not granted — fall back to inexact (~5-15 min window
                // in Doze). The cue is gentle, so this degradation is acceptable.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Logger.d(TAG, "Scheduled '$event' at ${triggerAt}")
        } catch (e: SecurityException) {
            Logger.w(TAG, "Cannot schedule exact solar event '$event' — permission denied", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to schedule solar event '$event'", e)
        }
    }

    private fun pendingIntentFor(
        context: Context,
        event: String,
        mutableFlag: Boolean
    ): PendingIntent? {
        val intent = Intent(context, SolarEventReceiver::class.java).apply {
            action = SolarEventReceiver.ACTION_SOLAR_EVENT
            putExtra(SolarEventReceiver.EXTRA_EVENT, event)
        }
        val flags = if (mutableFlag) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCodeFor(event), intent, flags)
    }

    private fun requestCodeFor(event: String): Int = REQUEST_CODE_BASE + EVENTS.indexOf(event)

    private fun scheduleZenithWindow(
        context: Context,
        alarmManager: AlarmManager,
        triggerAt: Long
    ) {
        val pi = zenithWindowPendingIntent(context, mutableFlag = true) ?: return
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Logger.d(TAG, "Scheduled zenith window start at $triggerAt")
        } catch (e: SecurityException) {
            Logger.w(TAG, "Cannot schedule zenith window — permission denied", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to schedule zenith window", e)
        }
    }

    private fun zenithWindowPendingIntent(context: Context, mutableFlag: Boolean): PendingIntent? {
        val intent = Intent(context, SolarEventReceiver::class.java).apply {
            action = SolarEventReceiver.ACTION_ZENITH_WINDOW_START
        }
        val flags = if (mutableFlag) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, ZENITH_WINDOW_REQUEST_CODE, intent, flags)
    }

    private fun eventTimes(data: AstronomyDataEntity): List<Pair<String, Long>> = listOf(
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN to data.astroDawn,
        AppConstants.SOLAR_EVENT_NAUTICAL_DAWN     to data.nauticalDawn,
        AppConstants.SOLAR_EVENT_CIVIL_DAWN        to data.civilDawn,
        AppConstants.SOLAR_EVENT_SUNRISE           to data.sunrise,
        AppConstants.SOLAR_EVENT_SOLAR_NOON        to data.solarNoon,
        AppConstants.SOLAR_EVENT_SUNSET            to data.sunset,
        AppConstants.SOLAR_EVENT_CIVIL_DUSK        to data.civilDusk,
        AppConstants.SOLAR_EVENT_NAUTICAL_DUSK     to data.nauticalDusk,
        AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK to data.astroDusk
    )

    private const val REQUEST_CODE_BASE = 9_500
    private const val ZENITH_WINDOW_REQUEST_CODE = 9_600
}
