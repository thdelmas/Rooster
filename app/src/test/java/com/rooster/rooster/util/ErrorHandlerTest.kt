package com.rooster.rooster.util

import kotlinx.coroutines.CoroutineExceptionHandler
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ErrorHandler
 */
class ErrorHandlerTest {

    // ==================== safely ====================

    @Test
    fun `safely returns value on success`() {
        val result = ErrorHandler.safely { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `safely returns null on exception`() {
        val result = ErrorHandler.safely { throw RuntimeException("fail") }
        assertNull(result)
    }

    @Test
    fun `safely invokes onError callback on exception`() {
        var captured: Throwable? = null
        ErrorHandler.safely(onError = { captured = it }) {
            throw IllegalArgumentException("test error")
        }
        assertNotNull(captured)
        assertTrue(captured is IllegalArgumentException)
        assertEquals("test error", captured?.message)
    }

    @Test
    fun `safely does not invoke onError on success`() {
        var called = false
        ErrorHandler.safely(onError = { called = true }) { "ok" }
        assertFalse(called)
    }

    // ==================== safelySuspend ====================

    @Test
    fun `safelySuspend returns value on success`() {
        val result = kotlinx.coroutines.runBlocking {
            ErrorHandler.safelySuspend { 99 }
        }
        assertEquals(99, result)
    }

    @Test
    fun `safelySuspend returns null on exception`() {
        val result = kotlinx.coroutines.runBlocking {
            ErrorHandler.safelySuspend { throw RuntimeException("fail") }
        }
        assertNull(result)
    }

    @Test
    fun `safelySuspend invokes onError callback`() {
        var captured: Throwable? = null
        kotlinx.coroutines.runBlocking {
            ErrorHandler.safelySuspend(onError = { captured = it }) {
                throw IOException("io fail")
            }
        }
        assertNotNull(captured)
        assertTrue(captured is IOException)
    }

    // ==================== createCoroutineExceptionHandler ====================

    @Test
    fun `createCoroutineExceptionHandler returns CoroutineExceptionHandler`() {
        val handler = ErrorHandler.createCoroutineExceptionHandler(tag = "Test")
        assertTrue(handler is CoroutineExceptionHandler)
    }

    @Test
    fun `createCoroutineExceptionHandler invokes onError`() {
        var captured: Throwable? = null
        val handler = ErrorHandler.createCoroutineExceptionHandler(
            tag = "Test",
            onError = { captured = it }
        )

        // Simulate invoking the handler
        handler.handleException(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).coroutineContext,
            RuntimeException("coroutine fail")
        )

        assertNotNull(captured)
        assertEquals("coroutine fail", captured?.message)
    }

    // ==================== onFailureLog extension ====================

    @Test
    fun `onFailureLog does not throw on success`() {
        val result = Result.success("ok")
        result.onFailureLog("Tag", "message") // Should not throw
    }

    @Test
    fun `onFailureLog does not throw on failure`() {
        val result = Result.failure<String>(RuntimeException("fail"))
        result.onFailureLog("Tag", "message") // Should not throw
    }
}

private typealias IOException = java.io.IOException
