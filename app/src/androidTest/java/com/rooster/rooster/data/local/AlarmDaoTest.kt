package com.rooster.rooster.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rooster.rooster.data.local.dao.AlarmDao
import com.rooster.rooster.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Integration tests for AlarmDao
 */
@RunWith(AndroidJUnit4::class)
class AlarmDaoTest {
    
    private lateinit var database: AlarmDatabase
    private lateinit var alarmDao: AlarmDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AlarmDatabase::class.java
        ).build()
        alarmDao = database.alarmDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndGetAlarm() = runBlocking {
        // Given
        val alarm = createTestAlarm(label = "Morning Alarm")
        
        // When
        val id = alarmDao.insertAlarm(alarm)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertNotNull(retrieved)
        assertEquals("Morning Alarm", retrieved?.label)
        assertEquals(alarm.mode, retrieved?.mode)
    }
    
    @Test
    fun getAllAlarms() = runBlocking {
        // Given
        val alarm1 = createTestAlarm(label = "Alarm 1")
        val alarm2 = createTestAlarm(label = "Alarm 2")
        alarmDao.insertAlarm(alarm1)
        alarmDao.insertAlarm(alarm2)
        
        // When
        val alarms = alarmDao.getAllAlarms()
        
        // Then
        assertEquals(2, alarms.size)
    }
    
    @Test
    fun updateAlarm() = runBlocking {
        // Given
        val alarm = createTestAlarm(label = "Original")
        val id = alarmDao.insertAlarm(alarm)
        
        // When
        val updated = alarm.copy(id = id, label = "Updated")
        alarmDao.updateAlarm(updated)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertEquals("Updated", retrieved?.label)
    }
    
    @Test
    fun deleteAlarm() = runBlocking {
        // Given
        val alarm = createTestAlarm()
        val id = alarmDao.insertAlarm(alarm)
        
        // When
        alarmDao.deleteAlarmById(id)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun getEnabledAlarms() = runBlocking {
        // Given
        val enabledAlarm = createTestAlarm(enabled = true)
        val disabledAlarm = createTestAlarm(enabled = false)
        alarmDao.insertAlarm(enabledAlarm)
        alarmDao.insertAlarm(disabledAlarm)
        
        // When
        val enabledAlarms = alarmDao.getEnabledAlarms()
        
        // Then
        assertEquals(1, enabledAlarms.size)
        assertTrue(enabledAlarms[0].enabled)
    }
    
    @Test
    fun updateAlarmEnabled() = runBlocking {
        // Given
        val alarm = createTestAlarm(enabled = true)
        val id = alarmDao.insertAlarm(alarm)
        
        // When
        alarmDao.updateAlarmEnabled(id, false)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertFalse(retrieved!!.enabled)
    }
    
    @Test
    fun updateCalculatedTime() = runBlocking {
        // Given
        val alarm = createTestAlarm(calculatedTime = 1000L)
        val id = alarmDao.insertAlarm(alarm)
        val newTime = 2000L
        
        // When
        alarmDao.updateCalculatedTime(id, newTime)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertEquals(newTime, retrieved?.calculatedTime)
    }
    
    @Test
    fun getAllAlarmsFlow_emitsUpdates() = runBlocking {
        // Given
        val alarm = createTestAlarm()
        
        // When
        val initialAlarms = alarmDao.getAllAlarmsFlow().first()
        alarmDao.insertAlarm(alarm)
        val updatedAlarms = alarmDao.getAllAlarmsFlow().first()
        
        // Then
        assertEquals(0, initialAlarms.size)
        assertEquals(1, updatedAlarms.size)
    }
    
    @Test
    fun deleteAllAlarms() = runBlocking {
        // Given
        alarmDao.insertAlarm(createTestAlarm())
        alarmDao.insertAlarm(createTestAlarm())
        
        // When
        alarmDao.deleteAllAlarms()
        val alarms = alarmDao.getAllAlarms()
        
        // Then
        assertEquals(0, alarms.size)
    }
    
    @Test
    fun replaceAlarmOnConflict() = runBlocking {
        // Given
        val alarm = createTestAlarm(label = "Original")
        val id = alarmDao.insertAlarm(alarm)
        
        // When - Insert with same ID (should replace)
        val replacement = alarm.copy(id = id, label = "Replacement")
        alarmDao.insertAlarm(replacement)
        val retrieved = alarmDao.getAlarmById(id)
        
        // Then
        assertEquals("Replacement", retrieved?.label)
        assertEquals(1, alarmDao.getAllAlarms().size) // Should still be only 1
    }
    
    private fun createTestAlarm(
        label: String = "Test Alarm",
        enabled: Boolean = true,
        calculatedTime: Long = System.currentTimeMillis()
    ): AlarmEntity {
        return AlarmEntity(
            id = 0,
            label = label,
            enabled = enabled,
            mode = "At",
            ringtoneUri = "Default",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = calculatedTime,
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
    }
}
