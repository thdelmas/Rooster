package com.rooster.rooster

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.rooster.rooster.presentation.compose.AlarmEditorScreen
import com.rooster.rooster.presentation.viewmodel.AlarmEditorViewModel
import com.rooster.rooster.ui.SoundPreviewHelper
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AlarmEditorActivity : AppCompatActivity() {

    private val viewModel: AlarmEditorViewModel by viewModels()
    private lateinit var soundPreviewHelper: SoundPreviewHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPreviewHelper = SoundPreviewHelper(this)

        val alarmId = intent.getLongExtra("alarm_id", -1L)
        viewModel.initialize(alarmId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.toastEvents.collect { toast(it) } }
                launch { viewModel.finishEvents.collect { finish() } }
            }
        }

        setContent {
            RoosterTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val state by viewModel.uiState.collectAsState()

                if (state.errorMessage != null) {
                    androidx.compose.runtime.LaunchedEffect(state.errorMessage) {
                        snackbarHostState.showSnackbar(state.errorMessage!!)
                        viewModel.clearError()
                    }
                }

                AlarmEditorScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onLabelChange = viewModel::setLabel,
                    onModeChange = viewModel::setMode,
                    onSunTimingChange = viewModel::setSunTimingMode,
                    onSolarEvent1Change = viewModel::setSolarEvent1,
                    onSolarEvent2Change = viewModel::setSolarEvent2,
                    onOffsetChange = viewModel::setOffsetMinutes,
                    onSolarRingTimeSelected = viewModel::setOffsetFromSolarRingTime,
                    onToggleDay = viewModel::toggleDay,
                    onApplyDayPreset = viewModel::applyDayPreset,
                    onVibrateChange = viewModel::setVibrate,
                    onSnoozeEnabledChange = viewModel::setSnoozeEnabled,
                    onAdjustSnoozeDuration = viewModel::adjustSnoozeDuration,
                    onAdjustSnoozeCount = viewModel::adjustSnoozeCount,
                    onGradualVolumeChange = viewModel::setGradualVolume,
                    onPickClassicTime = { showTimePicker(state.selectedTime) },
                    onPreviewRingtone = { previewRingtone(state.ringtoneUri) },
                    onPickRingtone = { openRingtonePicker(state.alarmId) },
                    onDelete = viewModel::deleteAlarm,
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPreviewHelper.cleanup()
    }

    private fun showTimePicker(currentMillis: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (currentMillis > 0L) currentMillis else System.currentTimeMillis()
        }
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText("Select Time")
            .build()
        picker.addOnPositiveButtonClickListener {
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            viewModel.setSelectedTime(cal.timeInMillis)
        }
        picker.show(supportFragmentManager, "MaterialTimePicker")
    }

    private fun previewRingtone(ringtoneUri: String) {
        if (soundPreviewHelper.isPreviewPlaying()) {
            soundPreviewHelper.stopPreview()
        } else {
            soundPreviewHelper.previewSound(ringtoneUri)
        }
    }

    private fun openRingtonePicker(alarmId: Long) {
        val intent = Intent(this, RingtoneActivity::class.java)
        if (alarmId != -1L) {
            intent.putExtra("alarm_id", alarmId)
        }
        startActivity(intent)
    }
}
