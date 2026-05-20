package com.rooster.rooster.util

import android.content.Context
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.RoosterApplication

/**
 * Owns the single auto-managed "Smart Wake" alarm. Settings calls
 * [sync] whenever the enable toggle or any Smart Wake pref changes; we
 * reconcile the DB and AlarmManager state to match the prefs.
 *
 * Identity rule: at most one enabled alarm with mode = ALARM_MODE_SMART
 * exists at a time. Toggling off cancels and deletes it.
 */
object SmartWakeScheduler {

    private const val TAG = "SmartWakeScheduler"
    private const val SMART_ALARM_LABEL = "Smart Wake"

    /**
     * Status returned by [sync] and [currentStatus]. `sunriseAnchored` is
     * false when astronomy reports no sunrise (polar night/midnight sun, or
     * data not yet fetched) — in that case the alarm fell back to mandatory
     * wake or now + target sleep.
     */
    data class Status(
        val nextMillis: Long?,
        val sunriseAnchored: Boolean,
    )

    /**
     * Reconciles the DB and AlarmManager state with the current Smart Wake prefs.
     * Returns the computed next-fire millis and whether sunrise data was used.
     */
    suspend fun sync(context: Context): Status {
        val empty = Status(nextMillis = null, sunriseAnchored = false)
        val app = context.applicationContext as? RoosterApplication ?: run {
            Logger.w(TAG, "RoosterApplication not available; skipping sync")
            return empty
        }
        val repo = app.provideAlarmRepository()
        val scheduler = app.provideScheduleAlarmUseCase()

        val existing = repo.getAllAlarms().firstOrNull { it.mode == AppConstants.ALARM_MODE_SMART }
        val enabled = SmartWakePrefs.isEnabled(context)

        if (!enabled) {
            existing?.let {
                scheduler.cancelAlarm(it)
                repo.deleteAlarm(it)
                Logger.i(TAG, "Smart Wake disabled — removed managed alarm ${it.id}")
            }
            return empty
        }

        val alarm = existing ?: run {
            val newId = repo.insertAlarm(buildCreation())
            repo.getAlarmById(newId)
                ?: run {
                    Logger.e(TAG, "Failed to read back inserted Smart Wake alarm $newId")
                    return empty
                }
        }

        if (!alarm.enabled) {
            repo.updateAlarmEnabled(alarm.id, true)
        }

        val toSchedule = alarm.copy(enabled = true)
        val nextMillis = scheduler.scheduleAlarm(toSchedule).fold(
            onSuccess = { repo.getAlarmById(alarm.id)?.calculatedTime },
            onFailure = {
                Logger.e(TAG, "Failed to schedule Smart Wake alarm", it)
                null
            }
        )
        return Status(nextMillis, sunriseAnchored = isSunriseAvailable(app))
    }

    /**
     * Returns the currently scheduled Smart Wake status, or an empty status if
     * no managed alarm exists. Used by Settings to render the preview row.
     */
    suspend fun currentStatus(context: Context): Status {
        val app = context.applicationContext as? RoosterApplication
            ?: return Status(null, false)
        val repo = app.provideAlarmRepository()
        val millis = repo.getAllAlarms()
            .firstOrNull { it.mode == AppConstants.ALARM_MODE_SMART && it.enabled }
            ?.calculatedTime
            ?.takeIf { it > 0L }
        return Status(millis, sunriseAnchored = isSunriseAvailable(app))
    }

    private suspend fun isSunriseAvailable(app: RoosterApplication): Boolean {
        val sunrise = app.provideAstronomyRepositoryOrNull()
            ?.getAstronomyData(forceRefresh = false)
            ?.sunrise
            ?: 0L
        return sunrise > 0L
    }

    private fun buildCreation(): AlarmCreation = AlarmCreation(
        label = SMART_ALARM_LABEL,
        enabled = true,
        mode = AppConstants.ALARM_MODE_SMART,
        ringtoneUri = AppConstants.DEFAULT_RINGTONE_URI,
        relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
        relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
        time1 = 0L,
        time2 = 0L,
        calculatedTime = 0L,
    )
}
