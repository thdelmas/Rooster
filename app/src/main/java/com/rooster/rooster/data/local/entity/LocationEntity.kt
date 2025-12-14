package com.rooster.rooster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for location data
 * Stores device location coordinates for astronomy calculations
 */
@Entity(tableName = "location_data")
data class LocationEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for current location
    val latitude: Float,
    val longitude: Float,
    val altitude: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)
