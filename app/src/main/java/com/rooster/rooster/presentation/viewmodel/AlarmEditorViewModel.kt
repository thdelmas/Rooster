package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.data.repository.AstronomyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AlarmEditorActivity
 * Handles astronomy data access through repository
 */
@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val astronomyRepository: AstronomyRepository
) : ViewModel() {
    
    private val _astronomyData = MutableStateFlow<AstronomyDataEntity?>(null)
    val astronomyData: StateFlow<AstronomyDataEntity?> = _astronomyData
    
    /**
     * Get astronomy data
     */
    fun getAstronomyData(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _astronomyData.value = astronomyRepository.getAstronomyData(forceRefresh)
        }
    }
}
