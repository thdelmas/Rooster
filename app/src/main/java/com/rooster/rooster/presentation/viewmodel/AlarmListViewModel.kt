package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.domain.usecase.CalculateAlarmTimeUseCase
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import com.rooster.rooster.util.ErrorMessageMapper
import com.rooster.rooster.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val calculateAlarmTimeUseCase: CalculateAlarmTimeUseCase,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase
) : ViewModel() {
    
    // Expose alarms as LiveData for easy observation in Activities
    val allAlarms: LiveData<List<Alarm>> = alarmRepository.getAllAlarmsFlow()
        .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    /**
     * Insert a new alarm
     */
    fun insertAlarm(alarm: AlarmCreation, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val id = alarmRepository.insertAlarm(alarm)
                onSuccess(id)
                _error.value = null
            } catch (e: Exception) {
                val friendlyError = ErrorMessageMapper.getContextualError("save", e)
                _error.value = friendlyError.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update an existing alarm
     */
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                // Recalculate alarm time (will fetch fresh astronomy data if needed)
                val updatedAlarm = alarm.copy(
                    calculatedTime = calculateAlarmTimeUseCase.execute(alarm)
                )
                alarmRepository.updateAlarm(updatedAlarm)
                
                // Cancel or schedule the alarm based on enabled status
                if (!updatedAlarm.enabled) {
                    // Cancel the alarm from AlarmManager when disabled
                    val cancelResult = scheduleAlarmUseCase.cancelAlarm(updatedAlarm)
                    cancelResult.fold(
                        onSuccess = {
                            Logger.i("AlarmListViewModel", "Alarm ${updatedAlarm.id} cancelled successfully after being disabled")
                        },
                        onFailure = { e ->
                            Logger.e("AlarmListViewModel", "Error cancelling alarm ${updatedAlarm.id} after being disabled", e)
                        }
                    )
                } else {
                    // Schedule the alarm when enabled
                    val scheduleResult = scheduleAlarmUseCase.scheduleAlarm(updatedAlarm)
                    scheduleResult.fold(
                        onSuccess = {
                            Logger.i("AlarmListViewModel", "Alarm ${updatedAlarm.id} scheduled successfully after being enabled")
                        },
                        onFailure = { e ->
                            Logger.e("AlarmListViewModel", "Error scheduling alarm ${updatedAlarm.id} after being enabled", e)
                            // Don't set error for disabled alarms being scheduled (this is expected)
                            if (e !is IllegalArgumentException || e.message != "Alarm is disabled") {
                                // Only log unexpected errors
                            }
                        }
                    )
                }
                
                _error.value = null
            } catch (e: Exception) {
                val friendlyError = ErrorMessageMapper.getContextualError("save", e)
                _error.value = friendlyError.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete an alarm
     */
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                // Cancel the alarm from AlarmManager before deleting from database
                val cancelResult = scheduleAlarmUseCase.cancelAlarm(alarm)
                cancelResult.fold(
                    onSuccess = {
                        Logger.i("AlarmListViewModel", "Alarm ${alarm.id} cancelled successfully before deletion")
                    },
                    onFailure = { e ->
                        Logger.e("AlarmListViewModel", "Error cancelling alarm ${alarm.id} before deletion", e)
                    }
                )
                alarmRepository.deleteAlarm(alarm)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to delete alarm: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle alarm enabled status
     */
    fun toggleAlarmEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                alarmRepository.updateAlarmEnabled(alarmId, enabled)
                
                // Cancel or schedule the alarm based on enabled status
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    if (!enabled) {
                        // Cancel the alarm from AlarmManager when disabled
                        val cancelResult = scheduleAlarmUseCase.cancelAlarm(alarm)
                        cancelResult.fold(
                            onSuccess = {
                                Logger.i("AlarmListViewModel", "Alarm $alarmId cancelled successfully after being disabled")
                            },
                            onFailure = { e ->
                                Logger.e("AlarmListViewModel", "Error cancelling alarm $alarmId after being disabled", e)
                            }
                        )
                    } else {
                        // Schedule the alarm when enabled
                        val scheduleResult = scheduleAlarmUseCase.scheduleAlarm(alarm)
                        scheduleResult.fold(
                            onSuccess = {
                                Logger.i("AlarmListViewModel", "Alarm $alarmId scheduled successfully after being enabled")
                            },
                            onFailure = { e ->
                                Logger.e("AlarmListViewModel", "Error scheduling alarm $alarmId after being enabled", e)
                                // Don't set error for disabled alarms being scheduled (this is expected)
                                if (e !is IllegalArgumentException || e.message != "Alarm is disabled") {
                                    // Only log unexpected errors
                                }
                            }
                        )
                    }
                }
                
                _error.value = null
            } catch (e: Exception) {
                val friendlyError = ErrorMessageMapper.mapError(e)
                _error.value = friendlyError.message
            }
        }
    }
    
    /**
     * Get the next alarm to trigger
     */
    suspend fun getNextAlarm(): Alarm? {
        return try {
            val enabledAlarms = alarmRepository.getEnabledAlarms()
            val currentTime = System.currentTimeMillis()
            
            enabledAlarms
                .filter { it.calculatedTime > currentTime }
                .minByOrNull { it.calculatedTime }
        } catch (e: Exception) {
            val friendlyError = ErrorMessageMapper.mapError(e)
            _error.value = friendlyError.message
            null
        }
    }
}
