package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 * Handles location data access through repository
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    
    /**
     * Save location to repository
     */
    fun saveLocation(latitude: Float, longitude: Float, altitude: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.saveLocation(latitude, longitude, altitude)
        }
    }
}
