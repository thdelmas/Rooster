package com.rooster.rooster.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rooster.rooster.AlarmListActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmListActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<AlarmListActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun topBarIsDisplayed() {
        composeRule.onNodeWithText("Alarms").assertIsDisplayed()
    }

    @Test
    fun addAlarmFabIsDisplayed() {
        composeRule.onNodeWithText("New Alarm").assertIsDisplayed()
    }

    @Test
    fun clickAddAlarmFab() {
        composeRule.onNodeWithText("New Alarm").performClick()
    }
}
