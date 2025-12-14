package com.rooster.rooster

import android.media.RingtoneManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.AnimationHelper

class RingtoneAdapter(
    private val ringtones: List<RingtoneItem>,
    private val onRingtoneSelect: (RingtoneItem) -> Unit,
    private val onRingtonePreview: (RingtoneItem) -> Unit,
    private val isCurrentlyPreviewing: (RingtoneItem) -> Boolean
) : RecyclerView.Adapter<RingtoneAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ringtoneCard: MaterialCardView = itemView.findViewById(R.id.ringtoneCard)
        val ringtoneTitle: TextView = itemView.findViewById(R.id.ringtoneTitle)
        val previewIcon: TextView = itemView.findViewById(R.id.previewIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ringtone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ringtone = ringtones[position]
        val context = holder.ringtoneCard.context

        // Set ringtone title
        holder.ringtoneTitle.text = ringtone.title

        // Update preview icon based on state
        val isPreviewing = isCurrentlyPreviewing(ringtone)
        holder.previewIcon.text = if (isPreviewing) "⏸" else "▶"

        // Animate card entry
        holder.ringtoneCard.alpha = 0f
        holder.ringtoneCard.translationY = 20f
        holder.ringtoneCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(position * 30L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Click anywhere on card: if previewing, select; otherwise preview
        holder.ringtoneCard.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            if (isPreviewing) {
                onRingtoneSelect(ringtone)
            } else {
                onRingtonePreview(ringtone)
            }
        }

        // Preview icon also triggers preview/select
        holder.previewIcon.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            AnimationHelper.scaleWithBounce(it)
            if (isPreviewing) {
                onRingtoneSelect(ringtone)
            } else {
                onRingtonePreview(ringtone)
            }
        }

        // Long press to select directly
        holder.ringtoneCard.setOnLongClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            onRingtoneSelect(ringtone)
            true
        }
    }

    override fun getItemCount(): Int = ringtones.size
}

data class RingtoneItem(
    val title: String,
    val uri: Uri?
)
