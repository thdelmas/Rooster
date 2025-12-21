package com.rooster.rooster

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.presentation.viewmodel.AlarmEditorViewModel
import com.rooster.rooster.util.AnimationHelper
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.TimeUtils
import com.rooster.rooster.util.ValidationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AlarmEditorActivity : AppCompatActivity() {

    private val viewModel: AlarmListViewModel by viewModels()
    private val editorViewModel: AlarmEditorViewModel by viewModels()
    private val activityJob = SupervisorJob()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)
    
    private var alarmId: Long = -1
    private var currentAlarm: Alarm? = null
    private var currentMode: String = "sun" // "sun" or "classic"
    private var sunTimingMode: String = AppConstants.ALARM_MODE_AT // "At", "Before", "After", "Between"
    private var offsetMinutes: Int = 30
    private var selectedTime: Long = 0
    private var solarEvent1: String = "Sunrise"
    private var solarEvent2: String = "Sunset"
    
    // Alarm settings
    private var vibrateEnabled: Boolean = true
    private var snoozeEnabled: Boolean = true
    private var snoozeDuration: Int = 10
    private var snoozeCount: Int = 3
    private var alarmVolume: Int = 80
    private var gradualVolumeEnabled: Boolean = false
    
    // UI Elements
    private lateinit var alarmLabelInput: TextInputEditText
    private lateinit var sunModeButton: MaterialButton
    private lateinit var classicModeButton: MaterialButton
    private lateinit var sunModeCard: MaterialCardView
    private lateinit var classicModeCard: MaterialCardView
    private lateinit var calculatedTimeCard: MaterialCardView
    private lateinit var calculatedTimeText: TextView
    private lateinit var sunTimingToggle: MaterialButtonToggleGroup
    private lateinit var solarEvent1Button: MaterialButton
    private lateinit var solarEvent2Button: MaterialButton
    private lateinit var offsetTimeLayout: View
    private lateinit var solarEvent2Layout: View
    private lateinit var selectTimeButton: MaterialButton
    private lateinit var saveFab: ExtendedFloatingActionButton
    private lateinit var sunCourseView: SunCourseView
    private lateinit var vibrateSwitch: MaterialSwitch
    private lateinit var snoozeSwitch: MaterialSwitch
    private lateinit var gradualVolumeSwitch: MaterialSwitch
    
    // Sound preview
    private lateinit var soundPreviewHelper: com.rooster.rooster.ui.SoundPreviewHelper
    
    // Day buttons
    private val dayButtons = mutableMapOf<String, MaterialButton>()
    
    private val solarEvents = arrayOf(
        "🌄 Astronomical Dawn",
        "🌅 Nautical Dawn",
        "🌆 Civil Dawn",
        "🌅 Sunrise",
        "☀️ Solar Noon",
        "🌇 Sunset",
        "🌆 Civil Dusk",
        "🌃 Nautical Dusk",
        "🌌 Astronomical Dusk"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_editor)
        
        alarmId = intent.getLongExtra("alarm_id", -1)
        soundPreviewHelper = com.rooster.rooster.ui.SoundPreviewHelper(this)
        
        initializeViews()
        loadAlarmData()
        setupListeners()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
        soundPreviewHelper.cleanup()
    }
    
    private fun initializeViews() {
        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Input fields
        alarmLabelInput = findViewById(R.id.alarmLabelInput)
        
        // Mode buttons
        sunModeButton = findViewById(R.id.sunModeButton)
        classicModeButton = findViewById(R.id.classicModeButton)
        
        // Mode cards
        sunModeCard = findViewById(R.id.sunModeCard)
        classicModeCard = findViewById(R.id.classicModeCard)
        calculatedTimeCard = findViewById(R.id.calculatedTimeCard)
        calculatedTimeText = findViewById(R.id.calculatedTimeText)
        
        // Sun mode elements
        sunTimingToggle = findViewById(R.id.sunTimingToggle)
        solarEvent1Button = findViewById(R.id.solarEvent1Button)
        solarEvent2Button = findViewById(R.id.solarEvent2Button)
        offsetTimeLayout = findViewById(R.id.offsetTimeLayout)
        solarEvent2Layout = findViewById(R.id.solarEvent2Layout)
        
        // Classic mode elements
        selectTimeButton = findViewById(R.id.selectTimeButton)
        
        // Sun course view
        sunCourseView = findViewById(R.id.sunCourseView)
        
        // Day buttons
        dayButtons["sunday"] = findViewById(R.id.sundayButton)
        dayButtons["monday"] = findViewById(R.id.mondayButton)
        dayButtons["tuesday"] = findViewById(R.id.tuesdayButton)
        dayButtons["wednesday"] = findViewById(R.id.wednesdayButton)
        dayButtons["thursday"] = findViewById(R.id.thursdayButton)
        dayButtons["friday"] = findViewById(R.id.fridayButton)
        dayButtons["saturday"] = findViewById(R.id.saturdayButton)
        
        // Action buttons
        saveFab = findViewById(R.id.saveFab)
        
        // Settings switches
        vibrateSwitch = findViewById(R.id.vibrateSwitch)
        snoozeSwitch = findViewById(R.id.snoozeSwitch)
        gradualVolumeSwitch = findViewById(R.id.gradualVolumeSwitch)
    }
    
    private fun loadAlarmData() {
        if (alarmId != -1L) {
            // Load alarm using ViewModel (which uses Repository)
            viewModel.allAlarms.observe(this) { alarms ->
                currentAlarm = alarms.find { it.id == alarmId }
                currentAlarm?.let { alarm ->
                alarmLabelInput.setText(alarm.label)
                
                // Determine mode based on alarm data
                if (alarm.relative1 != AppConstants.RELATIVE_TIME_PICK_TIME || alarm.mode != AppConstants.ALARM_MODE_AT) {
                    currentMode = "sun"
                    sunTimingMode = alarm.mode
                    solarEvent1 = alarm.relative1
                    solarEvent2 = alarm.relative2
                    // Load offset from time1 (stored in milliseconds)
                    if (alarm.mode == AppConstants.ALARM_MODE_BEFORE || alarm.mode == AppConstants.ALARM_MODE_AFTER) {
                        offsetMinutes = (alarm.time1 / 1000 / 60).toInt()
                    }
                } else {
                    currentMode = "classic"
                    selectedTime = alarm.time1
                }
                
                // Set day selections
                dayButtons.forEach { (day, button) ->
                    val isSelected = alarm.getDayEnabled(day)
                    button.isSelected = isSelected
                    updateDayButtonState(button, isSelected)
                }
                
                // Load alarm settings
                vibrateEnabled = alarm.vibrate
                snoozeEnabled = alarm.snoozeEnabled
                snoozeDuration = alarm.snoozeDuration
                snoozeCount = alarm.snoozeCount
                alarmVolume = alarm.volume
                gradualVolumeEnabled = alarm.gradualVolume
                
                vibrateSwitch.isChecked = vibrateEnabled
                snoozeSwitch.isChecked = snoozeEnabled
                gradualVolumeSwitch.isChecked = gradualVolumeEnabled
                updateSnoozeDisplay()
                
                updateUI()
                updateSunCourseVisualization()
                }
            } ?: run {
                // Alarm not found
                Log.e("AlarmEditorActivity", "Alarm with ID $alarmId not found")
                finish()
            }
        } else {
            // New alarm defaults
            currentMode = "classic"
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 30)
            selectedTime = calendar.timeInMillis
            updateUI()
        }
    }
    
    private fun setupListeners() {
        // Mode selection
        sunModeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            currentMode = "sun"
            updateUI()
            updateSunCourseVisualization()
        }
        
        classicModeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            currentMode = "classic"
            updateUI()
        }
        
        // Sun timing mode
        sunTimingToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                HapticFeedbackHelper.performClick(group)
                sunTimingMode = when (checkedId) {
                    R.id.atButton -> AppConstants.ALARM_MODE_AT
                    R.id.beforeButton -> AppConstants.ALARM_MODE_BEFORE
                    R.id.afterButton -> AppConstants.ALARM_MODE_AFTER
                    R.id.betweenButton -> AppConstants.ALARM_MODE_BETWEEN
                    else -> AppConstants.ALARM_MODE_AT
                }
                updateSunModeUI()
                updateCalculatedTime()
                updateSunCourseVisualization()
            }
        }
        
        // Solar event selection
        solarEvent1Button.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            showSolarEventPicker(1)
        }
        
        solarEvent2Button.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            showSolarEventPicker(2)
        }
        
        // Offset time button (opens time picker)
        val offsetTimeButton = findViewById<MaterialButton>(R.id.offsetTimeButton)
        offsetTimeButton?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            showOffsetTimePicker()
        }
        
        // Quick preset buttons
        setupOffsetPresetButton(R.id.preset15mButton, 15)
        setupOffsetPresetButton(R.id.preset30mButton, 30)
        setupOffsetPresetButton(R.id.preset1hButton, 60)
        setupOffsetPresetButton(R.id.preset2hButton, 120)
        setupOffsetPresetButton(R.id.preset3hButton, 180)
        setupOffsetPresetButton(R.id.preset6hButton, 360)
        setupOffsetPresetButton(R.id.preset8hButton, 480)
        setupOffsetPresetButton(R.id.preset12hButton, 720)
        
        // Quick preset buttons
        findViewById<MaterialButton>(R.id.presetSunriseButton)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            solarEvent1 = "Sunrise"
            sunTimingMode = AppConstants.ALARM_MODE_AT
            sunTimingToggle.check(R.id.atButton)
            updateSolarEventDisplay()
            updateSunModeUI()
        }
        
        findViewById<MaterialButton>(R.id.presetSunsetButton)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            solarEvent1 = "Sunset"
            sunTimingMode = AppConstants.ALARM_MODE_AT
            sunTimingToggle.check(R.id.atButton)
            updateSolarEventDisplay()
            updateSunModeUI()
        }
        
        findViewById<MaterialButton>(R.id.presetDawnButton)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            solarEvent1 = "Civil Dawn"
            sunTimingMode = AppConstants.ALARM_MODE_AT
            sunTimingToggle.check(R.id.atButton)
            updateSolarEventDisplay()
            updateSunModeUI()
        }
        
        // Interactive sun course view
        sunCourseView.onSolarEventSelected = { eventName ->
            HapticFeedbackHelper.performClick(sunCourseView)
            solarEvent1 = eventName
            sunTimingMode = AppConstants.ALARM_MODE_AT
            sunTimingToggle.check(R.id.atButton)
            updateSolarEventDisplay()
            updateSunModeUI()
        }
        
        // Classic time selection - Use Apple-style time picker
        selectTimeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            showAppleTimePicker()
        }
        
        // Day buttons
        dayButtons.forEach { (day, button) ->
            button.setOnClickListener {
                HapticFeedbackHelper.performLightClick(it)
                AnimationHelper.scaleWithBounce(it)
                val isSelected = !button.isSelected
                button.isSelected = isSelected
                updateDayButtonState(button, isSelected)
            }
        }
        
        // Day presets
        findViewById<MaterialButton>(R.id.weekdaysPreset)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            setDayPreset("weekdays")
        }
        
        findViewById<MaterialButton>(R.id.weekendsPreset)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            setDayPreset("weekends")
        }
        
        findViewById<MaterialButton>(R.id.everydayPreset)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            setDayPreset("everyday")
        }
        
        // Ringtone button with preview
        findViewById<MaterialButton>(R.id.ringtoneButton).setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            
            // Preview sound
            currentAlarm?.let { alarm ->
                if (soundPreviewHelper.isPreviewPlaying()) {
                    soundPreviewHelper.stopPreview()
                } else {
                    soundPreviewHelper.previewSound(alarm.ringtoneUri)
                }
            }
        }
        
        // Long press to change ringtone
        findViewById<MaterialButton>(R.id.ringtoneButton).setOnLongClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            val ringtoneActivity = Intent(this, RingtoneActivity::class.java)
            if (alarmId != -1L) {
                ringtoneActivity.putExtra("alarm_id", alarmId)
            }
            startActivity(ringtoneActivity)
            true
        }
        
        // Vibrate switch
        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            HapticFeedbackHelper.performLightClick(vibrateSwitch)
            vibrateEnabled = isChecked
        }
        
        // Snooze switch
        snoozeSwitch.setOnCheckedChangeListener { _, isChecked ->
            HapticFeedbackHelper.performLightClick(snoozeSwitch)
            snoozeEnabled = isChecked
            findViewById<View>(R.id.snoozeSettingsLayout).visibility = 
                if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Initialize snooze settings visibility
        findViewById<View>(R.id.snoozeSettingsLayout).visibility = 
            if (snoozeEnabled) View.VISIBLE else View.GONE
        
        // Snooze duration buttons
        findViewById<MaterialButton>(R.id.decreaseSnoozeDurationButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            snoozeDuration = maxOf(5, snoozeDuration - 5)
            updateSnoozeDisplay()
        }
        
        findViewById<MaterialButton>(R.id.increaseSnoozeDurationButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            snoozeDuration = minOf(30, snoozeDuration + 5)
            updateSnoozeDisplay()
        }
        
        // Snooze count buttons
        findViewById<MaterialButton>(R.id.decreaseSnoozeCountButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            snoozeCount = maxOf(1, snoozeCount - 1)
            updateSnoozeDisplay()
        }
        
        findViewById<MaterialButton>(R.id.increaseSnoozeCountButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            snoozeCount = minOf(10, snoozeCount + 1)
            updateSnoozeDisplay()
        }
        
        // Gradual volume switch
        gradualVolumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            HapticFeedbackHelper.performLightClick(gradualVolumeSwitch)
            gradualVolumeEnabled = isChecked
        }
        
        // Delete button
        findViewById<MaterialButton>(R.id.deleteAlarmButton).apply {
            if (alarmId == -1L) {
                visibility = View.GONE
            } else {
                setOnClickListener {
                    HapticFeedbackHelper.performHeavyClick(it)
                    HapticFeedbackHelper.performDeleteFeedback(this@AlarmEditorActivity)
                    showDeleteConfirmation()
                }
            }
        }
        
        // Save button
        saveFab.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            HapticFeedbackHelper.performSuccessFeedback(this)
            saveAlarm()
        }
    }
    
    private fun updateUI() {
        // Update mode button states with animation
        if (currentMode == "sun") {
            sunModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_primaryContainer))
                setTextColor(getColor(R.color.md_theme_dark_onPrimaryContainer))
            }
            classicModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_surfaceVariant))
                setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
            }
            
            // Animate card transitions
            if (classicModeCard.visibility == View.VISIBLE) {
                AnimationHelper.fadeOut(classicModeCard) {
                    classicModeCard.visibility = View.GONE
                }
            }
            if (sunModeCard.visibility != View.VISIBLE) {
                sunModeCard.visibility = View.VISIBLE
                AnimationHelper.fadeIn(sunModeCard)
            }
            
            // Set sun timing mode
            when (sunTimingMode) {
                AppConstants.ALARM_MODE_AT -> sunTimingToggle.check(R.id.atButton)
                AppConstants.ALARM_MODE_BEFORE -> sunTimingToggle.check(R.id.beforeButton)
                AppConstants.ALARM_MODE_AFTER -> sunTimingToggle.check(R.id.afterButton)
                AppConstants.ALARM_MODE_BETWEEN -> sunTimingToggle.check(R.id.betweenButton)
            }
            
            updateSunModeUI()
        } else {
            classicModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_primaryContainer))
                setTextColor(getColor(R.color.md_theme_dark_onPrimaryContainer))
            }
            sunModeButton.apply {
                setBackgroundColor(getColor(R.color.md_theme_dark_surfaceVariant))
                setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
            }
            
            // Animate card transitions
            if (sunModeCard.visibility == View.VISIBLE) {
                AnimationHelper.fadeOut(sunModeCard) {
                    sunModeCard.visibility = View.GONE
                }
            }
            if (classicModeCard.visibility != View.VISIBLE) {
                classicModeCard.visibility = View.VISIBLE
                AnimationHelper.fadeIn(classicModeCard)
            }
            
            if (calculatedTimeCard.visibility == View.VISIBLE) {
                AnimationHelper.fadeOut(calculatedTimeCard) {
                    calculatedTimeCard.visibility = View.GONE
                }
            }
            
            updateClassicTimeDisplay()
        }
    }
    
    private fun updateSunModeUI() {
        when (sunTimingMode) {
            AppConstants.ALARM_MODE_AT -> {
                if (offsetTimeLayout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(offsetTimeLayout) { offsetTimeLayout.visibility = View.GONE }
                }
                if (solarEvent2Layout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(solarEvent2Layout) { solarEvent2Layout.visibility = View.GONE }
                }
            }
            AppConstants.ALARM_MODE_BEFORE, AppConstants.ALARM_MODE_AFTER -> {
                if (offsetTimeLayout.visibility != View.VISIBLE) {
                    offsetTimeLayout.visibility = View.VISIBLE
                    AnimationHelper.fadeIn(offsetTimeLayout)
                }
                if (solarEvent2Layout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(solarEvent2Layout) { solarEvent2Layout.visibility = View.GONE }
                }
                // Initialize slider value
                updateOffsetDisplay()
            }
            "Between" -> {
                if (offsetTimeLayout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(offsetTimeLayout) { offsetTimeLayout.visibility = View.GONE }
                }
                if (solarEvent2Layout.visibility != View.VISIBLE) {
                    solarEvent2Layout.visibility = View.VISIBLE
                    AnimationHelper.fadeIn(solarEvent2Layout)
                }
            }
        }
        
        updateSolarEventDisplay()
        updateCalculatedTime()
        updateSunCourseVisualization()
    }
    
    private fun updateSolarEventDisplay() {
        val emoji = when (solarEvent1) {
            "Astronomical Dawn" -> "🌄"
            "Nautical Dawn" -> "🌅"
            "Civil Dawn" -> "🌆"
            "Sunrise" -> "🌅"
            "Solar Noon" -> "☀️"
            "Sunset" -> "🌇"
            "Civil Dusk" -> "🌆"
            "Nautical Dusk" -> "🌃"
            "Astronomical Dusk" -> "🌌"
            else -> "🌅"
        }
        solarEvent1Button.text = "$emoji $solarEvent1"
        
        if (sunTimingMode == AppConstants.ALARM_MODE_BETWEEN) {
            val emoji2 = when (solarEvent2) {
                "Astronomical Dawn" -> "🌄"
                "Nautical Dawn" -> "🌅"
                "Civil Dawn" -> "🌆"
                "Sunrise" -> "🌅"
                "Solar Noon" -> "☀️"
                "Sunset" -> "🌇"
                "Civil Dusk" -> "🌆"
                "Nautical Dusk" -> "🌃"
                "Astronomical Dusk" -> "🌌"
                else -> "🌇"
            }
            solarEvent2Button.text = "$emoji2 $solarEvent2"
        }
    }
    
    private fun updateOffsetDisplay() {
        val offsetTimeButton = findViewById<MaterialButton>(R.id.offsetTimeButton)
        offsetTimeButton?.text = TimeUtils.formatMinutesAsHours(offsetMinutes)
    }
    
    private fun setupOffsetPresetButton(buttonId: Int, minutes: Int) {
        findViewById<MaterialButton>(buttonId)?.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            offsetMinutes = minutes
            updateOffsetDisplay()
            updateCalculatedTime()
            updateSunCourseVisualization()
        }
    }
    
    private fun showOffsetTimePicker() {
        val hours = offsetMinutes / 60
        val minutes = offsetMinutes % 60
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                offsetMinutes = (selectedHour * 60) + selectedMinute
                // Clamp to max 12 hours (720 minutes)
                if (offsetMinutes > 720) {
                    offsetMinutes = 720
                }
                // Ensure minimum of 5 minutes
                if (offsetMinutes < 5) {
                    offsetMinutes = 5
                }
                updateOffsetDisplay()
                updateCalculatedTime()
                updateSunCourseVisualization()
            },
            hours,
            minutes,
            false // 24-hour format
        )
        
        timePickerDialog.setTitle("Select Time Offset")
        timePickerDialog.show()
    }
    
    private fun updateSnoozeDisplay() {
        findViewById<TextView>(R.id.snoozeDurationText).text = "$snoozeDuration min"
        findViewById<TextView>(R.id.snoozeCountText).text = "$snoozeCount times"
    }
    
    private fun updateSunCourseVisualization() {
        activityScope.launch(Dispatchers.IO) {
            try {
                // Get astronomy data from ViewModel (which uses Repository)
                editorViewModel.getAstronomyData(forceRefresh = false)
                // Wait a bit for the data to load
                kotlinx.coroutines.delay(100)
                val astronomyData = editorViewModel.astronomyData.value
                
                if (astronomyData != null) {
                    launch(Dispatchers.Main) {
                        // Set all sun times for full visualization
                        sunCourseView.setAllSunTimes(
                            astronomyData.astroDawn, astronomyData.nauticalDawn, astronomyData.civilDawn,
                            astronomyData.sunrise, astronomyData.solarNoon, astronomyData.sunset,
                            astronomyData.civilDusk, astronomyData.nauticalDusk, astronomyData.astroDusk
                        )
                        
                        // Set marker based on current alarm configuration
                        if (currentMode == "sun") {
                            val markerTime = when (sunTimingMode) {
                                AppConstants.ALARM_MODE_AT -> getSolarEventTime(solarEvent1, astronomyData)
                                AppConstants.ALARM_MODE_BEFORE -> getSolarEventTime(solarEvent1, astronomyData) - (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
                                AppConstants.ALARM_MODE_AFTER -> getSolarEventTime(solarEvent1, astronomyData) + (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
                                AppConstants.ALARM_MODE_BETWEEN -> {
                                    val time1 = getSolarEventTime(solarEvent1, astronomyData)
                                    val time2 = getSolarEventTime(solarEvent2, astronomyData)
                                    (time1 + time2) / 2
                                }
                                else -> 0L
                            }
                            
                            val markerLabel = when (sunTimingMode) {
                                AppConstants.ALARM_MODE_AT -> "Alarm"
                                AppConstants.ALARM_MODE_BEFORE -> "${TimeUtils.formatMinutesAsHours(offsetMinutes)} before"
                                AppConstants.ALARM_MODE_AFTER -> "${TimeUtils.formatMinutesAsHours(offsetMinutes)} after"
                                AppConstants.ALARM_MODE_BETWEEN -> "Between"
                                else -> "Alarm"
                            }
                            
                            sunCourseView.setMarker(markerTime, markerLabel)
                        }
                    }
                } else {
                    // Fallback to SharedPreferences if not in database
                    launch(Dispatchers.Main) {
                        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
                        val astroDawn = sharedPreferences.getLong("astroDawn", 0)
                        val nauticalDawn = sharedPreferences.getLong("nauticalDawn", 0)
                        val civilDawn = sharedPreferences.getLong("civilDawn", 0)
                        val sunrise = sharedPreferences.getLong("sunrise", 0)
                        val solarNoon = sharedPreferences.getLong("solarNoon", 0)
                        val sunset = sharedPreferences.getLong("sunset", 0)
                        val civilDusk = sharedPreferences.getLong("civilDusk", 0)
                        val nauticalDusk = sharedPreferences.getLong("nauticalDusk", 0)
                        val astroDusk = sharedPreferences.getLong("astroDusk", 0)
                        
                        sunCourseView.setAllSunTimes(
                            astroDawn, nauticalDawn, civilDawn,
                            sunrise, solarNoon, sunset,
                            civilDusk, nauticalDusk, astroDusk
                        )
                        
                        if (currentMode == "sun") {
                            val markerTime = when (sunTimingMode) {
                                AppConstants.ALARM_MODE_AT -> getSolarEventTime(solarEvent1, sharedPreferences)
                                AppConstants.ALARM_MODE_BEFORE -> getSolarEventTime(solarEvent1, sharedPreferences) - (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
                                AppConstants.ALARM_MODE_AFTER -> getSolarEventTime(solarEvent1, sharedPreferences) + (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
                                AppConstants.ALARM_MODE_BETWEEN -> {
                                    val time1 = getSolarEventTime(solarEvent1, sharedPreferences)
                                    val time2 = getSolarEventTime(solarEvent2, sharedPreferences)
                                    (time1 + time2) / 2
                                }
                                else -> 0L
                            }
                            
                            val markerLabel = when (sunTimingMode) {
                                AppConstants.ALARM_MODE_AT -> "Alarm"
                                AppConstants.ALARM_MODE_BEFORE -> "${TimeUtils.formatMinutesAsHours(offsetMinutes)} before"
                                AppConstants.ALARM_MODE_AFTER -> "${TimeUtils.formatMinutesAsHours(offsetMinutes)} after"
                                AppConstants.ALARM_MODE_BETWEEN -> "Between"
                                else -> "Alarm"
                            }
                            
                            sunCourseView.setMarker(markerTime, markerLabel)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmEditorActivity", "Error loading astronomy data", e)
                // Fallback to SharedPreferences on error
                launch(Dispatchers.Main) {
                    val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
                    val astroDawn = sharedPreferences.getLong("astroDawn", 0)
                    val nauticalDawn = sharedPreferences.getLong("nauticalDawn", 0)
                    val civilDawn = sharedPreferences.getLong("civilDawn", 0)
                    val sunrise = sharedPreferences.getLong("sunrise", 0)
                    val solarNoon = sharedPreferences.getLong("solarNoon", 0)
                    val sunset = sharedPreferences.getLong("sunset", 0)
                    val civilDusk = sharedPreferences.getLong("civilDusk", 0)
                    val nauticalDusk = sharedPreferences.getLong("nauticalDusk", 0)
                    val astroDusk = sharedPreferences.getLong("astroDusk", 0)
                    
                    sunCourseView.setAllSunTimes(
                        astroDawn, nauticalDawn, civilDawn,
                        sunrise, solarNoon, sunset,
                        civilDusk, nauticalDusk, astroDusk
                    )
                }
            }
        }
    }
    
    private fun updateCalculatedTime() {
        if (currentMode != "sun") return
        
        if (calculatedTimeCard.visibility != View.VISIBLE) {
            calculatedTimeCard.visibility = View.VISIBLE
            AnimationHelper.fadeIn(calculatedTimeCard)
        }
        
        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        val event1Time = getSolarEventTime(solarEvent1, sharedPreferences)
        
        val calculatedTime = when (sunTimingMode) {
            AppConstants.ALARM_MODE_AT -> event1Time
            AppConstants.ALARM_MODE_BEFORE -> event1Time - (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
            AppConstants.ALARM_MODE_AFTER -> event1Time + (offsetMinutes * AppConstants.MILLIS_PER_MINUTE)
            AppConstants.ALARM_MODE_BETWEEN -> {
                val event2Time = getSolarEventTime(solarEvent2, sharedPreferences)
                (event1Time + event2Time) / 2
            }
            else -> event1Time
        }
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = calculatedTime
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        calculatedTimeText.text = sdf.format(calendar.time)
    }
    
    private fun getSolarEventTime(event: String, astronomyData: com.rooster.rooster.data.local.entity.AstronomyDataEntity): Long {
        return when (event) {
            "Astronomical Dawn" -> astronomyData.astroDawn
            "Nautical Dawn" -> astronomyData.nauticalDawn
            "Civil Dawn" -> astronomyData.civilDawn
            "Sunrise" -> astronomyData.sunrise
            "Solar Noon" -> astronomyData.solarNoon
            "Sunset" -> astronomyData.sunset
            "Civil Dusk" -> astronomyData.civilDusk
            "Nautical Dusk" -> astronomyData.nauticalDusk
            "Astronomical Dusk" -> astronomyData.astroDusk
            else -> System.currentTimeMillis()
        }
    }
    
    private fun getSolarEventTime(event: String, prefs: android.content.SharedPreferences): Long {
        return when (event) {
            "Astronomical Dawn" -> prefs.getLong("astroDawn", 0)
            "Nautical Dawn" -> prefs.getLong("nauticalDawn", 0)
            "Civil Dawn" -> prefs.getLong("civilDawn", 0)
            "Sunrise" -> prefs.getLong("sunrise", 0)
            "Solar Noon" -> prefs.getLong("solarNoon", 0)
            "Sunset" -> prefs.getLong("sunset", 0)
            "Civil Dusk" -> prefs.getLong("civilDusk", 0)
            "Nautical Dusk" -> prefs.getLong("nauticalDusk", 0)
            "Astronomical Dusk" -> prefs.getLong("astroDusk", 0)
            else -> System.currentTimeMillis()
        }
    }
    
    private fun updateClassicTimeDisplay() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedTime
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        selectTimeButton.text = sdf.format(calendar.time)
    }
    
    private fun showSolarEventPicker(eventNumber: Int) {
        val cleanedEvents = solarEvents.map { it.substring(2) }.toTypedArray()
        
        // Create a custom dialog with better visual design
        val dialogView = layoutInflater.inflate(R.layout.dialog_solar_event_picker, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.solarEventRecyclerView)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Solar Event")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        
        val adapter = SolarEventAdapter(solarEvents.toList()) { selectedIndex ->
            HapticFeedbackHelper.performSuccessFeedback(this)
            val selected = cleanedEvents[selectedIndex]
            if (eventNumber == 1) {
                solarEvent1 = selected
            } else {
                solarEvent2 = selected
            }
            updateSolarEventDisplay()
            updateCalculatedTime()
            updateSunCourseVisualization()
            dialog.dismiss()
        }
        
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        dialog.show()
    }
    
    private fun showAppleTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedTime
        
        com.rooster.rooster.ui.AppleTimePickerDialog(
            this,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        ) { hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            selectedTime = calendar.timeInMillis
            updateClassicTimeDisplay()
        }.show()
    }
    
    private fun showTimePicker() {
        showAppleTimePicker()
    }
    
    private fun updateDayButtonState(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(getColor(R.color.md_theme_dark_primaryContainer))
            button.setTextColor(getColor(R.color.md_theme_dark_onPrimaryContainer))
        } else {
            button.setBackgroundColor(getColor(android.R.color.transparent))
            button.setTextColor(getColor(R.color.md_theme_dark_primary))
        }
    }
    
    private fun setDayPreset(preset: String) {
        when (preset) {
            "weekdays" -> {
                // Monday to Friday
                dayButtons["monday"]?.isSelected = true
                dayButtons["tuesday"]?.isSelected = true
                dayButtons["wednesday"]?.isSelected = true
                dayButtons["thursday"]?.isSelected = true
                dayButtons["friday"]?.isSelected = true
                dayButtons["saturday"]?.isSelected = false
                dayButtons["sunday"]?.isSelected = false
            }
            "weekends" -> {
                // Saturday and Sunday
                dayButtons["monday"]?.isSelected = false
                dayButtons["tuesday"]?.isSelected = false
                dayButtons["wednesday"]?.isSelected = false
                dayButtons["thursday"]?.isSelected = false
                dayButtons["friday"]?.isSelected = false
                dayButtons["saturday"]?.isSelected = true
                dayButtons["sunday"]?.isSelected = true
            }
            "everyday" -> {
                // All days
                dayButtons.values.forEach { it.isSelected = true }
            }
        }
        
        // Update button states
        dayButtons.forEach { (_, button) ->
            updateDayButtonState(button, button.isSelected)
            AnimationHelper.scaleWithBounce(button)
        }
        
        HapticFeedbackHelper.performSuccessFeedback(this)
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Delete") { _, _ ->
                currentAlarm?.let {
                    viewModel.deleteAlarm(it)
                }
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveAlarm() {
        val label = alarmLabelInput.text.toString().takeIf { it.isNotBlank() } ?: "Alarm"
        
        // Get day selections
        val daysMap = dayButtons.mapValues { it.value.isSelected }
        
        // Comprehensive validation using ValidationHelper
        val validationResult = ValidationHelper.validateAlarmEditorInputs(
            label = label,
            mode = currentMode,
            sunTimingMode = sunTimingMode,
            solarEvent1 = solarEvent1,
            solarEvent2 = solarEvent2,
            offsetMinutes = offsetMinutes,
            selectedTime = selectedTime,
            monday = daysMap["monday"] ?: false,
            tuesday = daysMap["tuesday"] ?: false,
            wednesday = daysMap["wednesday"] ?: false,
            thursday = daysMap["thursday"] ?: false,
            friday = daysMap["friday"] ?: false,
            saturday = daysMap["saturday"] ?: false,
            sunday = daysMap["sunday"] ?: false,
            snoozeDuration = snoozeDuration,
            snoozeCount = snoozeCount,
            volume = alarmVolume
        )
        
        // If validation fails, show error messages and return
        if (validationResult.isError()) {
            HapticFeedbackHelper.performErrorFeedback(this)
            val errorMessage = validationResult.getErrorMessage()
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                errorMessage,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
            return
        }
        
        // Sanitize label using ValidationHelper
        val sanitizedLabel = ValidationHelper.sanitizeLabel(label)
        
        // Determine time values based on mode
        val time1: Long
        val time2: Long
        val mode: String
        val relative1: String
        val relative2: String
        
        if (currentMode == "sun") {
            // Sun mode
            mode = sunTimingMode
            relative1 = solarEvent1
            relative2 = if (sunTimingMode == AppConstants.ALARM_MODE_BETWEEN) solarEvent2 else ""
            time1 = if (sunTimingMode == AppConstants.ALARM_MODE_BEFORE || sunTimingMode == AppConstants.ALARM_MODE_AFTER) {
                offsetMinutes * AppConstants.MILLIS_PER_MINUTE
            } else {
                0L
            }
            time2 = 0L
        } else {
            // Classic mode
            mode = AppConstants.ALARM_MODE_AT
            relative1 = AppConstants.RELATIVE_TIME_PICK_TIME
            relative2 = ""
            time1 = selectedTime
            time2 = 0L
        }
        
        // Get ringtone URI from current alarm or use default
        val existingAlarm = currentAlarm
        val ringtoneUri = existingAlarm?.ringtoneUri ?: AppConstants.DEFAULT_RINGTONE_URI
        
        if (alarmId != -1L && existingAlarm != null) {
            // Update existing alarm
            val updatedAlarm = existingAlarm.copy(
                label = sanitizedLabel,
                mode = mode,
                relative1 = relative1,
                relative2 = relative2,
                time1 = time1,
                time2 = time2,
                monday = daysMap["monday"] ?: false,
                tuesday = daysMap["tuesday"] ?: false,
                wednesday = daysMap["wednesday"] ?: false,
                thursday = daysMap["thursday"] ?: false,
                friday = daysMap["friday"] ?: false,
                saturday = daysMap["saturday"] ?: false,
                sunday = daysMap["sunday"] ?: false,
                enabled = true,
                vibrate = vibrateEnabled,
                snoozeEnabled = snoozeEnabled,
                snoozeDuration = snoozeDuration,
                snoozeCount = snoozeCount,
                volume = alarmVolume,
                gradualVolume = gradualVolumeEnabled
            )
            
            // Use ViewModel to update alarm (which uses Repository and recalculates time)
            viewModel.updateAlarm(updatedAlarm)
        } else {
            // Create new alarm
            // First, create a basic AlarmCreation to insert
            val alarmCreation = AlarmCreation(
                label = sanitizedLabel,
                enabled = true,
                mode = mode,
                ringtoneUri = ringtoneUri,
                relative1 = relative1,
                relative2 = relative2,
                time1 = time1,
                time2 = time2,
                calculatedTime = 0L // Will be calculated by ViewModel
            )
            
            // Insert the alarm and get its ID, then update it with all fields
            viewModel.insertAlarm(alarmCreation) { newAlarmId ->
                // After insertion, create a full Alarm with all settings and update it
                val fullAlarm = Alarm(
                    id = newAlarmId,
                    label = sanitizedLabel,
                    enabled = true,
                    mode = mode,
                    ringtoneUri = ringtoneUri,
                    relative1 = relative1,
                    relative2 = relative2,
                    time1 = time1,
                    time2 = time2,
                    calculatedTime = 0L, // Will be calculated by ViewModel
                    monday = daysMap["monday"] ?: false,
                    tuesday = daysMap["tuesday"] ?: false,
                    wednesday = daysMap["wednesday"] ?: false,
                    thursday = daysMap["thursday"] ?: false,
                    friday = daysMap["friday"] ?: false,
                    saturday = daysMap["saturday"] ?: false,
                    sunday = daysMap["sunday"] ?: false,
                    vibrate = vibrateEnabled,
                    snoozeEnabled = snoozeEnabled,
                    snoozeDuration = snoozeDuration,
                    snoozeCount = snoozeCount,
                    volume = alarmVolume,
                    gradualVolume = gradualVolumeEnabled
                )
                
                // Update with all fields (this will also recalculate the time)
                viewModel.updateAlarm(fullAlarm)
            }
        }
        
        finish()
    }
}
