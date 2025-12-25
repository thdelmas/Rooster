package com.rooster.rooster

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.AnimationHelper
import com.rooster.rooster.util.TimeUtils
import com.rooster.rooster.util.toast
import java.text.SimpleDateFormat
import java.util.*

class ImprovedAlarmAdapter(
    private val alarmList: List<Alarm>,
    private val viewModel: AlarmListViewModel
) : RecyclerView.Adapter<ImprovedAlarmAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmCard: MaterialCardView = itemView.findViewById(R.id.alarmCard)
        val modeBadgeText: TextView = itemView.findViewById(R.id.modeBadgeText)
        val alarmLabel: TextView = itemView.findViewById(R.id.textViewAlarmLabel)
        val alarmTime: TextView = itemView.findViewById(R.id.tvAlarmTime)
        val modeDescription: TextView = itemView.findViewById(R.id.tvModeDescription)
        val repeatDays: TextView = itemView.findViewById(R.id.tvRepeatDays)
        val enableSwitch: MaterialSwitch = itemView.findViewById(R.id.switchAlarmEnabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_item_improved, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarmList[position]
        val context = holder.alarmCard.context

        // Animate card entry
        holder.alarmCard.alpha = 0f
        holder.alarmCard.translationY = 20f
        holder.alarmCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Set alarm label - hide if empty or default name
        val shouldShowLabel = alarm.label.isNotBlank() && alarm.label != "Alarm"
        holder.alarmLabel.visibility = if (shouldShowLabel) View.VISIBLE else View.GONE
        if (shouldShowLabel) {
            holder.alarmLabel.text = alarm.label
        }

        // Display time (calculatedTime should already be set from repository)
        val timeStr = formatTime(alarm.calculatedTime)
        holder.alarmTime.text = timeStr

        // Set mode badge and description
        if (alarm.relative1 != "Pick Time") {
            // Sun mode
            holder.modeBadgeText.text = "☀️"
            holder.modeDescription.text = getModeDescription(alarm)
        } else {
            // Classic mode
            holder.modeBadgeText.text = "🕐"
            holder.modeDescription.text = "Classic alarm"
        }

        // Set day information (Today/Tomorrow/Days interval/day list/every day)
        holder.repeatDays.text = getDayInformationText(alarm)

        // Set enable switch
        holder.enableSwitch.isChecked = alarm.enabled
        holder.enableSwitch.setOnCheckedChangeListener { view, isChecked ->
            HapticFeedbackHelper.performToggleFeedback(view)
            alarm.enabled = isChecked
            viewModel.updateAlarm(alarm)
            // Show toast notification
            if (isChecked) {
                val message = getAlarmRingTimeMessage(alarm)
                context.toast(message)
            } else {
                context.toast("Alarm disabled")
            }
            // Alarm list will update automatically via LiveData
        }

        // Click to edit with animation
        holder.alarmCard.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            // Delay to show animation before navigation
            it.postDelayed({
                val intent = Intent(context, AlarmEditorActivity::class.java)
                intent.putExtra("alarm_id", alarm.id)
                context.startActivity(intent)
            }, 150)
        }

        // Update card appearance based on enabled state
        updateCardAppearance(holder, alarm.enabled, context)
    }

    override fun getItemCount(): Int = alarmList.size

    private fun formatTime(timeInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun getModeDescription(alarm: Alarm): String {
        return when (alarm.mode) {
            "At" -> "At ${alarm.relative1}"
            "Before" -> {
                val minutes = (alarm.time1 / 1000 / 60).toInt()
                "${TimeUtils.formatMinutesAsHours(minutes)} before ${alarm.relative2}"
            }
            "After" -> {
                val minutes = (alarm.time1 / 1000 / 60).toInt()
                "${TimeUtils.formatMinutesAsHours(minutes)} after ${alarm.relative2}"
            }
            "Between" -> "Between ${alarm.relative1} and ${alarm.relative2}"
            else -> "Sun course alarm"
        }
    }

    private fun getDayInformationText(alarm: Alarm): String {
        val dayList = mutableListOf<Int>()
        if (alarm.monday) dayList.add(Calendar.MONDAY)
        if (alarm.tuesday) dayList.add(Calendar.TUESDAY)
        if (alarm.wednesday) dayList.add(Calendar.WEDNESDAY)
        if (alarm.thursday) dayList.add(Calendar.THURSDAY)
        if (alarm.friday) dayList.add(Calendar.FRIDAY)
        if (alarm.saturday) dayList.add(Calendar.SATURDAY)
        if (alarm.sunday) dayList.add(Calendar.SUNDAY)

        return when {
            dayList.isEmpty() -> {
                // If no repeat days, show Today or Tomorrow based on calculatedTime
                val alarmCalendar = Calendar.getInstance()
                alarmCalendar.timeInMillis = alarm.calculatedTime
                val today = Calendar.getInstance()
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                
                when {
                    alarmCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    alarmCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                    alarmCalendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                    alarmCalendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                    else -> {
                        // Show day name if it's further out
                        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                        dayFormat.format(alarmCalendar.time)
                    }
                }
            }
            dayList.size == 7 -> "Every day"
            dayList.size == 5 && !alarm.saturday && !alarm.sunday -> "Weekdays"
            dayList.size == 2 && alarm.saturday && alarm.sunday -> "Weekends"
            else -> {
                // Check if days are consecutive
                val sortedDays = dayList.sorted()
                val formattedDays = sortedDays.map { getShortDayName(it) }
                
                // Check if all days are consecutive (more than 2)
                if (sortedDays.size > 2 && areDaysConsecutive(sortedDays)) {
                    "${formattedDays.first()} - ${formattedDays.last()}"
                } else {
                    formattedDays.joinToString(", ")
                }
            }
        }
    }
    
    private fun areDaysConsecutive(days: List<Int>): Boolean {
        if (days.size < 2) return false
        
        // Check if days are consecutive in a week
        // Handle wrap-around (e.g., Sat, Sun, Mon) - but for simplicity, we'll only handle
        // consecutive days that don't wrap around the week boundary
        for (i in 0 until days.size - 1) {
            val current = days[i]
            val next = days[i + 1]
            
            // Check if next day is exactly one day after current (no wrap-around)
            val expectedNext = current + 1
            if (next != expectedNext) {
                return false
            }
        }
        return true
    }
    
    private fun getShortDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "?"
        }
    }

    private fun updateCardAppearance(holder: ViewHolder, enabled: Boolean, context: Context) {
        if (enabled) {
            holder.alarmTime.alpha = 1.0f
            holder.modeDescription.alpha = 1.0f
            holder.repeatDays.alpha = 1.0f
        } else {
            holder.alarmTime.alpha = 0.5f
            holder.modeDescription.alpha = 0.5f
            holder.repeatDays.alpha = 0.5f
        }
    }

    private fun getAlarmRingTimeMessage(alarm: Alarm): String {
        val alarmCalendar = Calendar.getInstance()
        alarmCalendar.timeInMillis = alarm.calculatedTime
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = sdf.format(alarmCalendar.time)
        
        // Determine if it's today, tomorrow, or a specific day by comparing dates
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val alarmYear = alarmCalendar.get(Calendar.YEAR)
        val alarmMonth = alarmCalendar.get(Calendar.MONTH)
        val alarmDay = alarmCalendar.get(Calendar.DAY_OF_MONTH)
        
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        
        val tomorrowYear = tomorrow.get(Calendar.YEAR)
        val tomorrowMonth = tomorrow.get(Calendar.MONTH)
        val tomorrowDay = tomorrow.get(Calendar.DAY_OF_MONTH)
        
        val timeString = when {
            alarmYear == todayYear && alarmMonth == todayMonth && alarmDay == todayDay -> "today at $formattedTime"
            alarmYear == tomorrowYear && alarmMonth == tomorrowMonth && alarmDay == tomorrowDay -> "tomorrow at $formattedTime"
            else -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                "${dayFormat.format(alarmCalendar.time)} at $formattedTime"
            }
        }
        
        return "Alarm will ring $timeString"
    }
}


