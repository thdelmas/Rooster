package com.rooster.rooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.SolarEventPrefs
import com.rooster.rooster.util.SolarEventScheduler
import com.rooster.rooster.util.SolarEventTone
import com.rooster.rooster.util.SolarEventVibration
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
        if (intent.action != ACTION_SOLAR_EVENT) return
        val event = intent.getStringExtra(EXTRA_EVENT) ?: run {
            Logger.w(TAG, "Solar event broadcast missing event extra")
            return
        }

        val soundOn = SolarEventPrefs.isSoundEnabled(context)
        val vibrateOn = SolarEventPrefs.isVibrationEnabled(context)
        Logger.i(TAG, "Solar event fired: $event (sound=$soundOn, vibrate=$vibrateOn)")

        // Reschedule the next event regardless of whether anything plays —
        // we want the chain to keep advancing even if the user has muted.
        val appContext = context.applicationContext
        scope.launch {
            runCatching { SolarEventScheduler.scheduleUpcoming(appContext) }
                .onFailure { Logger.e(TAG, "Failed to reschedule after event", it) }
        }

        if (!soundOn && !vibrateOn) return

        val toneMs = if (soundOn) SolarEventTone.durationMsFor(event) else 0L
        val vibMs = if (vibrateOn) SolarEventVibration.durationMsFor(event) else 0L
        if (toneMs == 0L && vibMs == 0L) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                if (vibrateOn) SolarEventVibration.vibrate(appContext, event)
                if (soundOn) SolarEventTone.play(event)
            } catch (e: Exception) {
                Logger.e(TAG, "Error playing solar event cue", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SOLAR_EVENT = "com.rooster.rooster.ACTION_SOLAR_EVENT"
        const val EXTRA_EVENT = "solar_event"
        private const val TAG = "SolarEventReceiver"

        // Long-lived scope so goAsync() coroutines aren't tied to a receiver
        // instance the system may have already discarded.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
