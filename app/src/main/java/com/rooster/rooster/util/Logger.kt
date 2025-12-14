package com.rooster.rooster.util

import android.util.Log

/**
 * Centralized logging utility with consistent tag naming, log levels, and sensitive data sanitization.
 * 
 * Features:
 * - Debug logs are automatically disabled in release builds
 * - Sensitive data (passwords, tokens, keys) is sanitized
 * - Consistent tag naming with prefix
 * - Configurable minimum log level
 */
object Logger {
    private const val DEFAULT_TAG = "Rooster"
    private const val MAX_TAG_LENGTH = 23 // Android's limit
    
    // Minimum log level (can be configured)
    // In release builds, debug logs are automatically disabled
    // BuildConfig.DEBUG is checked at compile time, so we check if the class exists
    private val isDebugEnabled: Boolean = 
        try {
            val buildConfigClass = Class.forName("com.rooster.rooster.BuildConfig")
            buildConfigClass.getDeclaredField("DEBUG").getBoolean(null)
        } catch (e: ClassNotFoundException) {
            // BuildConfig not available yet (e.g., during annotation processing)
            true // Default to enabled during development
        } catch (e: Exception) {
            true // Default to enabled if there's any issue
        }
    
    // Patterns for sensitive data that should be sanitized
    private val sensitivePatterns = listOf(
        Regex("(?i)(password|passwd|pwd)\\s*[:=]\\s*[^\\s,}]+", RegexOption.IGNORE_CASE),
        Regex("(?i)(token|api[_-]?key|secret|auth[_-]?key)\\s*[:=]\\s*[^\\s,}]+", RegexOption.IGNORE_CASE),
        Regex("(?i)(authorization|bearer)\\s+[^\\s]+", RegexOption.IGNORE_CASE),
        Regex("(?i)(credit[_-]?card|card[_-]?number|cc[_-]?number)\\s*[:=]\\s*[^\\s,}]+", RegexOption.IGNORE_CASE),
    )
    
    /**
     * Log debug message (only in debug builds)
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebugEnabled) return
        
        val fullTag = formatTag(tag)
        val sanitizedMessage = sanitizeSensitiveData(message)
        if (throwable != null) {
            Log.d(fullTag, sanitizedMessage, throwable)
        } else {
            Log.d(fullTag, sanitizedMessage)
        }
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        val sanitizedMessage = sanitizeSensitiveData(message)
        if (throwable != null) {
            Log.i(fullTag, sanitizedMessage, throwable)
        } else {
            Log.i(fullTag, sanitizedMessage)
        }
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        val sanitizedMessage = sanitizeSensitiveData(message)
        if (throwable != null) {
            Log.w(fullTag, sanitizedMessage, throwable)
        } else {
            Log.w(fullTag, sanitizedMessage)
        }
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = formatTag(tag)
        val sanitizedMessage = sanitizeSensitiveData(message)
        if (throwable != null) {
            Log.e(fullTag, sanitizedMessage, throwable)
        } else {
            Log.e(fullTag, sanitizedMessage)
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
    
    /**
     * Sanitize sensitive data from log messages
     */
    private fun sanitizeSensitiveData(message: String): String {
        var sanitized = message
        sensitivePatterns.forEach { pattern ->
            sanitized = pattern.replace(sanitized) { matchResult ->
                val match = matchResult.value
                val keyValue = match.split(Regex("[:=]"), 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim()
                    // Replace value with masked version
                    "$key: ***REDACTED***"
                } else {
                    // For patterns like "Bearer token123", replace token part
                    val parts = match.split(Regex("\\s+"), 2)
                    if (parts.size == 2) {
                        "${parts[0]} ***REDACTED***"
                    } else {
                        "***REDACTED***"
                    }
                }
            }
        }
        return sanitized
    }
}

