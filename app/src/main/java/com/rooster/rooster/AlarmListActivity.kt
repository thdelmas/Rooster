package com.rooster.rooster

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.AnimationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmListActivity : AppCompatActivity() {

    private val viewModel: AlarmListViewModel by viewModels()
    private lateinit var alarmAdapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)
        setupRecyclerView()
        linkButtons()
        observeAlarms()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmListView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        alarmAdapter = AlarmAdapter(emptyList(), viewModel)
        recyclerView.adapter = alarmAdapter
    }

    private fun observeAlarms() {
        viewModel.allAlarms.observe(this) { alarms ->
            Log.d("AlarmListActivity", "Alarms updated: ${alarms.size} alarms")
            updateAlarmList(alarms)
            updateEmptyState(alarms.isEmpty())
        }
    }

    private fun linkButtons() {
        val addAlarmButton = findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            HapticFeedbackHelper.performSuccessFeedback(this)
            AnimationHelper.scaleWithBounce(it)
            createNewAlarm()
        }
        
        // Empty state button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.emptyStateButton)?.setOnClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            HapticFeedbackHelper.performSuccessFeedback(this)
            AnimationHelper.scaleWithBounce(it)
            createNewAlarm()
        }
    }
    
    private fun createNewAlarm() {
        val alarm = AlarmCreation("Alarm", false, "At", "Default", "Pick Time", "Pick Time", 0, 0, 0)
        viewModel.insertAlarm(alarm) { alarmId ->
            // Open editor with the new alarm ID
            val intent = android.content.Intent(this, AlarmEditorActivity::class.java)
            intent.putExtra("alarm_id", alarmId)
            startActivity(intent)
        }
    }

    private fun updateAlarmList(alarms: List<Alarm>) {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmListView)
        // Use ImprovedAlarmAdapter with ViewModel instead of AlarmDbHelper
        recyclerView.adapter = ImprovedAlarmAdapter(alarms, viewModel)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val emptyStateCard = findViewById<android.view.View>(R.id.emptyStateCard)
        if (isEmpty) {
            emptyStateCard?.let {
                it.visibility = android.view.View.VISIBLE
                AnimationHelper.fadeIn(it, 300)
            }
        } else {
            emptyStateCard?.let {
                AnimationHelper.fadeOut(it, 200) {
                    it.visibility = android.view.View.GONE
                }
            }
        }
    }
}