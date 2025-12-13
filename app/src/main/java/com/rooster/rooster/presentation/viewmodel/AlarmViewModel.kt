package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for individual alarm details and alarm activity
 */
@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {
    
    private val _currentAlarm = MutableStateFlow<Alarm?>(null)
    val currentAlarm: StateFlow<Alarm?> = _currentAlarm
    
    private val _isSnoozing = MutableStateFlow(false)
    val isSnoozing: StateFlow<Boolean> = _isSnoozing
    
    /**
     * Load alarm by ID
     */
    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentAlarm.value = alarmRepository.getAlarmById(alarmId)
        }
    }
    
    /**
     * Get alarm by ID as LiveData
     */
    fun getAlarmLiveData(alarmId: Long): LiveData<Alarm?> {
        return alarmRepository.getAlarmByIdFlow(alarmId)
            .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
    
    /**
     * Snooze the alarm for specified minutes
     */
    fun snoozeAlarm(alarmId: Long, snoozeMinutes: Int = 10) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSnoozing.value = true
            val alarm = alarmRepository.getAlarmById(alarmId)
            
            if (alarm != null) {
                val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
                alarmRepository.updateCalculatedTime(alarmId, snoozeTime)
            }
            
            _isSnoozing.value = false
        }
    }
    
    /**
     * Dismiss the alarm (disable if not repeating)
     */
    fun dismissAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val hasRepeatDays = alarm.monday || alarm.tuesday || alarm.wednesday ||
                    alarm.thursday || alarm.friday || alarm.saturday || alarm.sunday
            
            if (!hasRepeatDays) {
                // Disable one-time alarms after they trigger
                alarmRepository.updateAlarmEnabled(alarm.id, false)
            }
        }
    }
}
