package com.rooster.rooster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.rooster.rooster.util.HapticFeedbackHelper

class SolarEventAdapter(
    private val events: List<String>,
    private val onEventSelected: (Int) -> Unit
) : RecyclerView.Adapter<SolarEventAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.eventCard)
        val text: TextView = itemView.findViewById(R.id.eventText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solar_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.text.text = event
        
        holder.card.setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            com.rooster.rooster.util.AnimationHelper.scaleWithBounce(it)
            onEventSelected(position)
        }
    }

    override fun getItemCount() = events.size
}
