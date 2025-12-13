package com.rooster.rooster.data.mapper

import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.local.entity.AlarmEntity

/**
 * Mapper functions to convert between domain models and Room entities
 */

fun AlarmEntity.toAlarm(): Alarm {
    return Alarm(
        id = id,
        label = label,
        enabled = enabled,
        mode = mode,
        ringtoneUri = ringtoneUri,
        relative1 = relative1,
        relative2 = relative2,
        time1 = time1,
        time2 = time2,
        calculatedTime = calculatedTime,
        monday = monday,
        tuesday = tuesday,
        wednesday = wednesday,
        thursday = thursday,
        friday = friday,
        saturday = saturday,
        sunday = sunday,
        vibrate = vibrate,
        snoozeEnabled = snoozeEnabled,
        snoozeDuration = snoozeDuration,
        snoozeCount = snoozeCount,
        volume = volume,
        gradualVolume = gradualVolume
    )
}

fun Alarm.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        label = label,
        enabled = enabled,
        mode = mode,
        ringtoneUri = ringtoneUri,
        relative1 = relative1,
        relative2 = relative2,
        time1 = time1,
        time2 = time2,
        calculatedTime = calculatedTime,
        monday = monday,
        tuesday = tuesday,
        wednesday = wednesday,
        thursday = thursday,
        friday = friday,
        saturday = saturday,
        sunday = sunday,
        vibrate = vibrate,
        snoozeEnabled = snoozeEnabled,
        snoozeDuration = snoozeDuration,
        snoozeCount = snoozeCount,
        volume = volume,
        gradualVolume = gradualVolume
    )
}

fun AlarmCreation.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = 0, // Auto-generated
        label = label,
        enabled = enabled,
        mode = mode,
        ringtoneUri = ringtoneUri,
        relative1 = relative1,
        relative2 = relative2,
        time1 = time1,
        time2 = time2,
        calculatedTime = calculatedTime,
        monday = false,
        tuesday = false,
        wednesday = false,
        thursday = false,
        friday = false,
        saturday = false,
        sunday = false,
        vibrate = true,
        snoozeEnabled = true,
        snoozeDuration = 10,
        snoozeCount = 3,
        volume = 80,
        gradualVolume = false
    )
}
