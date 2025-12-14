package com.rooster.rooster.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Centralized error handling utility with user-friendly error messages
 */
object ErrorHandler {
    
    const val TAG = "ErrorHandler"
    
    /**
     * Handle general exceptions with user feedback
     * @param context Android context for showing error messages
     * @param error The exception that occurred
     * @param userMessage Optional custom user message (if null, will use ErrorMessageMapper)
     * @param operationContext Optional context about the operation (e.g., "backup", "save", "import")
     * @param showRecoverySuggestion Whether to show recovery suggestions (default: true)
     */
    fun handleError(
        context: Context, 
        error: Throwable, 
        userMessage: String? = null,
        operationContext: String? = null,
        showRecoverySuggestion: Boolean = true
    ) {
        // Always log the technical error for debugging
        Logger.e(TAG, "Error occurred: ${error.message}", error)
        
        val message = if (userMessage != null) {
            userMessage
        } else {
            val friendlyError = if (operationContext != null) {
                ErrorMessageMapper.getContextualError(operationContext, error)
            } else {
                ErrorMessageMapper.mapError(error)
            }
            
            if (showRecoverySuggestion) {
                friendlyError.getFullMessage()
            } else {
                friendlyError.message
            }
        }
        
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
            // Split long messages into multiple toasts if needed
            // Toast has a limit, so we'll show the first part
            val displayMessage = if (message.length > 200) {
                message.take(200) + "..."
            } else {
                message
            }
            Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Create a coroutine exception handler
     */
    fun createCoroutineExceptionHandler(
        context: Context? = null,
        tag: String = TAG,
        operationContext: String? = null,
        onError: ((Throwable) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, exception ->
            Logger.e(tag, "Coroutine exception", exception)
            
            context?.let {
                handleError(it, exception, operationContext = operationContext)
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
fun <T> Result<T>.onFailureShow(
    context: Context, 
    userMessage: String? = null,
    operationContext: String? = null
) {
    onFailure { error ->
        ErrorHandler.handleError(context, error, userMessage, operationContext)
    }
}
