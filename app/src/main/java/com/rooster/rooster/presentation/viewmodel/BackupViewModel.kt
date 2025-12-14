package com.rooster.rooster.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.data.backup.BackupManager
import com.rooster.rooster.util.ErrorMessageMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {
    
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState
    
    /**
     * Export alarms to a file
     */
    fun exportAlarms(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            
            val result = backupManager.exportToFile(context, uri)
            
            _backupState.value = if (result.isSuccess) {
                BackupState.Success(result.getOrNull() ?: "Export successful")
            } else {
                val exception = result.exceptionOrNull()
                val friendlyError = if (exception != null) {
                    ErrorMessageMapper.getContextualError("export", exception)
                } else {
                    ErrorMessageMapper.UserFriendlyError("Unable to export alarms", "Please try again")
                }
                BackupState.Error(friendlyError.message)
            }
        }
    }
    
    /**
     * Import alarms from a file
     */
    fun importAlarms(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            
            val result = backupManager.importFromFile(context, uri)
            
            _backupState.value = if (result.isSuccess) {
                BackupState.Success(result.getOrNull() ?: "Import successful")
            } else {
                val exception = result.exceptionOrNull()
                val friendlyError = if (exception != null) {
                    ErrorMessageMapper.getContextualError("import", exception)
                } else {
                    ErrorMessageMapper.UserFriendlyError("Unable to import alarms", "Please try again")
                }
                BackupState.Error(friendlyError.message)
            }
        }
    }
    
    /**
     * Reset state to idle
     */
    fun resetState() {
        _backupState.value = BackupState.Idle
    }
    
    sealed class BackupState {
        object Idle : BackupState()
        object Loading : BackupState()
        data class Success(val message: String) : BackupState()
        data class Error(val message: String) : BackupState()
    }
}
