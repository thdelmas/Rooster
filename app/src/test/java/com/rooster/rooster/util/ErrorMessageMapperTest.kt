package com.rooster.rooster.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorMessageMapper
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ErrorMessageMapperTest {

    // ==================== Network Errors ====================

    @Test
    fun `mapError UnknownHostException returns internet message`() {
        val result = ErrorMessageMapper.mapError(UnknownHostException("test"))
        assertEquals("Unable to connect to the internet", result.message)
        assertNotNull(result.recoverySuggestion)
        assertNotNull(result.actionHint)
    }

    @Test
    fun `mapError SocketTimeoutException returns timeout message`() {
        val result = ErrorMessageMapper.mapError(SocketTimeoutException("test"))
        assertEquals("Connection timed out", result.message)
    }

    @Test
    fun `mapError ConnectException returns server message`() {
        val result = ErrorMessageMapper.mapError(ConnectException("test"))
        assertEquals("Cannot reach the server", result.message)
    }

    // ==================== Permission Errors ====================

    @Test
    fun `mapError SecurityException location hint`() {
        val result = ErrorMessageMapper.mapError(SecurityException("Need location permission"))
        assertEquals("Permission required", result.message)
        assertTrue(result.actionHint!!.contains("Location"))
    }

    @Test
    fun `mapError SecurityException storage hint`() {
        val result = ErrorMessageMapper.mapError(SecurityException("Storage access denied"))
        assertTrue(result.actionHint!!.contains("Storage"))
    }

    @Test
    fun `mapError SecurityException notification hint`() {
        val result = ErrorMessageMapper.mapError(SecurityException("notification permission"))
        assertTrue(result.actionHint!!.contains("Notification"))
    }

    @Test
    fun `mapError SecurityException generic hint`() {
        val result = ErrorMessageMapper.mapError(SecurityException("some error"))
        assertEquals("Go to Settings > App Permissions", result.actionHint)
    }

    // ==================== File Errors ====================

    @Test
    fun `mapError FileNotFoundException default context`() {
        val result = ErrorMessageMapper.mapError(FileNotFoundException("missing"))
        assertEquals("File not found", result.message)
        assertTrue(result.recoverySuggestion!!.contains("file"))
    }

    @Test
    fun `mapError FileNotFoundException backup context`() {
        val result = ErrorMessageMapper.mapError(FileNotFoundException("missing"), "backup")
        assertTrue(result.recoverySuggestion!!.contains("backup file"))
    }

    @Test
    fun `mapError IOException with export context`() {
        val result = ErrorMessageMapper.mapError(IOException("fail"), "export")
        assertEquals("Unable to access file", result.message)
        assertTrue(result.recoverySuggestion!!.contains("exporting"))
    }

    @Test
    fun `mapError IOException with import context`() {
        val result = ErrorMessageMapper.mapError(IOException("fail"), "import")
        assertTrue(result.recoverySuggestion!!.contains("importing"))
    }

    // ==================== Validation Errors ====================

    @Test
    fun `mapError IllegalArgumentException label message`() {
        val result = ErrorMessageMapper.mapError(IllegalArgumentException("Invalid label"))
        assertEquals("Please enter a name for your alarm", result.message)
    }

    @Test
    fun `mapError IllegalArgumentException time message`() {
        val result = ErrorMessageMapper.mapError(IllegalArgumentException("Invalid time"))
        assertEquals("Please set a valid time for your alarm", result.message)
    }

    @Test
    fun `mapError IllegalArgumentException mode message`() {
        val result = ErrorMessageMapper.mapError(IllegalArgumentException("Invalid mode"))
        assertEquals("Please select a valid alarm mode", result.message)
    }

    @Test
    fun `mapError IllegalArgumentException day message`() {
        val result = ErrorMessageMapper.mapError(IllegalArgumentException("No day selected"))
        assertEquals("Please select at least one day for your alarm", result.message)
    }

    @Test
    fun `mapError IllegalArgumentException generic`() {
        val result = ErrorMessageMapper.mapError(IllegalArgumentException("something else"))
        assertEquals("Invalid alarm settings", result.message)
    }

    // ==================== State Errors ====================

    @Test
    fun `mapError IllegalStateException past time`() {
        val result = ErrorMessageMapper.mapError(IllegalStateException("Time is in the past"))
        assertEquals("The alarm time is in the past", result.message)
        assertTrue(result.recoverySuggestion!!.contains("future"))
    }

    @Test
    fun `mapError IllegalStateException disabled alarm`() {
        val result = ErrorMessageMapper.mapError(IllegalStateException("Alarm is disabled"))
        assertEquals("This alarm is currently disabled", result.message)
    }

    @Test
    fun `mapError IllegalStateException AlarmManager`() {
        val result = ErrorMessageMapper.mapError(IllegalStateException("AlarmManager failed"))
        assertEquals("Unable to schedule the alarm", result.message)
    }

    // ==================== JSON Error ====================

    @Test
    fun `mapError JSONException backup context`() {
        val result = ErrorMessageMapper.mapError(org.json.JSONException("bad json"), "backup")
        assertEquals("Invalid file format", result.message)
        assertTrue(result.recoverySuggestion!!.contains("corrupted"))
    }

    // ==================== Other Errors ====================

    @Test
    fun `mapError ArithmeticException`() {
        val result = ErrorMessageMapper.mapError(ArithmeticException("div by zero"))
        assertEquals("Calculation error", result.message)
    }

    @Test
    fun `mapError NullPointerException`() {
        val result = ErrorMessageMapper.mapError(NullPointerException())
        assertEquals("Unexpected error", result.message)
    }

    // ==================== Generic with context ====================

    @Test
    fun `mapError generic with backup context`() {
        val result = ErrorMessageMapper.mapError(RuntimeException("fail"), "backup")
        assertEquals("Unable to complete backup", result.message)
    }

    @Test
    fun `mapError generic with save context`() {
        val result = ErrorMessageMapper.mapError(RuntimeException("fail"), "save")
        assertEquals("Unable to save alarm", result.message)
    }

    @Test
    fun `mapError generic with schedule context`() {
        val result = ErrorMessageMapper.mapError(RuntimeException("fail"), "schedule")
        assertEquals("Unable to schedule alarm", result.message)
    }

    @Test
    fun `mapError generic with location context`() {
        val result = ErrorMessageMapper.mapError(RuntimeException("fail"), "location")
        assertEquals("Unable to get location", result.message)
    }

    @Test
    fun `mapError generic with no context`() {
        val result = ErrorMessageMapper.mapError(RuntimeException("fail"))
        assertEquals("Something went wrong", result.message)
    }

    // ==================== UserFriendlyError ====================

    @Test
    fun `getFullMessage includes all parts`() {
        val error = ErrorMessageMapper.UserFriendlyError(
            message = "Error",
            recoverySuggestion = "Try again",
            actionHint = "Check settings"
        )
        val full = error.getFullMessage()
        assertTrue(full.contains("Error"))
        assertTrue(full.contains("Try again"))
        assertTrue(full.contains("Check settings"))
    }

    @Test
    fun `getFullMessage with no optional fields`() {
        val error = ErrorMessageMapper.UserFriendlyError(message = "Error")
        assertEquals("Error", error.getFullMessage())
    }

    // ==================== getContextualError ====================

    @Test
    fun `getContextualError delegates to mapError`() {
        val result = ErrorMessageMapper.getContextualError("BACKUP", UnknownHostException("test"))
        assertEquals("Unable to connect to the internet", result.message)
    }
}
