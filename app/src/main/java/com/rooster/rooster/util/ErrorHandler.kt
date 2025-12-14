package com.rooster.rooster.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Centralized error handling utility
 */
object ErrorHandler {
    
    const val TAG = "ErrorHandler"
    
    /**
     * Handle general exceptions with user feedback
     */
    fun handleError(context: Context, error: Throwable, userMessage: String? = null) {
        Logger.e(TAG, "Error occurred: ${error.message}", error)
        
        val message = userMessage ?: getDefaultErrorMessage(error)
        showErrorToast(context, message)
    }
    
    /**
     * Handle errors without user feedback (silent logging)
     */
    fun logError(tag: String, message: String, error: Throwable? = null) {
        Logger.e(tag, message, error)
    }
    
    /**
     * Handle warnings
     */
    fun logWarning(tag: String, message: String) {
        Logger.w(tag, message)
    }
    
    /**
     * Show error message to user
     */
    private fun showErrorToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Get default error message based on exception type
     */
    private fun getDefaultErrorMessage(error: Throwable): String {
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException -> "Network error. Please check your internet connection."
            
            is SecurityException -> "Permission denied. Please grant required permissions."
            
            is IllegalArgumentException,
            is IllegalStateException -> "Invalid operation: ${error.message}"
            
            is java.io.IOException -> "File operation failed: ${error.message}"
            
            else -> "An error occurred: ${error.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Create a coroutine exception handler
     */
    fun createCoroutineExceptionHandler(
        context: Context? = null,
        tag: String = TAG,
        onError: ((Throwable) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, exception ->
            Logger.e(tag, "Coroutine exception", exception)
            
            context?.let {
                handleError(it, exception)
            }
            
            onError?.invoke(exception)
        }
    }
    
    /**
     * Wrap a potentially failing operation with error handling
     */
    inline fun <T> safely(
        tag: String = TAG,
        noinline onError: ((Throwable) -> Unit)? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            Logger.e(tag, "Safe operation failed", e)
            onError?.invoke(e)
            null
        }
    }
    
    /**
     * Execute a suspend function safely with error handling
     */
    suspend inline fun <T> safelySuspend(
        tag: String = TAG,
        noinline onError: ((Throwable) -> Unit)? = null,
        crossinline block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            Logger.e(tag, "Safe suspend operation failed", e)
            onError?.invoke(e)
            null
        }
    }
}

/**
 * Extension function to handle Result type errors
 */
fun <T> Result<T>.onFailureLog(tag: String, message: String) {
    onFailure { error ->
        ErrorHandler.logError(tag, message, error)
    }
}

/**
 * Extension function to handle Result type errors with user feedback
 */
fun <T> Result<T>.onFailureShow(context: Context, userMessage: String? = null) {
    onFailure { error ->
        ErrorHandler.handleError(context, error, userMessage)
    }
}
