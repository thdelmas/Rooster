package com.rooster.rooster.domain.usecase

import javax.inject.Inject

/**
 * Pure computation for "Smart" alarm mode: derives a wake time from
 *   - a target sleep duration (minimum sleep the user wants before waking)
 *   - a mandatory wake-up time (hard upper bound; alarm never fires later)
 *   - an optional solar anchor (preferred wake event, e.g. sunrise)
 *
 * Rule:
 *   - Solar anchor is the preferred target; rolled forward by whole days
 *     until it is at least `now + targetSleep`, so the user gets the sun
 *     AND enough rest.
 *   - Otherwise mandatory wake is the target (already next-occurrence).
 *   - Otherwise `now + targetSleep`.
 *   - Mandatory wake is then applied as a hard cap.
 *
 * `mandatoryWakeMillis` and `solarAnchorMillis` are passed in as the next
 * occurrence after `nowMillis`. The caller is responsible for that rollover.
 */
class ComputeSmartWakeUseCase @Inject constructor() {

    data class Input(
        val nowMillis: Long,
        val targetSleepMinutes: Int,
        val mandatoryWakeMillis: Long?,
        val solarAnchorMillis: Long?
    )

    fun execute(input: Input): Long {
        val targetMinutes = input.targetSleepMinutes.coerceAtLeast(0)
        val earliest = input.nowMillis + targetMinutes * 60_000L

        val initial = when {
            input.solarAnchorMillis != null -> rollForwardUntil(input.solarAnchorMillis, earliest)
            input.mandatoryWakeMillis != null -> input.mandatoryWakeMillis
            else -> earliest
        }

        val capped = input.mandatoryWakeMillis?.let { minOf(initial, it) } ?: initial
        return capped
    }

    private fun rollForwardUntil(start: Long, floor: Long): Long {
        if (start >= floor) return start
        val msPerDay = 24 * 60 * 60 * 1000L
        var t = start
        while (t < floor) t += msPerDay
        return t
    }
}
