package com.rooster.rooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.SolarEventPrefs
import com.rooster.rooster.util.SolarEventScheduler
import com.rooster.rooster.util.SolarEventTone
import com.rooster.rooster.util.SolarEventVibration
import com.rooster.rooster.util.ZenithGongTone
import com.rooster.rooster.util.ZenithNotification
import com.rooster.rooster.util.ZenithVibration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires the per-event tone + vibration signature when AlarmManager triggers
 * a scheduled solar-event broadcast. Uses goAsync() to extend the receiver's
 * lifetime past onReceive() so the bell tone has time to ring out.
 */
class SolarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext

        if (intent.action == ACTION_ZENITH_WINDOW_START) {
            Logger.i(TAG, "Zenith window opening — posting notification + tactile signature")
            ZenithNotification.show(appContext)
            // Reschedule so tomorrow's zenith window gets queued up.
            scope.launch {
                runCatching { SolarEventScheduler.scheduleUpcoming(appContext) }
                    .onFailure { Logger.e(TAG, "Failed to reschedule after zenith window", it) }
            }
            return
        }

        if (intent.action != ACTION_SOLAR_EVENT) return
        val event = intent.getStringExtra(EXTRA_EVENT) ?: run {
            Logger.w(TAG, "Solar event broadcast missing event extra")
            return
        }

        val soundOn = SolarEventPrefs.isSoundEnabled(context)
        val vibrateOn = SolarEventPrefs.isVibrationEnabled(context)
        val gongOn = SolarEventPrefs.isZenithGongEnabled(context) &&
            event == AppConstants.SOLAR_EVENT_SOLAR_NOON
        Logger.i(TAG, "Solar event fired: $event (sound=$soundOn, vibrate=$vibrateOn, gong=$gongOn)")

        // Reschedule the next event regardless of whether anything plays —
        // we want the chain to keep advancing even if the user has muted.
        scope.launch {
            runCatching { SolarEventScheduler.scheduleUpcoming(appContext) }
                .onFailure { Logger.e(TAG, "Failed to reschedule after event", it) }
        }

        // At noon, the gong takes the place of the bell so the two don't overlap.
        val playBell = soundOn && !gongOn
        if (!playBell && !vibrateOn && !gongOn) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                if (gongOn) {
                    // Unique tactile signature paired with the gong, so the user
                    // still feels the moment when audio is suppressed. Replaces
                    // the standard noon "crown peal" vibration.
                    ZenithVibration.vibrate(appContext)
                    ZenithGongTone.play()
                } else {
                    if (vibrateOn) SolarEventVibration.vibrate(appContext, event)
                    if (playBell) SolarEventTone.play(event)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error playing solar event cue", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SOLAR_EVENT = "com.rooster.rooster.ACTION_SOLAR_EVENT"
        const val ACTION_ZENITH_WINDOW_START = "com.rooster.rooster.ACTION_ZENITH_WINDOW_START"
        const val EXTRA_EVENT = "solar_event"
        private const val TAG = "SolarEventReceiver"

        // Long-lived scope so goAsync() coroutines aren't tied to a receiver
        // instance the system may have already discarded.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
