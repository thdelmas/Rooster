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
     * Reconciles the DB and AlarmManager state with the current Smart Wake prefs.
     * Returns the computed next-fire epoch millis, or null if disabled or scheduling failed.
     */
    suspend fun sync(context: Context): Long? {
        val app = context.applicationContext as? RoosterApplication ?: run {
            Logger.w(TAG, "RoosterApplication not available; skipping sync")
            return null
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
            return null
        }

        val alarm = existing ?: run {
            val newId = repo.insertAlarm(buildCreation())
            repo.getAlarmById(newId)
                ?: run {
                    Logger.e(TAG, "Failed to read back inserted Smart Wake alarm $newId")
                    return null
                }
        }

        if (!alarm.enabled) {
            repo.updateAlarmEnabled(alarm.id, true)
        }

        val toSchedule = alarm.copy(enabled = true)
        val result = scheduler.scheduleAlarm(toSchedule)
        return result.fold(
            onSuccess = { repo.getAlarmById(alarm.id)?.calculatedTime },
            onFailure = {
                Logger.e(TAG, "Failed to schedule Smart Wake alarm", it)
                null
            }
        )
    }

    /**
     * Returns the currently scheduled Smart Wake fire time, or null if no
     * managed alarm exists. Used by Settings to render the preview.
     */
    suspend fun currentScheduledMillis(context: Context): Long? {
        val app = context.applicationContext as? RoosterApplication ?: return null
        val repo = app.provideAlarmRepository()
        return repo.getAllAlarms()
            .firstOrNull { it.mode == AppConstants.ALARM_MODE_SMART && it.enabled }
            ?.calculatedTime
            ?.takeIf { it > 0L }
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
