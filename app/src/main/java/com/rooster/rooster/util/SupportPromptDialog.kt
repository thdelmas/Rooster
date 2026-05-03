package com.rooster.rooster.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import com.rooster.rooster.R

/**
 * Drives the monthly support cascade: Buy Me a Coffee → Play Store review → feedback survey.
 *
 * Each rung exposes three actions:
 *  - Primary CTA  → opens the relevant URL and records the click.
 *  - "Maybe later" → dismisses; the cascade does NOT advance, prompt returns next month.
 *  - "No thanks"  → advances to the next rung within this same session, OR opts out on the last rung.
 */
object SupportPromptDialog {

    fun showIfEligible(activity: Activity) {
        if (!SupportPromptHelper.shouldShowPrompt(activity)) return
        SupportPromptHelper.markPromptShown(activity)
        show(activity, SupportPromptHelper.nextRung(activity))
    }

    fun showFromSettings(activity: Activity) {
        show(activity, SupportPromptHelper.nextRung(activity))
    }

    private fun show(activity: Activity, rung: SupportPromptHelper.Rung) {
        if (activity.isFinishing || activity.isDestroyed) return
        when (rung) {
            SupportPromptHelper.Rung.DONATE -> showDonate(activity)
            SupportPromptHelper.Rung.REVIEW -> showReview(activity)
            SupportPromptHelper.Rung.SURVEY -> showSurvey(activity)
        }
    }

    private fun showDonate(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.support_donate_title)
            .setMessage(R.string.support_donate_message)
            .setPositiveButton(R.string.support_donate_cta) { dialog, _ ->
                SupportPromptHelper.markDonateClicked(activity)
                openUrl(activity, activity.getString(R.string.support_donate_url))
                dialog.dismiss()
            }
            .setNeutralButton(R.string.support_maybe_later) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(R.string.support_no_thanks) { dialog, _ ->
                dialog.dismiss()
                advance(activity, SupportPromptHelper.Rung.DONATE)
            }
            .show()
    }

    private fun showReview(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.support_review_title)
            .setMessage(R.string.support_review_message)
            .setPositiveButton(R.string.support_review_cta) { dialog, _ ->
                SupportPromptHelper.markReviewClicked(activity)
                launchInAppReview(activity)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.support_maybe_later) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(R.string.support_no_thanks) { dialog, _ ->
                dialog.dismiss()
                advance(activity, SupportPromptHelper.Rung.REVIEW)
            }
            .show()
    }

    /**
     * Triggers Google's in-app review overlay. The flow may silently no-op when quota
     * is exceeded or the user has already reviewed — Google's API gives us no way to
     * tell. On failure to obtain a ReviewInfo (no Play Store, no network on first call,
     * sideloaded build), we fall back to the Play Store deep link so the user always
     * gets *somewhere* useful from clicking the CTA.
     */
    private fun launchInAppReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                Logger.w("SupportPromptDialog", "In-app review unavailable, falling back to Play Store", request.exception)
                openPlayStore(activity)
            }
        }
    }

    private fun showSurvey(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.support_survey_title)
            .setMessage(R.string.support_survey_message)
            .setPositiveButton(R.string.support_survey_cta) { dialog, _ ->
                SupportPromptHelper.markSurveyClicked(activity)
                openUrl(activity, activity.getString(R.string.support_survey_url))
                dialog.dismiss()
            }
            .setNeutralButton(R.string.support_maybe_later) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(R.string.support_dont_ask_again) { dialog, _ ->
                SupportPromptHelper.setOptedOut(activity, true)
                dialog.dismiss()
            }
            .show()
    }

    private fun advance(activity: Activity, current: SupportPromptHelper.Rung) {
        val next = SupportPromptHelper.rungAfter(activity, current)
        if (next != null) {
            show(activity, next)
        } else {
            SupportPromptHelper.setOptedOut(activity, true)
        }
    }

    private fun openUrl(activity: Activity, url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Logger.w("SupportPromptDialog", "No activity to open $url")
        }
    }

    private fun openPlayStore(activity: Activity) {
        val pkg = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            )
        } catch (e: ActivityNotFoundException) {
            openUrl(activity, "https://play.google.com/store/apps/details?id=$pkg")
        }
    }
}
