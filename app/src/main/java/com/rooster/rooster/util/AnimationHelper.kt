package com.rooster.rooster.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Utility class for creating smooth animations throughout the app
 */
object AnimationHelper {
    
    private const val EXPAND_DURATION = 300L
    private const val COLLAPSE_DURATION = 250L
    private const val FADE_DURATION = 200L
    private const val SCALE_DURATION = 150L
    
    /**
     * Expands a view with a smooth animation
     */
    fun expand(view: View, onComplete: (() -> Unit)? = null) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        val targetHeight = view.measuredHeight
        
        // Set initial state
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE
        
        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.duration = EXPAND_DURATION
        animator.interpolator = DecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                onComplete?.invoke()
            }
        })
        
        animator.start()
    }
    
    /**
     * Collapses a view with a smooth animation
     */
    fun collapse(view: View, onComplete: (() -> Unit)? = null) {
        val initialHeight = view.height
        
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.duration = COLLAPSE_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                onComplete?.invoke()
            }
        })
        
        animator.start()
    }
    
    /**
     * Fades in a view
     */
    fun fadeIn(view: View, duration: Long = FADE_DURATION) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(null)
            .start()
    }
    
    /**
     * Fades out a view
     */
    fun fadeOut(view: View, duration: Long = FADE_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Scales a view with a bounce effect
     */
    fun scaleWithBounce(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(SCALE_DURATION / 2)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SCALE_DURATION)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
    }
    
    /**
     * Rotates a view (useful for expand/collapse indicators)
     */
    fun rotate(view: View, fromDegrees: Float, toDegrees: Float, duration: Long = EXPAND_DURATION) {
        ObjectAnimator.ofFloat(view, "rotation", fromDegrees, toDegrees).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}

// Extension to make ViewGroup.LayoutParams available
private object ViewGroup {
    object LayoutParams {
        const val WRAP_CONTENT = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    }
}










