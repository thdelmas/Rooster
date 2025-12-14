package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.ViewModel
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
 * ViewModel for RingtoneActivity
 * Handles alarm ringtone updates through repository
 */
@HiltViewModel
class RingtoneViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {
    
    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult
    
    /**
     * Get alarm by ID
     */
    suspend fun getAlarm(alarmId: Long): Alarm? {
        return alarmRepository.getAlarmById(alarmId)
    }
    
    /**
     * Update alarm ringtone
     */
    fun updateAlarmRingtone(alarmId: Long, ringtoneUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    val updatedAlarm = alarm.copy(ringtoneUri = ringtoneUri)
                    alarmRepository.updateAlarm(updatedAlarm)
                    _updateResult.value = UpdateResult.Success
                } else {
                    _updateResult.value = UpdateResult.Error("Alarm not found")
                }
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset update result
     */
    fun resetUpdateResult() {
        _updateResult.value = null
    }
    
    sealed class UpdateResult {
        object Success : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}
