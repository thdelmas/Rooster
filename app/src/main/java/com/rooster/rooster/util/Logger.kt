package com.rooster.rooster.util

import android.util.Log

/**
 * Centralized logging utility with consistent tag naming and log levels
 */
object Logger {
    private const val DEFAULT_TAG = "Rooster"
    private const val MAX_TAG_LENGTH = 23 // Android's limit
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        if (throwable != null) {
            Log.d(fullTag, message, throwable)
        } else {
            Log.d(fullTag, message)
        }
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        if (throwable != null) {
            Log.i(fullTag, message, throwable)
        } else {
            Log.i(fullTag, message)
        }
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        if (throwable != null) {
            Log.w(fullTag, message, throwable)
        } else {
            Log.w(fullTag, message)
        }
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        if (throwable != null) {
            Log.e(fullTag, message, throwable)
        } else {
            Log.e(fullTag, message)
        }
    }
    
    /**
     * Format tag to ensure it doesn't exceed Android's limit
     */
    private fun formatTag(tag: String): String {
        val fullTag = if (tag.startsWith(DEFAULT_TAG)) tag else "$DEFAULT_TAG:$tag"
        return if (fullTag.length > MAX_TAG_LENGTH) {
            fullTag.substring(0, MAX_TAG_LENGTH)
        } else {
            fullTag
        }
    }
}

