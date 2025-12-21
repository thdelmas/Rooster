package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.domain.usecase.CalculateAlarmTimeUseCase
import com.rooster.rooster.util.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val calculateAlarmTimeUseCase: CalculateAlarmTimeUseCase
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
