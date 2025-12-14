package com.rooster.rooster.util

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import java.util.Locale

/**
 * Extension functions for common operations
 */

// Context extensions
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// SharedPreferences extensions
fun SharedPreferences.putFloat(key: String, value: Float) {
    edit().putFloat(key, value).apply()
}

fun SharedPreferences.putLong(key: String, value: Long) {
    edit().putLong(key, value).apply()
}

fun SharedPreferences.putString(key: String, value: String) {
    edit().putString(key, value).apply()
}

fun SharedPreferences.putBoolean(key: String, value: Boolean) {
    edit().putBoolean(key, value).apply()
}

// Time formatting extensions
fun Long.toFormattedTime(pattern: String = "HH:mm"): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(calendar.time)
}

fun Long.toFormattedDateTime(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(calendar.time)
}

fun Long.toFormattedDate(pattern: String = "yyyy-MM-dd"): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(calendar.time)
}

// Time calculation extensions
fun Long.minutesUntil(): Long {
    val now = System.currentTimeMillis()
    return (this - now) / 1000 / 60
}

fun Long.hoursUntil(): Long {
    val now = System.currentTimeMillis()
    return (this - now) / 1000 / 60 / 60
}

fun Long.isInPast(): Boolean {
    return this < System.currentTimeMillis()
}

fun Long.isInFuture(): Boolean {
    return this > System.currentTimeMillis()
}

// Flow extensions
fun <T> Flow<T>.logErrors(tag: String): Flow<T> {
    return this.catch { e ->
        ErrorHandler.logError(tag, "Flow error", e)
        throw e
    }
}

fun <T> Flow<T>.onEachLog(tag: String, message: (T) -> String): Flow<T> {
    return this.onEach { value ->
        Logger.d(tag, message(value))
    }
}

// LiveData extensions
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}

// String extensions
fun String.toCapitalized(): String {
    return this.lowercase().replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// Calendar extensions
fun Calendar.toTimeInMillis(): Long = this.timeInMillis

fun Calendar.formatTime(pattern: String = "HH:mm"): String {
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(this.time)
}

fun Calendar.copyFrom(other: Calendar) {
    this.timeInMillis = other.timeInMillis
}

// Time of day extensions
fun Long.getHourOfDay(): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return calendar.get(Calendar.HOUR_OF_DAY)
}

fun Long.getMinute(): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return calendar.get(Calendar.MINUTE)
}

fun Long.getDayOfWeek(): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return calendar.get(Calendar.DAY_OF_WEEK)
}

// Duration formatting
fun Long.toReadableDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""}"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        else -> "$seconds second${if (seconds > 1) "s" else ""}"
    }
}

// Alarm-specific extensions
fun String.isRelativeTime(): Boolean {
    return ValidationHelper.isValidRelativeTime(this)
}

// Collection extensions
fun <T> List<T>.second(): T? {
    return if (this.size >= 2) this[1] else null
}

fun <T> List<T>.penultimate(): T? {
    return if (this.size >= 2) this[this.size - 2] else null
}

// Boolean extensions for day selection
fun Boolean.toEnabledString(): String {
    return if (this) "Enabled" else "Disabled"
}

// Number formatting
fun Float.toCoordinateString(): String {
    return String.format(Locale.US, "%.6f", this)
}

fun Double.toCoordinateString(): String {
    return String.format(Locale.US, "%.6f", this)
}
