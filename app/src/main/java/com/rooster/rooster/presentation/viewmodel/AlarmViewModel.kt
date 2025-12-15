package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.Alarm
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import com.rooster.rooster.util.Logger
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
    private val alarmRepository: AlarmRepository,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase
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
     * Cancels the AlarmManager pending intent to prevent the alarm from restarting
     */
    fun dismissAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel the AlarmManager pending intent to prevent the alarm from restarting
            // This is important because AlarmclockReceiver schedules the next alarm immediately
            // when an alarm fires, and we need to cancel it when the user dismisses
            val cancelResult = scheduleAlarmUseCase.cancelAlarm(alarm)
            cancelResult.fold(
                onSuccess = {
                    Logger.i("AlarmViewModel", "Alarm ${alarm.id} cancelled successfully")
                },
                onFailure = { e ->
                    Logger.e("AlarmViewModel", "Error cancelling alarm ${alarm.id}", e)
                }
            )
            
            val hasRepeatDays = alarm.monday || alarm.tuesday || alarm.wednesday ||
                    alarm.thursday || alarm.friday || alarm.saturday || alarm.sunday
            
            if (!hasRepeatDays) {
                // Disable one-time alarms after they trigger
                alarmRepository.updateAlarmEnabled(alarm.id, false)
            } else {
                // For repeating alarms, reschedule the next occurrence properly
                // This ensures the alarm will fire at the correct next time
                val scheduleResult = scheduleAlarmUseCase.scheduleAlarm(alarm)
                scheduleResult.fold(
                    onSuccess = {
                        Logger.i("AlarmViewModel", "Repeating alarm ${alarm.id} rescheduled for next occurrence")
                    },
                    onFailure = { e ->
                        Logger.e("AlarmViewModel", "Error rescheduling repeating alarm ${alarm.id}", e)
                    }
                )
            }
        }
    }
}
