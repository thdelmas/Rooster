package com.rooster.rooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.presentation.compose.PickerMode
import com.rooster.rooster.presentation.compose.TimePickerScreen
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.presentation.viewmodel.MainViewModel
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.HapticFeedbackHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class TimePickerActivity : androidx.fragment.app.FragmentActivity() {

    private val alarmListViewModel: AlarmListViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private val mode = MutableStateFlow(PickerMode.TRADITIONAL)
    private val selectedTime = MutableStateFlow(initialTimeMillis())
    private val astronomyData = MutableStateFlow<AstronomyDataEntity?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.getAstronomyDataFlow().collect { data ->
                    astronomyData.value = data
                }
            }
        }

        setContent {
            RoosterTheme {
                val currentMode by mode.collectAsState()
                val time by selectedTime.collectAsState()
                val astronomy by astronomyData.collectAsState()

                TimePickerScreen(
                    mode = currentMode,
                    selectedTime = time,
                    astronomyData = astronomy,
                    onModeChange = { newMode ->
                        HapticFeedbackHelper.performClick(window.decorView)
                        mode.value = newMode
                    },
                    onPickTraditionalTime = ::showMaterialTimePicker,
                    onSolarTimeSelected = { newTime ->
                        HapticFeedbackHelper.performLightClick(window.decorView)
                        selectedTime.value = newTime
                    },
                    onContinue = ::createAlarmAndContinue,
                    onClose = ::handleBack,
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun handleBack() {
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun showMaterialTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTime.value }
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
            selectedTime.value = cal.timeInMillis
            HapticFeedbackHelper.performSuccessFeedback(this)
        }
        picker.show(supportFragmentManager, "MaterialTimePicker")
    }

    @Suppress("DEPRECATION")
    private fun createAlarmAndContinue() {
        HapticFeedbackHelper.performSuccessFeedback(this)
        val current = selectedTime.value
        val alarm = if (mode.value == PickerMode.TRADITIONAL) {
            AlarmCreation(
                label = "Alarm",
                enabled = false,
                mode = AppConstants.ALARM_MODE_AT,
                ringtoneUri = AppConstants.DEFAULT_RINGTONE_URI,
                relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
                relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
                time1 = current,
                time2 = 0,
                calculatedTime = current,
            )
        } else {
            val (event, offsetMinutes, alarmMode) = findClosestSolarEventWithOffset(current)
            AlarmCreation(
                label = "Alarm",
                enabled = false,
                mode = alarmMode,
                ringtoneUri = AppConstants.DEFAULT_RINGTONE_URI,
                relative1 = event,
                relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
                time1 = if (alarmMode == AppConstants.ALARM_MODE_BEFORE || alarmMode == AppConstants.ALARM_MODE_AFTER) {
                    offsetMinutes * AppConstants.MILLIS_PER_MINUTE.toLong()
                } else {
                    0L
                },
                time2 = 0,
                calculatedTime = current,
            )
        }

        alarmListViewModel.insertAlarm(alarm) {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun findClosestSolarEventWithOffset(time: Long): Triple<String, Int, String> {
        val data = astronomyData.value
            ?: return Triple(AppConstants.SOLAR_EVENT_SUNRISE, 0, AppConstants.ALARM_MODE_AT)

        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun normalizeTime(originalTime: Long): Long {
            if (originalTime <= 0) return 0
            calendar.timeInMillis = originalTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            calendar.timeInMillis = todayStart
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        calendar.timeInMillis = time
        val timeHour = calendar.get(Calendar.HOUR_OF_DAY)
        val timeMinute = calendar.get(Calendar.MINUTE)
        calendar.timeInMillis = todayStart
        calendar.set(Calendar.HOUR_OF_DAY, timeHour)
        calendar.set(Calendar.MINUTE, timeMinute)
        val normalizedTime = calendar.timeInMillis

        val events = listOf(
            normalizeTime(data.astroDawn) to AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN,
            normalizeTime(data.nauticalDawn) to AppConstants.SOLAR_EVENT_NAUTICAL_DAWN,
            normalizeTime(data.civilDawn) to AppConstants.SOLAR_EVENT_CIVIL_DAWN,
            normalizeTime(data.sunrise) to AppConstants.SOLAR_EVENT_SUNRISE,
            normalizeTime(data.solarNoon) to AppConstants.SOLAR_EVENT_SOLAR_NOON,
            normalizeTime(data.sunset) to AppConstants.SOLAR_EVENT_SUNSET,
            normalizeTime(data.civilDusk) to AppConstants.SOLAR_EVENT_CIVIL_DUSK,
            normalizeTime(data.nauticalDusk) to AppConstants.SOLAR_EVENT_NAUTICAL_DUSK,
            normalizeTime(data.astroDusk) to AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK,
        ).filter { it.first > 0 }

        if (events.isEmpty()) {
            return Triple(AppConstants.SOLAR_EVENT_SUNRISE, 0, AppConstants.ALARM_MODE_AT)
        }

        val dayInMillis = 24 * 60 * 60 * 1000L
        val closestEvent = events.minBy { eventTime ->
            var diff = kotlin.math.abs(normalizedTime - eventTime.first)
            if (diff > dayInMillis / 2) {
                diff = dayInMillis - diff
            }
            diff
        }

        var timeDiff = normalizedTime - closestEvent.first
        if (timeDiff > dayInMillis / 2) {
            timeDiff -= dayInMillis
        } else if (timeDiff < -dayInMillis / 2) {
            timeDiff += dayInMillis
        }

        val offsetMinutes = (timeDiff / AppConstants.MILLIS_PER_MINUTE).toInt()

        val alarmMode = when {
            kotlin.math.abs(offsetMinutes) <= 5 -> AppConstants.ALARM_MODE_AT
            offsetMinutes > 0 -> AppConstants.ALARM_MODE_AFTER
            else -> AppConstants.ALARM_MODE_BEFORE
        }

        val finalOffset = if (alarmMode == AppConstants.ALARM_MODE_AT) 0 else kotlin.math.abs(offsetMinutes)
        return Triple(closestEvent.second, finalOffset, alarmMode)
    }

    private fun initialTimeMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
