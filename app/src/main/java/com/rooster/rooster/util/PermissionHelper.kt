package com.rooster.rooster.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.rooster.rooster.util.Logger
import androidx.core.content.ContextCompat

/**
 * Helper class for managing app permissions
 */
object PermissionHelper {
    
    private const val TAG = "PermissionHelper"
    
    // Permission request codes
    const val REQUEST_CODE_LOCATION = 1001
    const val REQUEST_CODE_NOTIFICATIONS = 1002
    const val REQUEST_CODE_EXACT_ALARM = 1003
    const val REQUEST_CODE_ALL_PERMISSIONS = 1004
    
    /**
     * Check if location permissions are granted
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Check if exact alarm permission is granted (Android 12+)
     */
    fun isExactAlarmPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Get list of required permissions for the current Android version
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.USE_FULL_SCREEN_INTENT,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE
        )
        
        // Add version-specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        
        return permissions
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of requestable runtime permissions (excludes special permissions that require settings)
     */
    private fun getRequestableRuntimePermissions(context: Context): List<String> {
        val allMissing = getMissingPermissions(context)
        
        // Filter out permissions that cannot be requested via requestPermissions():
        // - SCHEDULE_EXACT_ALARM: Must be granted via Settings (Android 12+)
        // - USE_FULL_SCREEN_INTENT, WAKE_LOCK, FOREGROUND_SERVICE: Typically granted via manifest
        return allMissing.filter { permission ->
            when (permission) {
                Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                    // This must be handled separately via Settings
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Logger.d(TAG, "SCHEDULE_EXACT_ALARM cannot be requested via runtime permission, must use Settings")
                    }
                    false
                }
                Manifest.permission.USE_FULL_SCREEN_INTENT,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.FOREGROUND_SERVICE -> {
                    // These are typically granted via manifest, not runtime permissions
                    false
                }
                else -> true
            }
        }
    }
    
    /**
     * Request all required permissions that can be requested at runtime
     * Note: SCHEDULE_EXACT_ALARM must be handled separately via openExactAlarmSettings()
     */
    fun requestAllPermissions(activity: Activity) {
        val requestablePermissions = getRequestableRuntimePermissions(activity)
        
        if (requestablePermissions.isNotEmpty()) {
            Logger.i(TAG, "Requesting runtime permissions: $requestablePermissions")
            ActivityCompat.requestPermissions(
                activity,
                requestablePermissions.toTypedArray(),
                REQUEST_CODE_ALL_PERMISSIONS
            )
        } else {
            Logger.i(TAG, "All requestable runtime permissions already granted")
        }
        
        // Handle SCHEDULE_EXACT_ALARM separately if needed (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isExactAlarmPermissionGranted(activity)) {
                Logger.i(TAG, "SCHEDULE_EXACT_ALARM permission missing - user must grant via Settings")
                // Don't automatically open settings here - let the calling code decide when to show the dialog
            }
        }
    }
    
    /**
     * Request location permission
     */
    fun requestLocationPermission(activity: Activity) {
        if (!isLocationPermissionGranted(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_CODE_LOCATION
            )
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }
    }
    
    /**
     * Open settings to grant exact alarm permission (Android 12+)
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Logger.e(TAG, "Error opening exact alarm settings", e)
                // Fallback to app settings
                openAppSettings(context)
            }
        }
    }
    
    /**
     * Open settings to grant overlay permission
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                Logger.e(TAG, "Error opening overlay settings", e)
                openAppSettings(context)
            }
        }
    }
    
    /**
     * Open app settings
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e(TAG, "Error opening app settings", e)
        }
    }
    
    /**
     * Check if all critical permissions are granted
     */
    fun areAllCriticalPermissionsGranted(context: Context): Boolean {
        return isLocationPermissionGranted(context) &&
               isNotificationPermissionGranted(context) &&
               isExactAlarmPermissionGranted(context) &&
               isOverlayPermissionGranted(context)
    }
    
    /**
     * Check if absolutely critical permissions are granted (required for app to function)
     * Without these, the app cannot work properly and should exit
     */
    fun areCriticalPermissionsGranted(context: Context): Boolean {
        // Exact alarm permission is absolutely critical - without it, alarms won't fire
        return isExactAlarmPermissionGranted(context)
    }
    
    /**
     * Get list of missing critical permissions (required for app to function)
     */
    fun getMissingCriticalPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        
        if (!isExactAlarmPermissionGranted(context)) {
            missing.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        
        return missing
    }
    
    /**
     * Get human-readable permission name
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.SCHEDULE_EXACT_ALARM -> "Exact Alarms"
            Manifest.permission.USE_FULL_SCREEN_INTENT -> "Full Screen Intent"
            Manifest.permission.WAKE_LOCK -> "Wake Lock"
            Manifest.permission.FOREGROUND_SERVICE -> "Foreground Service"
            else -> permission.substringAfterLast('.')
        }
    }
}
