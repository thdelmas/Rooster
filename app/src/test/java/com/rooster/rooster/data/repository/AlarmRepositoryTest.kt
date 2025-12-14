package com.rooster.rooster.data.repository

import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.local.dao.AlarmDao
import com.rooster.rooster.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Unit tests for AlarmRepositoryImpl
 */
class AlarmRepositoryTest {
    
    private lateinit var alarmDao: AlarmDao
    private lateinit var alarmRepository: AlarmRepository
    
    @Before
    fun setup() {
        alarmDao = mock()
        alarmRepository = AlarmRepositoryImpl(alarmDao)
    }
    
    @Test
    fun `test getAllAlarms returns mapped alarms`(): Unit = runBlocking {
        // Given
        val entities = listOf(
            createTestEntity(1),
            createTestEntity(2)
        )
        whenever(alarmDao.getAllAlarms()).thenReturn(entities)
        
        // When
        val result = alarmRepository.getAllAlarms()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
        verify(alarmDao).getAllAlarms()
    }
    
    @Test
    fun `test getAlarmById returns alarm when exists`(): Unit = runBlocking {
        // Given
        val entity = createTestEntity(1)
        whenever(alarmDao.getAlarmById(1)).thenReturn(entity)
        
        // When
        val result = alarmRepository.getAlarmById(1)
        
        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id)
        verify(alarmDao).getAlarmById(1)
    }
    
    @Test
    fun `test getAlarmById returns null when not exists`(): Unit = runBlocking {
        // Given
        whenever(alarmDao.getAlarmById(999)).thenReturn(null)
        
        // When
        val result = alarmRepository.getAlarmById(999)
        
        // Then
        assertNull(result)
        verify(alarmDao).getAlarmById(999)
    }
    
    @Test
    fun `test insertAlarm calls dao insert`(): Unit = runBlocking {
        // Given
        val alarm = createTestAlarmCreation()
        whenever(alarmDao.insertAlarm(any())).thenReturn(1L)
        
        // When
        val result = alarmRepository.insertAlarm(alarm)
        
        // Then
        assertEquals(1L, result)
        verify(alarmDao).insertAlarm(any())
    }
    
    @Test
    fun `test updateAlarm calls dao update`(): Unit = runBlocking {
        // Given
        val alarm = createTestAlarm(1)
        
        // When
        alarmRepository.updateAlarm(alarm)
        
        // Then
        verify(alarmDao).updateAlarm(any())
    }
    
    @Test
    fun `test deleteAlarm calls dao delete`(): Unit = runBlocking {
        // Given
        val alarm = createTestAlarm(1)
        
        // When
        alarmRepository.deleteAlarm(alarm)
        
        // Then
        verify(alarmDao).deleteAlarm(any())
    }
    
    @Test
    fun `test deleteAlarmById calls dao delete by id`(): Unit = runBlocking {
        // Given
        val alarmId = 1L
        
        // When
        alarmRepository.deleteAlarmById(alarmId)
        
        // Then
        verify(alarmDao).deleteAlarmById(alarmId)
    }
    
    @Test
    fun `test getEnabledAlarms returns only enabled alarms`(): Unit = runBlocking {
        // Given
        val entities = listOf(
            createTestEntity(1, enabled = true),
            createTestEntity(2, enabled = true)
        )
        whenever(alarmDao.getEnabledAlarms()).thenReturn(entities)
        
        // When
        val result = alarmRepository.getEnabledAlarms()
        
        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.enabled })
        verify(alarmDao).getEnabledAlarms()
    }
    
    @Test
    fun `test getAllAlarmsFlow emits updates`(): Unit = runBlocking {
        // Given
        val entities = listOf(createTestEntity(1))
        whenever(alarmDao.getAllAlarmsFlow()).thenReturn(flowOf(entities))
        
        // When
        val result = alarmRepository.getAllAlarmsFlow().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }
    
    @Test
    fun `test updateAlarmEnabled calls dao update enabled`(): Unit = runBlocking {
        // When
        alarmRepository.updateAlarmEnabled(1L, true)
        
        // Then
        verify(alarmDao).updateAlarmEnabled(1L, true)
    }
    
    @Test
    fun `test updateCalculatedTime calls dao update calculated time`(): Unit = runBlocking {
        // Given
        val alarmId = 1L
        val calculatedTime = System.currentTimeMillis()
        
        // When
        alarmRepository.updateCalculatedTime(alarmId, calculatedTime)
        
        // Then
        verify(alarmDao).updateCalculatedTime(alarmId, calculatedTime)
    }
    
    private fun createTestEntity(
        id: Long,
        enabled: Boolean = true
    ): AlarmEntity {
        return AlarmEntity(
            id = id,
            label = "Test Alarm $id",
            enabled = enabled,
            mode = "At",
            ringtoneUri = "Default",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = System.currentTimeMillis(),
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
    }
    
    private fun createTestAlarm(id: Long): Alarm {
        return Alarm(
            id = id,
            label = "Test Alarm",
            enabled = true,
            mode = "At",
            ringtoneUri = "Default",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = System.currentTimeMillis(),
            monday = true,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )
    }
    
    private fun createTestAlarmCreation(): AlarmCreation {
        return AlarmCreation(
            label = "Test Alarm",
            enabled = true,
            mode = "At",
            ringtoneUri = "Default",
            relative1 = "Pick Time",
            relative2 = "Pick Time",
            time1 = System.currentTimeMillis(),
            time2 = 0,
            calculatedTime = System.currentTimeMillis()
        )
    }
}
