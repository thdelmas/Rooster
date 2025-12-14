package com.rooster.rooster.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rooster.rooster.AlarmListActivity
import com.rooster.rooster.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for alarm creation flow
 * Tests the critical user flow of creating a new alarm
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmCreationFlowTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testAlarmListActivityDisplays() {
        // Given - launching AlarmListActivity
        ActivityScenario.launch(AlarmListActivity::class.java)
        
        // Then - verify main elements are displayed
        onView(withId(R.id.alarmsList))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testAddAlarmButtonIsClickable() {
        // Given - AlarmListActivity is launched
        ActivityScenario.launch(AlarmListActivity::class.java)
        
        // Then - add alarm button should be visible and clickable
        onView(withId(R.id.addAlarmButton))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
    
    // Note: Full alarm creation flow tests would require:
    // 1. Clicking add alarm button
    // 2. Filling in alarm details in AlarmEditorActivity
    // 3. Saving the alarm
    // 4. Verifying it appears in the list
    // These tests require proper test data setup and mocking of dependencies
}
