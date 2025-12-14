package com.rooster.rooster.data.repository

import com.rooster.rooster.Alarm
import com.rooster.rooster.util.Logger
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.local.dao.AlarmDao
import com.rooster.rooster.data.mapper.toAlarm
import com.rooster.rooster.data.mapper.toEntity
import com.rooster.rooster.util.ValidationHelper
import com.rooster.rooster.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlarmRepository using Room database with validation
 */
@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {
    
    companion object {
        private const val TAG = "AlarmRepositoryImpl"
    }
    
    override fun getAllAlarmsFlow(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarmsFlow().map { entities ->
            entities.map { it.toAlarm() }
        }
    }
    
    override suspend fun getAllAlarms(): List<Alarm> {
        return alarmDao.getAllAlarms().map { it.toAlarm() }
    }
    
    override suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)?.toAlarm()
    }
    
    override fun getAlarmByIdFlow(id: Long): Flow<Alarm?> {
        return alarmDao.getAlarmByIdFlow(id).map { it?.toAlarm() }
    }
    
    override suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms().map { it.toAlarm() }
    }
    
    override fun getEnabledAlarmsFlow(): Flow<List<Alarm>> {
        return alarmDao.getEnabledAlarmsFlow().map { entities ->
            entities.map { it.toAlarm() }
        }
    }
    
    override suspend fun insertAlarm(alarm: AlarmCreation): Long {
        // Validate alarm before inserting
        val validationResult = ValidationHelper.validateAlarm(alarm)
        if (validationResult.isError()) {
            val errorMsg = validationResult.getErrorMessage()
            Logger.e(TAG, "Alarm validation failed: $errorMsg")
            throw IllegalArgumentException(errorMsg)
        }
        
        if (validationResult.isWarning()) {
            Logger.w(TAG, "Alarm validation warning: ${validationResult.getErrorMessage()}")
        }
        
        return alarmDao.insertAlarm(alarm.toEntity())
    }
    
    override suspend fun updateAlarm(alarm: Alarm) {
        // Validate alarm before updating
        val validationResult = ValidationHelper.validateExistingAlarm(alarm)
        if (validationResult.isError()) {
            val errorMsg = validationResult.getErrorMessage()
            Logger.e(TAG, "Alarm validation failed: $errorMsg")
            throw IllegalArgumentException(errorMsg)
        }
        
        if (validationResult.isWarning()) {
            Logger.w(TAG, "Alarm validation warning: ${validationResult.getErrorMessage()}")
        }
        
        alarmDao.updateAlarm(alarm.toEntity())
    }
    
    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm.toEntity())
    }
    
    override suspend fun deleteAlarmById(id: Long) {
        alarmDao.deleteAlarmById(id)
    }
    
    override suspend fun deleteAllAlarms() {
        alarmDao.deleteAllAlarms()
    }
    
    override suspend fun updateAlarmEnabled(id: Long, enabled: Boolean) {
        alarmDao.updateAlarmEnabled(id, enabled)
    }
    
    override suspend fun updateCalculatedTime(id: Long, calculatedTime: Long) {
        alarmDao.updateCalculatedTime(id, calculatedTime)
    }
}
