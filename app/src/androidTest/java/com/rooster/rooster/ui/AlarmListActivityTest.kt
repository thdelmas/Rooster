package com.rooster.rooster.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
 * UI tests for AlarmListActivity
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmListActivityTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testActivityLaunches() {
        ActivityScenario.launch(AlarmListActivity::class.java)
        
        // Verify that the activity is displayed
        onView(withId(R.id.alarmsList))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testAddAlarmButtonIsVisible() {
        ActivityScenario.launch(AlarmListActivity::class.java)
        
        // Verify add alarm button is visible
        onView(withId(R.id.addAlarmButton))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }
    
    @Test
    fun testClickAddAlarmButton() {
        ActivityScenario.launch(AlarmListActivity::class.java)
        
        // Click the add alarm button
        onView(withId(R.id.addAlarmButton))
            .perform(click())
        
        // Verify that AlarmActivity is launched (or dialog appears)
        // This would require proper navigation testing
    }
}
