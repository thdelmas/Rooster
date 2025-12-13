package com.rooster.rooster.data.repository

import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Alarm data operations
 */
interface AlarmRepository {
    /**
     * Get all alarms as a Flow for reactive updates
     */
    fun getAllAlarmsFlow(): Flow<List<Alarm>>
    
    /**
     * Get all alarms
     */
    suspend fun getAllAlarms(): List<Alarm>
    
    /**
     * Get a specific alarm by ID
     */
    suspend fun getAlarmById(id: Long): Alarm?
    
    /**
     * Get a specific alarm by ID as a Flow
     */
    fun getAlarmByIdFlow(id: Long): Flow<Alarm?>
    
    /**
     * Get all enabled alarms
     */
    suspend fun getEnabledAlarms(): List<Alarm>
    
    /**
     * Get all enabled alarms as a Flow
     */
    fun getEnabledAlarmsFlow(): Flow<List<Alarm>>
    
    /**
     * Insert a new alarm
     * @return the ID of the inserted alarm
     */
    suspend fun insertAlarm(alarm: AlarmCreation): Long
    
    /**
     * Update an existing alarm
     */
    suspend fun updateAlarm(alarm: Alarm)
    
    /**
     * Delete an alarm
     */
    suspend fun deleteAlarm(alarm: Alarm)
    
    /**
     * Delete an alarm by ID
     */
    suspend fun deleteAlarmById(id: Long)
    
    /**
     * Delete all alarms
     */
    suspend fun deleteAllAlarms()
    
    /**
     * Update alarm enabled status
     */
    suspend fun updateAlarmEnabled(id: Long, enabled: Boolean)
    
    /**
     * Update alarm calculated time
     */
    suspend fun updateCalculatedTime(id: Long, calculatedTime: Long)
}
