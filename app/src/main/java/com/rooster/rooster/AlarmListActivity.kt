package com.rooster.rooster

import android.os.Bundle
import android.util.Log
import android.widget.Button
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
    @Deprecated("Use repository through ViewModel instead")
    val alarmDbHelper = AlarmDbHelper(this)

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
        alarmAdapter = AlarmAdapter(emptyList(), alarmDbHelper)
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
        val addAlarmButton = findViewById<Button>(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener {
            HapticFeedbackHelper.performHeavyClick(it)
            HapticFeedbackHelper.performSuccessFeedback(this)
            AnimationHelper.scaleWithBounce(it)
            // Create alarm and open editor
            val alarm = AlarmCreation("Alarm", false, "At", "Default", "Pick Time", "Pick Time", 0, 0, 0)
            val alarmId = viewModel.insertAlarm(alarm)
            // Wait a moment for insertion, then open editor
            it.postDelayed({
                val allAlarms = alarmDbHelper.getAllAlarms()
                val newAlarm = allAlarms.maxByOrNull { it.id }
                newAlarm?.let { alarm ->
                    val intent = android.content.Intent(this, AlarmEditorActivity::class.java)
                    intent.putExtra("alarm_id", alarm.id)
                    startActivity(intent)
                }
            }, 200)
        }
    }

    private fun updateAlarmList(alarms: List<Alarm>) {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmListView)
        recyclerView.adapter = ImprovedAlarmAdapter(alarms, alarmDbHelper)
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

    @Deprecated("No longer needed with LiveData observation")
    fun reloadAlarmList() {
        Log.e("Redraw", "Redraw")
        // This is now handled automatically by LiveData observation
    }
}