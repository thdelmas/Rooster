package com.rooster.rooster.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for SleepProfileHelper
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SleepProfileHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `default profile is NONE`() {
        assertEquals(AppConstants.SLEEP_PROFILE_NONE, SleepProfileHelper.getSleepProfile(context))
    }

    @Test
    fun `set and get Early Bird profile`() {
        SleepProfileHelper.setSleepProfile(context, AppConstants.SLEEP_PROFILE_EARLY_BIRD)
        assertEquals(AppConstants.SLEEP_PROFILE_EARLY_BIRD, SleepProfileHelper.getSleepProfile(context))
    }

    @Test
    fun `set and get Night Owl profile`() {
        SleepProfileHelper.setSleepProfile(context, AppConstants.SLEEP_PROFILE_NIGHT_OWL)
        assertEquals(AppConstants.SLEEP_PROFILE_NIGHT_OWL, SleepProfileHelper.getSleepProfile(context))
    }

    @Test
    fun `getProfileName returns Early Bird`() {
        assertEquals("Early Bird", SleepProfileHelper.getProfileName(AppConstants.SLEEP_PROFILE_EARLY_BIRD))
    }

    @Test
    fun `getProfileName returns Night Owl`() {
        assertEquals("Night Owl", SleepProfileHelper.getProfileName(AppConstants.SLEEP_PROFILE_NIGHT_OWL))
    }

    @Test
    fun `getProfileName returns None for default`() {
        assertEquals("None", SleepProfileHelper.getProfileName(AppConstants.SLEEP_PROFILE_NONE))
    }

    @Test
    fun `getProfileName returns None for unknown value`() {
        assertEquals("None", SleepProfileHelper.getProfileName(999))
    }
}
