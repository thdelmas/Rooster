package com.rooster.rooster.domain.usecase

import javax.inject.Inject

/**
 * Pure computation for "Smart" alarm mode: derives a wake time from
 *   - the next sunrise + a user-chosen offset (the primary anchor)
 *   - a mandatory wake-up time (hard upper bound; alarm never fires later)
 *   - a target sleep duration (used only as a last-resort fallback when
 *     neither sunrise nor mandatory wake is available)
 *
 * Rule:
 *   - If sunrise is known, target = sunrise + offset (caller rolls to the
 *     next future occurrence).
 *   - Otherwise, target = next mandatory wake.
 *   - Otherwise, target = now + targetSleep.
 *   - Mandatory wake is then applied as a hard cap.
 *
 * Target sleep does NOT push the wake time forward — bedtime is the user's
 * responsibility, and consistent sunrise-anchored wakes are the health goal.
 */
class ComputeSmartWakeUseCase @Inject constructor() {

    data class Input(
        val nowMillis: Long,
        val targetSleepMinutes: Int,
        val mandatoryWakeMillis: Long?,
        val sunriseAnchorMillis: Long?,
    )

    fun execute(input: Input): Long {
        val initial = input.sunriseAnchorMillis
            ?: input.mandatoryWakeMillis
            ?: (input.nowMillis + input.targetSleepMinutes.coerceAtLeast(0) * 60_000L)

        return input.mandatoryWakeMillis?.let { minOf(initial, it) } ?: initial
    }
}
