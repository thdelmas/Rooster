package com.rooster.rooster.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import com.rooster.rooster.R

/**
 * Single monthly contribution popup with three parallel actions: donate, review, feedback.
 *
 * Hard constraints (see android-apps-contribution-popup-guide.md):
 *  - All three actions are presented at once. No sequential cascade.
 *  - The app does not record which action the user picked, only that the popup was shown.
 *  - There is no "don't ask again" — the popup re-appears on the next cadence regardless.
 *  - Donate hands off to the external donation URL; the app never learns whether the user paid.
 */
object SupportPromptDialog {

    fun showIfEligible(activity: Activity) {
        if (!SupportPromptHelper.shouldShowPrompt(activity)) return
        SupportPromptHelper.markPromptShown(activity)
        show(activity)
    }

    fun showFromSettings(activity: Activity) {
        show(activity)
    }

    private fun show(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.support_prompt_title)
            .setMessage(R.string.support_prompt_message)
            .setPositiveButton(R.string.support_prompt_donate) { dialog, _ ->
                openUrl(activity, activity.getString(R.string.support_donate_url))
                dialog.dismiss()
            }
            .setNeutralButton(R.string.support_prompt_review) { dialog, _ ->
                launchInAppReview(activity)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.support_prompt_feedback) { dialog, _ ->
                openUrl(activity, activity.getString(R.string.support_feedback_url))
                dialog.dismiss()
            }
            .show()
    }

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
