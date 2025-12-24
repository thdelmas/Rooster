package com.rooster.rooster.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.rooster.rooster.R
import com.rooster.rooster.util.HapticFeedbackHelper
import java.util.Calendar

/**
 * iOS-style time picker dialog with wheel pickers
 */
class AppleTimePickerDialog(
    context: Context,
    private val initialHour: Int,
    private val initialMinute: Int,
    private val onTimeSet: (hour: Int, minute: Int) -> Unit
) : Dialog(context, R.style.Theme_Rooster) {

    private lateinit var hourPicker: WheelTimePicker
    private lateinit var minutePicker: WheelTimePicker
    private lateinit var periodPicker: WheelTimePicker
    
    private var selectedHour = initialHour
    private var selectedMinute = initialMinute
    private var is24Hour = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_apple_time_picker)
        
        window?.setBackgroundDrawableResource(R.drawable.card_background_elevated)
        
        setupViews()
        setupListeners()
        initializeTime()
    }
    
    private fun setupViews() {
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        periodPicker = findViewById(R.id.periodPicker)
        
        // Setup hour picker (1-12 for 12-hour format)
        hourPicker.setItems((1..12).map { String.format("%02d", it) })
        
        // Setup minute picker (0-59)
        minutePicker.setItems((0..59).map { String.format("%02d", it) })
        
        // Setup period picker (AM/PM)
        periodPicker.setItems(listOf("AM", "PM"))
    }
    
    private fun setupListeners() {
        hourPicker.onValueChangedListener = { index ->
            selectedHour = convertTo24Hour(index + 1, periodPicker.getSelectedIndex() == 1)
            HapticFeedbackHelper.performLightClick(hourPicker)
        }
        
        minutePicker.onValueChangedListener = { index ->
            selectedMinute = index
            HapticFeedbackHelper.performLightClick(minutePicker)
        }
        
        periodPicker.onValueChangedListener = { index ->
            val currentHour12 = hourPicker.getSelectedIndex() + 1
            selectedHour = convertTo24Hour(currentHour12, index == 1)
            HapticFeedbackHelper.performLightClick(periodPicker)
        }
        
        findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            dismiss()
        }
        
        findViewById<MaterialButton>(R.id.setButton).setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            HapticFeedbackHelper.performSuccessFeedback(context)
            onTimeSet(selectedHour, selectedMinute)
            dismiss()
        }
    }
    
    private fun initializeTime() {
        val (hour12, isPM) = convertTo12Hour(initialHour)
        
        hourPicker.setSelectedIndex(hour12 - 1, false)
        minutePicker.setSelectedIndex(initialMinute, false)
        periodPicker.setSelectedIndex(if (isPM) 1 else 0, false)
        
        selectedHour = initialHour
        selectedMinute = initialMinute
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
}










