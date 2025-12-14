package com.rooster.rooster.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.FileNotFoundException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.net.ConnectException

/**
 * Maps technical errors to user-friendly messages with recovery suggestions
 */
object ErrorMessageMapper {
    
    /**
     * Represents a user-friendly error message with recovery suggestions
     */
    data class UserFriendlyError(
        val message: String,
        val recoverySuggestion: String? = null,
        val actionHint: String? = null
    ) {
        fun getFullMessage(): String {
            return buildString {
                append(message)
                recoverySuggestion?.let {
                    append("\n\n")
                    append(it)
                }
                actionHint?.let {
                    append("\n\n")
                    append("💡 $it")
                }
            }
        }
    }
    
    /**
     * Get user-friendly error message based on exception type and context
     */
    fun mapError(error: Throwable, context: String? = null): UserFriendlyError {
        return when (error) {
            // Network errors
            is UnknownHostException -> UserFriendlyError(
                message = "Unable to connect to the internet",
                recoverySuggestion = "Please check your internet connection and try again.",
                actionHint = "Make sure Wi-Fi or mobile data is enabled"
            )
            
            is SocketTimeoutException -> UserFriendlyError(
                message = "Connection timed out",
                recoverySuggestion = "The server took too long to respond. Please check your internet connection and try again.",
                actionHint = "Try again in a moment, or check if your connection is stable"
            )
            
            is ConnectException -> UserFriendlyError(
                message = "Cannot reach the server",
                recoverySuggestion = "Please check your internet connection and try again.",
                actionHint = "Verify your network settings"
            )
            
            // Permission errors
            is SecurityException -> {
                val permissionHint = when {
                    error.message?.contains("location", ignoreCase = true) == true -> 
                        "Go to Settings > App Permissions > Location"
                    error.message?.contains("storage", ignoreCase = true) == true -> 
                        "Go to Settings > App Permissions > Storage"
                    error.message?.contains("notification", ignoreCase = true) == true -> 
                        "Go to Settings > App Permissions > Notifications"
                    else -> "Go to Settings > App Permissions"
                }
                
                UserFriendlyError(
                    message = "Permission required",
                    recoverySuggestion = "This feature needs permission to work. Please grant the required permission in your device settings.",
                    actionHint = permissionHint
                )
            }
            
            // File/Storage errors
            is FileNotFoundException -> {
                val fileContext = when {
                    context?.contains("backup", ignoreCase = true) == true -> "backup file"
                    context?.contains("export", ignoreCase = true) == true -> "export file"
                    context?.contains("import", ignoreCase = true) == true -> "import file"
                    else -> "file"
                }
                
                UserFriendlyError(
                    message = "File not found",
                    recoverySuggestion = "The $fileContext could not be found. Please make sure the file exists and try again.",
                    actionHint = "Check if the file was moved or deleted"
                )
            }
            
            is java.io.IOException -> {
                val ioContext = when {
                    context?.contains("backup", ignoreCase = true) == true -> 
                        "saving or loading your backup"
                    context?.contains("export", ignoreCase = true) == true -> 
                        "exporting your alarms"
                    context?.contains("import", ignoreCase = true) == true -> 
                        "importing your alarms"
                    context?.contains("read", ignoreCase = true) == true -> 
                        "reading the file"
                    context?.contains("write", ignoreCase = true) == true -> 
                        "saving the file"
                    else -> "accessing the file"
                }
                
                UserFriendlyError(
                    message = "Unable to access file",
                    recoverySuggestion = "There was a problem $ioContext. Please make sure you have enough storage space and try again.",
                    actionHint = "Check available storage space or try a different location"
                )
            }
            
            // Validation errors
            is IllegalArgumentException -> {
                val validationMessage = when {
                    error.message?.contains("label", ignoreCase = true) == true -> 
                        "Please enter a name for your alarm"
                    error.message?.contains("time", ignoreCase = true) == true -> 
                        "Please set a valid time for your alarm"
                    error.message?.contains("mode", ignoreCase = true) == true -> 
                        "Please select a valid alarm mode"
                    error.message?.contains("day", ignoreCase = true) == true -> 
                        "Please select at least one day for your alarm"
                    error.message?.contains("validation", ignoreCase = true) == true -> 
                        "Some alarm settings are invalid"
                    else -> "Invalid alarm settings"
                }
                
                UserFriendlyError(
                    message = validationMessage,
                    recoverySuggestion = "Please check your alarm settings and make sure all required fields are filled correctly.",
                    actionHint = "Review the alarm form and fix any highlighted errors"
                )
            }
            
            is IllegalStateException -> {
                val stateMessage = when {
                    error.message?.contains("past", ignoreCase = true) == true -> 
                        "The alarm time is in the past"
                    error.message?.contains("disabled", ignoreCase = true) == true -> 
                        "This alarm is currently disabled"
                    error.message?.contains("calculated", ignoreCase = true) == true -> 
                        "Unable to calculate alarm time"
                    error.message?.contains("AlarmManager", ignoreCase = true) == true -> 
                        "Unable to schedule the alarm"
                    else -> "Invalid operation"
                }
                
                UserFriendlyError(
                    message = stateMessage,
                    recoverySuggestion = when {
                        error.message?.contains("past", ignoreCase = true) == true -> 
                            "Please set the alarm for a future time."
                        error.message?.contains("disabled", ignoreCase = true) == true -> 
                            "Enable the alarm first, then try again."
                        else -> "Please try again or restart the app if the problem persists."
                    },
                    actionHint = when {
                        error.message?.contains("past", ignoreCase = true) == true -> 
                            "Set the alarm time to a future date and time"
                        error.message?.contains("disabled", ignoreCase = true) == true -> 
                            "Toggle the alarm switch to enable it"
                        else -> "Try refreshing or restarting the app"
                    }
                )
            }
            
            // Database errors - handled by generic exception handler
            // SQLite exceptions are typically wrapped or caught at repository level
            
            // JSON parsing errors (for backup)
            is org.json.JSONException -> {
                val jsonContext = when {
                    context?.contains("backup", ignoreCase = true) == true -> 
                        "The backup file appears to be corrupted or in an unsupported format"
                    context?.contains("import", ignoreCase = true) == true -> 
                        "The import file is not valid"
                    else -> "The file format is invalid"
                }
                
                UserFriendlyError(
                    message = "Invalid file format",
                    recoverySuggestion = "$jsonContext. Please make sure you're using a valid backup file from this app.",
                    actionHint = "Try exporting a new backup or use a backup file created by this app"
                )
            }
            
            // Arithmetic errors (for time calculations)
            is ArithmeticException -> UserFriendlyError(
                message = "Calculation error",
                recoverySuggestion = "There was a problem calculating the alarm time. Please check your alarm settings.",
                actionHint = "Verify your alarm time and date settings are correct"
            )
            
            // Null pointer (shouldn't happen, but handle gracefully)
            is NullPointerException -> UserFriendlyError(
                message = "Unexpected error",
                recoverySuggestion = "Something went wrong. Please try again.",
                actionHint = "Try restarting the app if the problem continues"
            )
            
            // Generic exception with context-aware messages
            else -> {
                val genericMessage = when {
                    context?.contains("backup", ignoreCase = true) == true -> 
                        "Unable to complete backup"
                    context?.contains("export", ignoreCase = true) == true -> 
                        "Unable to export alarms"
                    context?.contains("import", ignoreCase = true) == true -> 
                        "Unable to import alarms"
                    context?.contains("save", ignoreCase = true) == true -> 
                        "Unable to save alarm"
                    context?.contains("delete", ignoreCase = true) == true -> 
                        "Unable to delete alarm"
                    context?.contains("schedule", ignoreCase = true) == true -> 
                        "Unable to schedule alarm"
                    context?.contains("location", ignoreCase = true) == true -> 
                        "Unable to get location"
                    else -> "Something went wrong"
                }
                
                UserFriendlyError(
                    message = genericMessage,
                    recoverySuggestion = "Please try again. If the problem persists, try restarting the app.",
                    actionHint = "Check your connection and settings, then try again"
                )
            }
        }
    }
    
    /**
     * Check if device has internet connection
     */
    fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Get context-specific error message for common operations
     */
    fun getContextualError(operation: String, error: Throwable): UserFriendlyError {
        return mapError(error, context = operation.lowercase())
    }
}
