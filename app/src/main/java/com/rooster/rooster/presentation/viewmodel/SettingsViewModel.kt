package com.rooster.rooster.presentation.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.data.local.entity.LocationEntity
import com.rooster.rooster.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity
 * Handles location data access through repository
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    
    /**
     * Get location as LiveData
     */
    fun getLocationLiveData(): LiveData<LocationEntity?> {
        return locationRepository.getLocationFlow()
            .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
    
    /**
     * Get location (suspend function)
     */
    suspend fun getLocation(): LocationEntity? {
        return locationRepository.getLocation()
    }
    
    /**
     * Save location to repository
     */
    fun saveLocation(location: Location) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.saveLocation(location)
        }
    }
}
