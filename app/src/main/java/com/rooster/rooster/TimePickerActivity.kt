package com.rooster.rooster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.presentation.viewmodel.MainViewModel
import com.rooster.rooster.ui.SolarRingTimePickerView
import com.rooster.rooster.ui.WheelTimePicker
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.AnimationHelper
import com.rooster.rooster.util.HapticFeedbackHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class TimePickerActivity : AppCompatActivity() {

    private val alarmListViewModel: AlarmListViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var traditionalModeButton: MaterialButton
    private lateinit var solarModeButton: MaterialButton
    private lateinit var traditionalTimePickerCard: MaterialCardView
    private lateinit var solarTimePickerCard: MaterialCardView
    private lateinit var hourPicker: WheelTimePicker
    private lateinit var minutePicker: WheelTimePicker
    private lateinit var periodPicker: WheelTimePicker
    private lateinit var solarRingTimePicker: SolarRingTimePickerView
    private lateinit var continueButton: ExtendedFloatingActionButton

    private var currentMode: String = "traditional" // "traditional" or "solar"
    private var selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE)
    private var selectedTime: Long = 0
    private var astronomyData: com.rooster.rooster.data.local.entity.AstronomyDataEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_picker)

        setupToolbar()
        setupViews()
        setupListeners()
        initializeTime()
        observeAstronomyData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            HapticFeedbackHelper.performClick(toolbar)
            finish()
        }
    }

    private fun setupViews() {
        traditionalModeButton = findViewById(R.id.traditionalModeButton)
        solarModeButton = findViewById(R.id.solarModeButton)
        traditionalTimePickerCard = findViewById(R.id.traditionalTimePickerCard)
        solarTimePickerCard = findViewById(R.id.solarTimePickerCard)
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        periodPicker = findViewById(R.id.periodPicker)
        solarRingTimePicker = findViewById(R.id.solarRingTimePicker)
        continueButton = findViewById(R.id.continueButton)
    }

    private fun setupListeners() {
        // Mode selection buttons
        traditionalModeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            setMode("traditional")
        }

        solarModeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            setMode("solar")
        }

        // Traditional time picker listeners
        hourPicker.onValueChangedListener = { index ->
            val (hour12, isPM) = convertTo12Hour(selectedHour)
            selectedHour = convertTo24Hour(index + 1, periodPicker.getSelectedIndex() == 1)
            updateSelectedTime()
            HapticFeedbackHelper.performLightClick(hourPicker)
        }

        minutePicker.onValueChangedListener = { index ->
            selectedMinute = index
            updateSelectedTime()
            HapticFeedbackHelper.performLightClick(minutePicker)
        }

        periodPicker.onValueChangedListener = { index ->
            val currentHour12 = hourPicker.getSelectedIndex() + 1
            selectedHour = convertTo24Hour(currentHour12, index == 1)
            updateSelectedTime()
            HapticFeedbackHelper.performLightClick(periodPicker)
        }

        // Solar time picker listener
        solarRingTimePicker.setOnTimeSelectedListener { time ->
            selectedTime = time
            HapticFeedbackHelper.performLightClick(solarRingTimePicker)
        }

        // Continue button
        continueButton.setOnClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            HapticFeedbackHelper.performSuccessFeedback(this)
            AnimationHelper.scaleWithBounce(it)
            createAlarmAndContinue()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun initializeTime() {
        val calendar = Calendar.getInstance()
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)

        // Initialize traditional time picker
        val (hour12, isPM) = convertTo12Hour(selectedHour)
        hourPicker.setItems((1..12).map { String.format("%02d", it) })
        minutePicker.setItems((0..59).map { String.format("%02d", it) })
        periodPicker.setItems(listOf("AM", "PM"))

        hourPicker.setSelectedIndex(hour12 - 1, false)
        minutePicker.setSelectedIndex(selectedMinute, false)
        periodPicker.setSelectedIndex(if (isPM) 1 else 0, false)

        updateSelectedTime()

        // Set initial mode to traditional
        setMode("traditional")
    }

    private fun setMode(mode: String) {
        currentMode = mode

        if (mode == "traditional") {
            traditionalModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_primaryContainer))
                setTextColor(getColor(R.color.md_theme_dark_onPrimaryContainer))
            }
            solarModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_surfaceVariant))
                setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
            }

            if (solarTimePickerCard.visibility == View.VISIBLE) {
                AnimationHelper.fadeOut(solarTimePickerCard) {
                    solarTimePickerCard.visibility = View.GONE
                }
            }
            if (traditionalTimePickerCard.visibility != View.VISIBLE) {
                traditionalTimePickerCard.visibility = View.VISIBLE
                AnimationHelper.fadeIn(traditionalTimePickerCard)
            }
        } else {
            solarModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_primaryContainer))
                setTextColor(getColor(R.color.md_theme_dark_onPrimaryContainer))
            }
            traditionalModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_surfaceVariant))
                setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
            }

            if (traditionalTimePickerCard.visibility == View.VISIBLE) {
                AnimationHelper.fadeOut(traditionalTimePickerCard) {
                    traditionalTimePickerCard.visibility = View.GONE
                }
            }
            if (solarTimePickerCard.visibility != View.VISIBLE) {
                solarTimePickerCard.visibility = View.VISIBLE
                AnimationHelper.fadeIn(solarTimePickerCard)
            }
        }
    }

    private fun updateSelectedTime() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        calendar.set(Calendar.MINUTE, selectedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        selectedTime = calendar.timeInMillis
    }

    private fun observeAstronomyData() {
        lifecycleScope.launch {
            mainViewModel.getAstronomyDataFlow().collect { data ->
                astronomyData = data
                data?.let {
                    solarRingTimePicker.setAstronomyData(it)
                    // Set initial selected time for solar picker if not set
                    if (selectedTime == 0L) {
                        selectedTime = System.currentTimeMillis()
                        solarRingTimePicker.setSelectedTime(selectedTime)
                    }
                }
            }
        }
    }

    private fun convertTo12Hour(hour24: Int): Pair<Int, Boolean> {
        val isPM = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return Pair(hour12, isPM)
    }

    private fun convertTo24Hour(hour12: Int, isPM: Boolean): Int {
        return when {
            hour12 == 12 && !isPM -> 0
            hour12 == 12 && isPM -> 12
            isPM -> hour12 + 12
            else -> hour12
        }
    }

    private fun createAlarmAndContinue() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedTime

        // Create alarm based on selected mode
        val alarm = if (currentMode == "traditional") {
            // Traditional time picker - use classic mode
            AlarmCreation(
                label = "Alarm",
                enabled = false,
                mode = AppConstants.ALARM_MODE_AT,
                ringtoneUri = AppConstants.DEFAULT_RINGTONE_URI,
                relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
                relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
                time1 = selectedTime,
                time2 = 0,
                calculatedTime = selectedTime
            )
        } else {
            // Solar time picker - find closest solar event and calculate offset if needed
            val (solarEvent, offsetMinutes, mode) = findClosestSolarEventWithOffset(selectedTime)
            AlarmCreation(
                label = "Alarm",
                enabled = false,
                mode = mode,
                ringtoneUri = AppConstants.DEFAULT_RINGTONE_URI,
                relative1 = solarEvent,
                relative2 = AppConstants.RELATIVE_TIME_PICK_TIME,
                time1 = if (mode == AppConstants.ALARM_MODE_BEFORE || mode == AppConstants.ALARM_MODE_AFTER) {
                    offsetMinutes * AppConstants.MILLIS_PER_MINUTE
                } else {
                    0
                },
                time2 = 0,
                calculatedTime = selectedTime
            )
        }

        // Insert alarm and open editor
        alarmListViewModel.insertAlarm(alarm) { alarmId ->
            val intent = Intent(this, AlarmEditorActivity::class.java)
            intent.putExtra("alarm_id", alarmId)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun findClosestSolarEventWithOffset(time: Long): Triple<String, Int, String> {
        val data = astronomyData ?: return Triple(AppConstants.SOLAR_EVENT_SUNRISE, 0, AppConstants.ALARM_MODE_AT)
        
        // Normalize time to today for comparison
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
        
        // Create list of solar events with their times
        val events = listOf(
            Pair(normalizeTime(data.astroDawn), AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN),
            Pair(normalizeTime(data.nauticalDawn), AppConstants.SOLAR_EVENT_NAUTICAL_DAWN),
            Pair(normalizeTime(data.civilDawn), AppConstants.SOLAR_EVENT_CIVIL_DAWN),
            Pair(normalizeTime(data.sunrise), AppConstants.SOLAR_EVENT_SUNRISE),
            Pair(normalizeTime(data.solarNoon), AppConstants.SOLAR_EVENT_SOLAR_NOON),
            Pair(normalizeTime(data.sunset), AppConstants.SOLAR_EVENT_SUNSET),
            Pair(normalizeTime(data.civilDusk), AppConstants.SOLAR_EVENT_CIVIL_DUSK),
            Pair(normalizeTime(data.nauticalDusk), AppConstants.SOLAR_EVENT_NAUTICAL_DUSK),
            Pair(normalizeTime(data.astroDusk), AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK)
        ).filter { it.first > 0 }
        
        if (events.isEmpty()) {
            return Triple(AppConstants.SOLAR_EVENT_SUNRISE, 0, AppConstants.ALARM_MODE_AT)
        }
        
        // Find the closest event and calculate offset
        val dayInMillis = 24 * 60 * 60 * 1000L
        val closestEvent = events.minByOrNull { eventTime ->
            var diff = kotlin.math.abs(normalizedTime - eventTime.first)
            // Handle wrap-around (e.g., if time is 23:00 and event is 01:00)
            if (diff > dayInMillis / 2) {
                diff = dayInMillis - diff
            }
            diff
        }
        
        if (closestEvent == null) {
            return Triple(AppConstants.SOLAR_EVENT_SUNRISE, 0, AppConstants.ALARM_MODE_AT)
        }
        
        // Calculate offset in minutes
        var timeDiff = normalizedTime - closestEvent.first
        // Handle wrap-around
        if (timeDiff > dayInMillis / 2) {
            timeDiff -= dayInMillis
        } else if (timeDiff < -dayInMillis / 2) {
            timeDiff += dayInMillis
        }
        
        val offsetMinutes = (timeDiff / AppConstants.MILLIS_PER_MINUTE).toInt()
        
        // If offset is small (within 5 minutes), use "At" mode
        // Otherwise use "Before" or "After" mode
        val mode = when {
            kotlin.math.abs(offsetMinutes) <= 5 -> AppConstants.ALARM_MODE_AT
            offsetMinutes > 0 -> AppConstants.ALARM_MODE_AFTER
            else -> AppConstants.ALARM_MODE_BEFORE
        }
        
        val finalOffset = if (mode == AppConstants.ALARM_MODE_AT) 0 else kotlin.math.abs(offsetMinutes)
        
        return Triple(closestEvent.second, finalOffset, mode)
    }
}

