package com.rooster.rooster

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.rooster.rooster.presentation.compose.AlarmListScreen
import com.rooster.rooster.presentation.compose.ringScheduleMessage
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmListActivity : ComponentActivity() {

    private val viewModel: AlarmListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoosterTheme {
                val alarms by viewModel.allAlarms.observeAsState(emptyList())
                Logger.d("AlarmListActivity", "Alarms updated: ${alarms.size} alarms")

                AlarmListScreen(
                    viewModel = viewModel,
                    onCreateAlarm = ::createNewAlarm,
                    onEditAlarm = ::openAlarmEditor,
                    onAlarmEnabledToggled = ::onAlarmEnabledToggled,
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun createNewAlarm() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        startActivity(Intent(this, TimePickerActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun openAlarmEditor(alarmId: Long) {
        HapticFeedbackHelper.performSuccessFeedback(this)
        val intent = Intent(this, AlarmEditorActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        startActivity(intent)
    }

    private fun onAlarmEnabledToggled(alarm: Alarm, isChecked: Boolean) {
        HapticFeedbackHelper.performSuccessFeedback(this)
        alarm.enabled = isChecked
        viewModel.updateAlarm(alarm)
        toast(if (isChecked) ringScheduleMessage(alarm) else "Alarm disabled")
    }
}
