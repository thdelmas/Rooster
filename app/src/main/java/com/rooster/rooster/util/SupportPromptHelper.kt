package com.rooster.rooster.util

import android.content.Context
import com.rooster.rooster.util.AppConstants.MILLIS_PER_DAY

/**
 * Tracks eligibility and state for the monthly support-cascade prompt
 * (Buy Me a Coffee → Play Store review → feedback survey).
 *
 * Rules:
 *  - First prompt is gated by [GRACE_DAYS_AFTER_INSTALL] days since first launch.
 *  - Subsequent prompts respect [MIN_DAYS_BETWEEN_PROMPTS].
 *  - "Don't ask again" sets a permanent opt-out.
 *  - Once the user clicks the donate CTA, prompts pause for [DAYS_AFTER_DONATE_CLICK].
 *  - Once the user has reviewed, that rung is skipped in future cascades.
 */
object SupportPromptHelper {

    private const val PREFS_NAME = "rooster_prefs"

    private const val KEY_FIRST_SEEN = "support_first_seen_millis"
    private const val KEY_LAST_PROMPT = "support_last_prompt_millis"
    private const val KEY_OPTED_OUT = "support_opted_out"
    private const val KEY_DONATE_CLICKED_AT = "support_donate_clicked_at"
    private const val KEY_REVIEW_CLICKED_AT = "support_review_clicked_at"
    private const val KEY_SURVEY_CLICKED_AT = "support_survey_clicked_at"

    private const val GRACE_DAYS_AFTER_INSTALL = 7L
    private const val MIN_DAYS_BETWEEN_PROMPTS = 30L
    private const val DAYS_AFTER_DONATE_CLICK = 90L

    enum class Rung { DONATE, REVIEW, SURVEY }

    fun recordFirstSeenIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_FIRST_SEEN, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
        }
    }

    fun shouldShowPrompt(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_OPTED_OUT, false)) return false

        val firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L)
        if (firstSeen == 0L) return false
        if (now - firstSeen < GRACE_DAYS_AFTER_INSTALL * MILLIS_PER_DAY) return false

        val donateClickedAt = prefs.getLong(KEY_DONATE_CLICKED_AT, 0L)
        if (donateClickedAt > 0L && now - donateClickedAt < DAYS_AFTER_DONATE_CLICK * MILLIS_PER_DAY) {
            return false
        }

        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        return now - lastPrompt >= MIN_DAYS_BETWEEN_PROMPTS * MILLIS_PER_DAY
    }

    /** Returns the first rung the cascade should attempt for this prompt cycle. */
    fun nextRung(context: Context): Rung {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when {
            prefs.getLong(KEY_REVIEW_CLICKED_AT, 0L) == 0L && prefs.getLong(KEY_DONATE_CLICKED_AT, 0L) == 0L -> Rung.DONATE
            prefs.getLong(KEY_REVIEW_CLICKED_AT, 0L) == 0L -> Rung.REVIEW
            else -> Rung.SURVEY
        }
    }

    fun rungAfter(context: Context, current: Rung): Rung? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (current) {
            Rung.DONATE -> if (prefs.getLong(KEY_REVIEW_CLICKED_AT, 0L) == 0L) Rung.REVIEW else Rung.SURVEY
            Rung.REVIEW -> Rung.SURVEY
            Rung.SURVEY -> null
        }
    }

    fun markPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
    }

    fun markDonateClicked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_DONATE_CLICKED_AT, System.currentTimeMillis()).apply()
    }

    fun markReviewClicked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_REVIEW_CLICKED_AT, System.currentTimeMillis()).apply()
    }

    fun markSurveyClicked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SURVEY_CLICKED_AT, System.currentTimeMillis()).apply()
    }

    fun setOptedOut(context: Context, optedOut: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OPTED_OUT, optedOut).apply()
    }
}
