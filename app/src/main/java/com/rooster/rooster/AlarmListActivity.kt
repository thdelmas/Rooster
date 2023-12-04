package com.rooster.rooster

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlarmListActivity() : ComponentActivity() {

    val alarmDbHelper = AlarmDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)
        linkButtons()
        fillAlarmList(alarmDbHelper)
    }


    private fun linkButtons() {
        val addAlarmButton = findViewById<Button>(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener {
            val alarm = AlarmCreation("Alarm",false, "At", "Pick Time", "Pick Time", 0, 0, 0)
            alarmDbHelper.insertAlarm(alarm)
            reloadAlarmList()
        }
    }

    private fun fillAlarmList(alarmDbHelper: AlarmDbHelper) {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmListView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val alarms = this.alarmDbHelper.getAllAlarms()
        recyclerView.adapter = AlarmAdapter(alarms, alarmDbHelper)
    }

    fun reloadAlarmList() {
        Log.e("Redraw", "Redraw")
        val recyclerView = findViewById<RecyclerView>(R.id.alarmListView)
        recyclerView.invalidate()
        fillAlarmList(alarmDbHelper)
    }

    override fun onResume() {
        super.onResume()
        reloadAlarmList()
    }
}