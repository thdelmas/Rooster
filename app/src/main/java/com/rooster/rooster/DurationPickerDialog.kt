package com.rooster.rooster

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.rooster.rooster.util.HapticFeedbackHelper

/**
 * Dialog for selecting a duration (hours and minutes)
 * Similar to Google Clock app's duration picker
 */
class DurationPickerDialog(
    private val initialMinutes: Int,
    private val minMinutes: Int = 5,
    private val maxMinutes: Int = 720,
    private val onDurationSelected: (minutes: Int) -> Unit
) : DialogFragment() {

    private var hoursPicker: NumberPicker? = null
    private var minutesPicker: NumberPicker? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_duration_picker, null)
        
        hoursPicker = view.findViewById(R.id.hoursPicker)
        minutesPicker = view.findViewById(R.id.minutesPicker)
        
        val currentHours = (initialMinutes / 60).coerceIn(0, 12)
        val currentMinutes = (initialMinutes % 60).coerceIn(0, 59)
        
        // Setup hours picker (0-12)
        hoursPicker?.apply {
            minValue = 0
            maxValue = 12
            value = currentHours
            wrapSelectorWheel = false
        }
        
        // Setup minutes picker (0-59)
        minutesPicker?.apply {
            minValue = 0
            maxValue = 59
            value = currentMinutes
            wrapSelectorWheel = false
        }
        
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
        
        cancelButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            dismiss()
        }
        
        confirmButton.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            val selectedHours = hoursPicker?.value ?: 0
            val selectedMinutes = minutesPicker?.value ?: 0
            val totalMinutes = (selectedHours * 60) + selectedMinutes
            
            // Validate and clamp
            val finalMinutes = totalMinutes.coerceIn(minMinutes, maxMinutes)
            onDurationSelected(finalMinutes)
            dismiss()
        }
        
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}

