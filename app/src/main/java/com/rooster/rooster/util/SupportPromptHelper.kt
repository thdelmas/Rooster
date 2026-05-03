package com.rooster.rooster.util

import android.content.Context
import com.rooster.rooster.util.AppConstants.MILLIS_PER_DAY

/**
 * Tracks eligibility for the monthly contribution popup.
 *
 * Hard constraints (see android-apps-contribution-popup-guide.md):
 *  - Popup re-shows on a fixed cadence regardless of which action the user picked.
 *  - No per-user differentiation: no donor flag, no opt-out, no branch tracking.
 *  - Only state persisted is the install timestamp (for the post-install grace period)
 *    and the last-shown timestamp (for the cadence).
 */
object SupportPromptHelper {

    private const val PREFS_NAME = "rooster_prefs"

    private const val KEY_FIRST_SEEN = "support_first_seen_millis"
    private const val KEY_LAST_PROMPT = "support_last_prompt_millis"

    private const val GRACE_DAYS_AFTER_INSTALL = 7L
    private const val MIN_DAYS_BETWEEN_PROMPTS = 30L

    fun recordFirstSeenIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_FIRST_SEEN, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
        }
    }

    fun shouldShowPrompt(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L)
        if (firstSeen == 0L) return false
        if (now - firstSeen < GRACE_DAYS_AFTER_INSTALL * MILLIS_PER_DAY) return false

        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        return now - lastPrompt >= MIN_DAYS_BETWEEN_PROMPTS * MILLIS_PER_DAY
    }

    fun markPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
    }
}
