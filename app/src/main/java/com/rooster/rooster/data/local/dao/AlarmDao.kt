package com.rooster.rooster.data.local.dao

import androidx.room.*
import com.rooster.rooster.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY calculated_time ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>
    
    @Query("SELECT * FROM alarms ORDER BY calculated_time ASC")
    suspend fun getAllAlarms(): List<AlarmEntity>
    
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?
    
    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?>
    
    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY calculated_time ASC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>
    
    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY calculated_time ASC")
    fun getEnabledAlarmsFlow(): Flow<List<AlarmEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long
    
    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)
    
    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)
    
    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)
    
    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
    
    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun updateAlarmEnabled(id: Long, enabled: Boolean)
    
    @Query("UPDATE alarms SET calculated_time = :calculatedTime WHERE id = :id")
    suspend fun updateCalculatedTime(id: Long, calculatedTime: Long)
}
