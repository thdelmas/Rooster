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
import java.text.SimpleDateFormat
import java.util.*

class ImprovedAlarmAdapter(
    private val alarmList: List<Alarm>,
    private val viewModel: AlarmListViewModel
) : RecyclerView.Adapter<ImprovedAlarmAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmCard: MaterialCardView = itemView.findViewById(R.id.alarmCard)
        val modeBadge: MaterialCardView = itemView.findViewById(R.id.modeBadge)
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

        // Set alarm label
        holder.alarmLabel.text = alarm.label

        // Display time (calculatedTime should already be set from repository)
        val timeStr = formatTime(alarm.calculatedTime)
        holder.alarmTime.text = timeStr

        // Set mode badge and description
        if (alarm.relative1 != "Pick Time") {
            // Sun mode
            holder.modeBadgeText.text = "☀️"
            holder.modeBadge.setCardBackgroundColor(context.getColor(R.color.md_theme_dark_tertiaryContainer))
            holder.modeDescription.text = getModeDescription(alarm)
        } else {
            // Classic mode
            holder.modeBadgeText.text = "🕐"
            holder.modeBadge.setCardBackgroundColor(context.getColor(R.color.md_theme_dark_primaryContainer))
            holder.modeDescription.text = "Classic alarm"
        }

        // Set repeat days
        holder.repeatDays.text = getRepeatDaysText(alarm)

        // Set enable switch
        holder.enableSwitch.isChecked = alarm.enabled
        holder.enableSwitch.setOnCheckedChangeListener { view, isChecked ->
            HapticFeedbackHelper.performToggleFeedback(view)
            alarm.enabled = isChecked
            viewModel.updateAlarm(alarm)
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

    private fun getRepeatDaysText(alarm: Alarm): String {
        val days = mutableListOf<String>()
        if (alarm.monday) days.add("Mon")
        if (alarm.tuesday) days.add("Tue")
        if (alarm.wednesday) days.add("Wed")
        if (alarm.thursday) days.add("Thu")
        if (alarm.friday) days.add("Fri")
        if (alarm.saturday) days.add("Sat")
        if (alarm.sunday) days.add("Sun")

        return when {
            days.isEmpty() -> "Once"
            days.size == 7 -> "Every day"
            days.size == 5 && !alarm.saturday && !alarm.sunday -> "Weekdays"
            days.size == 2 && alarm.saturday && alarm.sunday -> "Weekends"
            else -> days.joinToString(", ")
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
}


