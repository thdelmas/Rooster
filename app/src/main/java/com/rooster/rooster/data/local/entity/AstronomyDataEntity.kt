package com.rooster.rooster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for astronomy data
 */
@Entity(tableName = "astronomy_data")
data class AstronomyDataEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for current location's data
    val latitude: Float,
    val longitude: Float,
    val sunrise: Long,
    val sunset: Long,
    val solarNoon: Long,
    val civilDawn: Long,
    val civilDusk: Long,
    val nauticalDawn: Long,
    val nauticalDusk: Long,
    val astroDawn: Long,
    val astroDusk: Long,
    val lastUpdated: Long,
    val dayLength: Long
)
