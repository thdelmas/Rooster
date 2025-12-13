package com.rooster.rooster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "label")
    val label: String,
    
    @ColumnInfo(name = "enabled")
    val enabled: Boolean,
    
    @ColumnInfo(name = "mode")
    val mode: String,
    
    @ColumnInfo(name = "ringtoneUri")
    val ringtoneUri: String,
    
    @ColumnInfo(name = "relative1")
    val relative1: String,
    
    @ColumnInfo(name = "relative2")
    val relative2: String,
    
    @ColumnInfo(name = "time1")
    val time1: Long,
    
    @ColumnInfo(name = "time2")
    val time2: Long,
    
    @ColumnInfo(name = "calculated_time")
    val calculatedTime: Long,
    
    @ColumnInfo(name = "monday")
    val monday: Boolean,
    
    @ColumnInfo(name = "tuesday")
    val tuesday: Boolean,
    
    @ColumnInfo(name = "wednesday")
    val wednesday: Boolean,
    
    @ColumnInfo(name = "thursday")
    val thursday: Boolean,
    
    @ColumnInfo(name = "friday")
    val friday: Boolean,
    
    @ColumnInfo(name = "saturday")
    val saturday: Boolean,
    
    @ColumnInfo(name = "sunday")
    val sunday: Boolean,
    
    @ColumnInfo(name = "vibrate", defaultValue = "1")
    val vibrate: Boolean = true,
    
    @ColumnInfo(name = "snooze_enabled", defaultValue = "1")
    val snoozeEnabled: Boolean = true,
    
    @ColumnInfo(name = "snooze_duration", defaultValue = "10")
    val snoozeDuration: Int = 10,
    
    @ColumnInfo(name = "snooze_count", defaultValue = "3")
    val snoozeCount: Int = 3,
    
    @ColumnInfo(name = "volume", defaultValue = "80")
    val volume: Int = 80,
    
    @ColumnInfo(name = "gradual_volume", defaultValue = "0")
    val gradualVolume: Boolean = false
)
