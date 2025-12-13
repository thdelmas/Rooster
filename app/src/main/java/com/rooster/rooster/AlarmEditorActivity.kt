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
import com.rooster.rooster.util.AnimationHelper
import com.rooster.rooster.util.HapticFeedbackHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AlarmEditorActivity : AppCompatActivity() {

    private val viewModel: AlarmListViewModel by viewModels()
    private lateinit var alarmDbHelper: AlarmDbHelper
    
    private var alarmId: Long = -1
    private var currentAlarm: Alarm? = null
    private var currentMode: String = "sun" // "sun" or "classic"
    private var sunTimingMode: String = "At" // "At", "Before", "After", "Between"
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
    private lateinit var offsetTimeText: TextView
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
        
        alarmDbHelper = AlarmDbHelper(this)
        alarmId = intent.getLongExtra("alarm_id", -1)
        soundPreviewHelper = com.rooster.rooster.ui.SoundPreviewHelper(this)
        
        initializeViews()
        loadAlarmData()
        setupListeners()
    }
    
    override fun onDestroy() {
        super.onDestroy()
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
        offsetTimeText = findViewById(R.id.offsetTimeText)
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
            currentAlarm = alarmDbHelper.getAlarm(alarmId)
            currentAlarm?.let { alarm ->
                alarmLabelInput.setText(alarm.label)
                
                // Determine mode based on alarm data
                if (alarm.relative1 != "Pick Time" || alarm.mode != "At") {
                    currentMode = "sun"
                    sunTimingMode = alarm.mode
                    solarEvent1 = alarm.relative1
                    solarEvent2 = alarm.relative2
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
            currentMode = "sun"
            updateUI()
        }
        
        classicModeButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            currentMode = "classic"
            updateUI()
        }
        
        // Sun timing mode
        sunTimingToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                HapticFeedbackHelper.performClick(group)
                sunTimingMode = when (checkedId) {
                    R.id.atButton -> "At"
                    R.id.beforeButton -> "Before"
                    R.id.afterButton -> "After"
                    R.id.betweenButton -> "Between"
                    else -> "At"
                }
                updateSunModeUI()
                updateCalculatedTime()
            }
        }
        
        // Solar event selection
        solarEvent1Button.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            showSolarEventPicker(1)
        }
        
        solarEvent2Button.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            showSolarEventPicker(2)
        }
        
        // Offset adjustment
        findViewById<MaterialButton>(R.id.decreaseOffsetButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            offsetMinutes = maxOf(5, offsetMinutes - 5)
            updateOffsetDisplay()
            updateCalculatedTime()
        }
        
        findViewById<MaterialButton>(R.id.increaseOffsetButton).setOnClickListener {
            HapticFeedbackHelper.performLightClick(it)
            offsetMinutes = minOf(180, offsetMinutes + 5)
            updateOffsetDisplay()
            updateCalculatedTime()
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
                "At" -> sunTimingToggle.check(R.id.atButton)
                "Before" -> sunTimingToggle.check(R.id.beforeButton)
                "After" -> sunTimingToggle.check(R.id.afterButton)
                "Between" -> sunTimingToggle.check(R.id.betweenButton)
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
            "At" -> {
                if (offsetTimeLayout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(offsetTimeLayout) { offsetTimeLayout.visibility = View.GONE }
                }
                if (solarEvent2Layout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(solarEvent2Layout) { solarEvent2Layout.visibility = View.GONE }
                }
            }
            "Before", "After" -> {
                if (offsetTimeLayout.visibility != View.VISIBLE) {
                    offsetTimeLayout.visibility = View.VISIBLE
                    AnimationHelper.fadeIn(offsetTimeLayout)
                }
                if (solarEvent2Layout.visibility == View.VISIBLE) {
                    AnimationHelper.fadeOut(solarEvent2Layout) { solarEvent2Layout.visibility = View.GONE }
                }
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
        
        if (sunTimingMode == "Between") {
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
        offsetTimeText.text = "$offsetMinutes minutes"
    }
    
    private fun updateSnoozeDisplay() {
        findViewById<TextView>(R.id.snoozeDurationText).text = "$snoozeDuration min"
        findViewById<TextView>(R.id.snoozeCountText).text = "$snoozeCount times"
    }
    
    private fun updateSunCourseVisualization() {
        val sharedPreferences = getSharedPreferences("rooster_prefs", Context.MODE_PRIVATE)
        
        val civilDawn = sharedPreferences.getLong("civilDawn", 0)
        val sunrise = sharedPreferences.getLong("sunrise", 0)
        val solarNoon = sharedPreferences.getLong("solarNoon", 0)
        val sunset = sharedPreferences.getLong("sunset", 0)
        val civilDusk = sharedPreferences.getLong("civilDusk", 0)
        
        sunCourseView.setSunTimes(civilDawn, sunrise, solarNoon, sunset, civilDusk)
        
        // Set marker based on current alarm configuration
        if (currentMode == "sun") {
            val markerTime = when (sunTimingMode) {
                "At" -> getSolarEventTime(solarEvent1, sharedPreferences)
                "Before" -> getSolarEventTime(solarEvent1, sharedPreferences) - (offsetMinutes * 60 * 1000)
                "After" -> getSolarEventTime(solarEvent1, sharedPreferences) + (offsetMinutes * 60 * 1000)
                "Between" -> {
                    val time1 = getSolarEventTime(solarEvent1, sharedPreferences)
                    val time2 = getSolarEventTime(solarEvent2, sharedPreferences)
                    (time1 + time2) / 2
                }
                else -> 0L
            }
            
            val markerEmoji = when (sunTimingMode) {
                "At" -> "⏰"
                "Before" -> "⏰"
                "After" -> "⏰"
                "Between" -> "⏰"
                else -> "⏰"
            }
            
            sunCourseView.setMarker(markerTime, markerEmoji)
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
            "At" -> event1Time
            "Before" -> event1Time - (offsetMinutes * 60 * 1000)
            "After" -> event1Time + (offsetMinutes * 60 * 1000)
            "Between" -> {
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
        
        AlertDialog.Builder(this)
            .setTitle("Select Solar Event")
            .setItems(solarEvents) { _, which ->
                HapticFeedbackHelper.performSuccessFeedback(this)
                val selected = cleanedEvents[which]
                if (eventNumber == 1) {
                    solarEvent1 = selected
                } else {
                    solarEvent2 = selected
                }
                updateSolarEventDisplay()
                updateCalculatedTime()
            }
            .show()
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
                    alarmDbHelper.deleteAlarm(it.id)
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
        
        // Validate that at least one day is selected
        if (!daysMap.values.any { it }) {
            HapticFeedbackHelper.performErrorFeedback(this)
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "Please select at least one day",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        
        val alarm = if (alarmId != -1L && currentAlarm != null) {
            currentAlarm!!.apply {
                this.label = label
                this.monday = daysMap["monday"] ?: false
                this.tuesday = daysMap["tuesday"] ?: false
                this.wednesday = daysMap["wednesday"] ?: false
                this.thursday = daysMap["thursday"] ?: false
                this.friday = daysMap["friday"] ?: false
                this.saturday = daysMap["saturday"] ?: false
                this.sunday = daysMap["sunday"] ?: false
            }
        } else {
            null
        }
        
        if (currentMode == "sun") {
            // Sun mode
            alarm?.apply {
                this.mode = sunTimingMode
                this.relative1 = solarEvent1
                this.relative2 = if (sunTimingMode == "Between") solarEvent2 else ""
                this.time1 = if (sunTimingMode == "Before" || sunTimingMode == "After") {
                    offsetMinutes * 60 * 1000L
                } else {
                    0L
                }
                this.time2 = 0L
            }
        } else {
            // Classic mode
            alarm?.apply {
                this.mode = "At"
                this.relative1 = "Pick Time"
                this.relative2 = ""
                this.time1 = selectedTime
                this.time2 = 0L
            }
        }
        
        alarm?.let {
            it.enabled = true
            it.vibrate = vibrateEnabled
            it.snoozeEnabled = snoozeEnabled
            it.snoozeDuration = snoozeDuration
            it.snoozeCount = snoozeCount
            it.volume = alarmVolume
            it.gradualVolume = gradualVolumeEnabled
            alarmDbHelper.updateAlarm(it)
        }
        
        finish()
    }
}
