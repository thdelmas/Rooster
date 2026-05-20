package com.rooster.rooster.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rooster.rooster.Alarm
import com.rooster.rooster.AlarmCreation
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.data.repository.AstronomyRepository
import com.rooster.rooster.domain.usecase.CalculateAlarmTimeUseCase
import com.rooster.rooster.domain.usecase.ScheduleAlarmUseCase
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.Logger
import com.rooster.rooster.util.ValidationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AlarmEditorUiState(
    val alarmId: Long = -1L,
    val isNew: Boolean = true,
    val label: String = "",
    val mode: String = "sun",
    val sunTimingMode: String = AppConstants.ALARM_MODE_AT,
    val solarEvent1: String = AppConstants.SOLAR_EVENT_SUNRISE,
    val solarEvent2: String = AppConstants.SOLAR_EVENT_SUNSET,
    val offsetMinutes: Int = 30,
    val selectedTime: Long = 0L,
    val days: Map<String, Boolean> = DEFAULT_DAYS,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDuration: Int = 10,
    val snoozeCount: Int = 3,
    val volume: Int = 80,
    val gradualVolume: Boolean = false,
    val ringtoneUri: String = AppConstants.DEFAULT_RINGTONE_URI,
    val calculatedTime: Long? = null,
    val astronomyData: AstronomyDataEntity? = null,
    val errorMessage: String? = null,
) {
    companion object {
        val DEFAULT_DAYS: Map<String, Boolean> = listOf(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        ).associateWith { false }
    }
}

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val astronomyRepository: AstronomyRepository,
    private val calculateAlarmTimeUseCase: CalculateAlarmTimeUseCase,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditorUiState())
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _finishEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finishEvents: SharedFlow<Unit> = _finishEvents.asSharedFlow()

    private var loadedAlarm: Alarm? = null
    private var saveJob: Job? = null
    private var toastJob: Job? = null
    private var initialized = false

    fun initialize(alarmId: Long) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            astronomyRepository.getAstronomyDataFlow().collect { data ->
                _uiState.update { current -> current.copy(astronomyData = data) }
                refreshCalculatedTime()
            }
        }

        if (alarmId != -1L) {
            loadAlarm(alarmId)
        } else {
            applyClassicDefaults()
        }
    }

    private fun loadAlarm(alarmId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm == null) {
                Logger.e(TAG, "Alarm with ID $alarmId not found")
                _finishEvents.tryEmit(Unit)
                return@launch
            }
            loadedAlarm = alarm
            val isClassic = alarm.relative1 == AppConstants.RELATIVE_TIME_PICK_TIME &&
                alarm.mode == AppConstants.ALARM_MODE_AT
            val mode = if (isClassic) "classic" else "sun"
            val offsetMinutes = if (alarm.mode == AppConstants.ALARM_MODE_BEFORE ||
                alarm.mode == AppConstants.ALARM_MODE_AFTER
            ) {
                snapToOffsetStep((alarm.time1 / 1000 / 60).toInt())
            } else {
                30
            }
            _uiState.update {
                it.copy(
                    alarmId = alarmId,
                    isNew = false,
                    label = alarm.label,
                    mode = mode,
                    sunTimingMode = if (isClassic) AppConstants.ALARM_MODE_AT else alarm.mode,
                    solarEvent1 = if (isClassic) AppConstants.SOLAR_EVENT_SUNRISE else alarm.relative1,
                    solarEvent2 = if (isClassic || alarm.relative2.isBlank()) {
                        AppConstants.SOLAR_EVENT_SUNSET
                    } else {
                        alarm.relative2
                    },
                    offsetMinutes = offsetMinutes,
                    selectedTime = if (isClassic) alarm.time1 else 0L,
                    days = mapOf(
                        "sunday" to alarm.sunday,
                        "monday" to alarm.monday,
                        "tuesday" to alarm.tuesday,
                        "wednesday" to alarm.wednesday,
                        "thursday" to alarm.thursday,
                        "friday" to alarm.friday,
                        "saturday" to alarm.saturday,
                    ),
                    vibrate = alarm.vibrate,
                    snoozeEnabled = alarm.snoozeEnabled,
                    snoozeDuration = alarm.snoozeDuration,
                    snoozeCount = alarm.snoozeCount,
                    volume = alarm.volume,
                    gradualVolume = alarm.gradualVolume,
                    ringtoneUri = alarm.ringtoneUri,
                )
            }
            refreshCalculatedTime()
        }
    }

    private fun applyClassicDefaults() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _uiState.update {
            it.copy(
                mode = "classic",
                sunTimingMode = AppConstants.ALARM_MODE_AT,
                selectedTime = cal.timeInMillis,
            )
        }
        refreshCalculatedTime()
    }

    fun setLabel(value: String) {
        _uiState.update { it.copy(label = value) }
        scheduleAutoSave()
    }

    fun setMode(value: String) {
        if (_uiState.value.mode == value) return
        _uiState.update { it.copy(mode = value) }
        refreshCalculatedTime()
        scheduleAutoSave()
    }

    fun setSunTimingMode(value: String) {
        _uiState.update { it.copy(sunTimingMode = value) }
        refreshCalculatedTime()
        scheduleAutoSave()
    }

    fun setSolarEvent1(event: String) {
        _uiState.update { it.copy(solarEvent1 = event) }
        refreshCalculatedTime()
        scheduleAutoSave()
    }

    fun setSolarEvent2(event: String) {
        _uiState.update { it.copy(solarEvent2 = event) }
        refreshCalculatedTime()
        scheduleAutoSave()
    }

    fun setOffsetMinutes(minutes: Int) {
        _uiState.update { it.copy(offsetMinutes = snapToOffsetStep(minutes)) }
        refreshCalculatedTime()
        scheduleAutoSave()
    }

    fun setOffsetFromSolarRingTime(selectedMillis: Long) {
        val state = _uiState.value
        val solarEventTime = state.astronomyData?.let { getSolarEventTime(state.solarEvent1, it) } ?: return
        if (solarEventTime <= 0L) return
        val diffMinutes = ((selectedMillis - solarEventTime) / 1000 / 60).toInt()
        val abs = kotlin.math.abs(diffMinutes)
        setOffsetMinutes(abs)
    }

    fun setSelectedTime(timeInMillis: Long) {
        _uiState.update { it.copy(selectedTime = timeInMillis) }
        scheduleAutoSave()
    }

    fun toggleDay(day: String) {
        _uiState.update {
            val newDays = it.days.toMutableMap().apply {
                this[day] = !(this[day] ?: false)
            }
            it.copy(days = newDays)
        }
        scheduleAutoSave()
    }

    fun applyDayPreset(preset: DayPreset) {
        val days = when (preset) {
            DayPreset.WEEKDAYS -> weekdayPreset()
            DayPreset.WEEKENDS -> weekendPreset()
            DayPreset.EVERYDAY -> AlarmEditorUiState.DEFAULT_DAYS.mapValues { true }
        }
        _uiState.update { it.copy(days = days) }
        scheduleAutoSave()
    }

    fun setVibrate(value: Boolean) {
        _uiState.update { it.copy(vibrate = value) }
        scheduleAutoSave()
    }

    fun setSnoozeEnabled(value: Boolean) {
        _uiState.update { it.copy(snoozeEnabled = value) }
        scheduleAutoSave()
    }

    fun adjustSnoozeDuration(delta: Int) {
        _uiState.update {
            it.copy(snoozeDuration = (it.snoozeDuration + delta).coerceIn(5, 30))
        }
        scheduleAutoSave()
    }

    fun adjustSnoozeCount(delta: Int) {
        _uiState.update {
            it.copy(snoozeCount = (it.snoozeCount + delta).coerceIn(1, 10))
        }
        scheduleAutoSave()
    }

    fun setGradualVolume(value: Boolean) {
        _uiState.update { it.copy(gradualVolume = value) }
        scheduleAutoSave()
    }

    fun deleteAlarm() {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = loadedAlarm ?: return@launch
            try {
                scheduleAlarmUseCase.cancelAlarm(alarm)
                alarmRepository.deleteAlarm(alarm)
                _finishEvents.tryEmit(Unit)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to delete alarm", e)
                _uiState.update { it.copy(errorMessage = "Failed to delete alarm: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun scheduleAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveAlarm()
        }
    }

    private suspend fun saveAlarm() {
        val state = _uiState.value
        val sanitizedLabel = ValidationHelper.sanitizeLabel(
            state.label.takeIf { it.isNotBlank() } ?: "Alarm",
        )
        val validation = ValidationHelper.validateAlarmEditorInputs(
            label = sanitizedLabel,
            mode = state.mode,
            sunTimingMode = state.sunTimingMode,
            solarEvent1 = state.solarEvent1,
            solarEvent2 = state.solarEvent2,
            offsetMinutes = state.offsetMinutes,
            selectedTime = state.selectedTime,
            monday = state.days["monday"] ?: false,
            tuesday = state.days["tuesday"] ?: false,
            wednesday = state.days["wednesday"] ?: false,
            thursday = state.days["thursday"] ?: false,
            friday = state.days["friday"] ?: false,
            saturday = state.days["saturday"] ?: false,
            sunday = state.days["sunday"] ?: false,
            snoozeDuration = state.snoozeDuration,
            snoozeCount = state.snoozeCount,
            volume = state.volume,
        )
        if (validation.isError()) {
            _uiState.update { it.copy(errorMessage = validation.getErrorMessage()) }
            return
        }

        val alarmFields = buildPersistedFields(state)

        try {
            val savedAlarm = if (state.alarmId == -1L) {
                val creation = AlarmCreation(
                    label = sanitizedLabel,
                    enabled = true,
                    mode = alarmFields.mode,
                    ringtoneUri = state.ringtoneUri,
                    relative1 = alarmFields.relative1,
                    relative2 = alarmFields.relative2,
                    time1 = alarmFields.time1,
                    time2 = alarmFields.time2,
                    calculatedTime = 0L,
                )
                val newId = alarmRepository.insertAlarm(creation)
                _uiState.update { it.copy(alarmId = newId, isNew = false) }
                buildFullAlarm(newId, sanitizedLabel, alarmFields, state)
            } else {
                buildFullAlarm(state.alarmId, sanitizedLabel, alarmFields, state)
            }

            val withCalculated = savedAlarm.copy(
                calculatedTime = calculateAlarmTimeUseCase.execute(savedAlarm),
            )
            alarmRepository.updateAlarm(withCalculated)
            loadedAlarm = withCalculated

            scheduleAlarmUseCase.scheduleAlarm(withCalculated).fold(
                onSuccess = { Logger.i(TAG, "Alarm '${withCalculated.label}' (ID: ${withCalculated.id}) scheduled") },
                onFailure = { Logger.e(TAG, "Error scheduling alarm ${withCalculated.id}", it) },
            )

            emitNextAlarmToast(withCalculated)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save alarm", e)
            _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
        }
    }

    private fun emitNextAlarmToast(alarm: Alarm) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            try {
                val nextAlarmTime = calculateAlarmTimeUseCase.execute(alarm)
                val cal = Calendar.getInstance().apply { timeInMillis = nextAlarmTime }
                val today = Calendar.getInstance()
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val formattedTime = timeFormat.format(cal.time)
                val whenString = when {
                    sameDay(cal, today) -> "today at $formattedTime"
                    sameDay(cal, tomorrow) -> "tomorrow at $formattedTime"
                    else -> "${java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(cal.time)} at $formattedTime"
                }
                _toastEvents.tryEmit("Alarm will ring $whenString")
            } catch (e: Exception) {
                Logger.e(TAG, "Error calculating alarm time for toast", e)
            }
        }
    }

    private fun buildPersistedFields(state: AlarmEditorUiState): PersistedFields {
        return if (state.mode == "sun") {
            val mode = state.sunTimingMode
            val time1 = if (mode == AppConstants.ALARM_MODE_BEFORE || mode == AppConstants.ALARM_MODE_AFTER) {
                state.offsetMinutes * AppConstants.MILLIS_PER_MINUTE.toLong()
            } else {
                0L
            }
            PersistedFields(
                mode = mode,
                relative1 = state.solarEvent1,
                relative2 = if (mode == AppConstants.ALARM_MODE_BETWEEN) state.solarEvent2 else "",
                time1 = time1,
                time2 = 0L,
            )
        } else {
            PersistedFields(
                mode = AppConstants.ALARM_MODE_AT,
                relative1 = AppConstants.RELATIVE_TIME_PICK_TIME,
                relative2 = "",
                time1 = state.selectedTime,
                time2 = 0L,
            )
        }
    }

    private fun buildFullAlarm(
        id: Long,
        sanitizedLabel: String,
        fields: PersistedFields,
        state: AlarmEditorUiState,
    ): Alarm = Alarm(
        id = id,
        label = sanitizedLabel,
        enabled = true,
        mode = fields.mode,
        ringtoneUri = state.ringtoneUri,
        relative1 = fields.relative1,
        relative2 = fields.relative2,
        time1 = fields.time1,
        time2 = fields.time2,
        calculatedTime = 0L,
        monday = state.days["monday"] ?: false,
        tuesday = state.days["tuesday"] ?: false,
        wednesday = state.days["wednesday"] ?: false,
        thursday = state.days["thursday"] ?: false,
        friday = state.days["friday"] ?: false,
        saturday = state.days["saturday"] ?: false,
        sunday = state.days["sunday"] ?: false,
        vibrate = state.vibrate,
        snoozeEnabled = state.snoozeEnabled,
        snoozeDuration = state.snoozeDuration,
        snoozeCount = state.snoozeCount,
        volume = state.volume,
        gradualVolume = state.gradualVolume,
    )

    private fun refreshCalculatedTime() {
        val state = _uiState.value
        if (state.mode != "sun") {
            _uiState.update { it.copy(calculatedTime = null) }
            return
        }
        val data = state.astronomyData
        if (data == null) {
            _uiState.update { it.copy(calculatedTime = null) }
            return
        }
        val event1Time = getSolarEventTime(state.solarEvent1, data)
        if (event1Time <= 0L) {
            _uiState.update { it.copy(calculatedTime = null) }
            return
        }
        val raw = when (state.sunTimingMode) {
            AppConstants.ALARM_MODE_AT -> event1Time
            AppConstants.ALARM_MODE_BEFORE -> event1Time - state.offsetMinutes * AppConstants.MILLIS_PER_MINUTE
            AppConstants.ALARM_MODE_AFTER -> event1Time + state.offsetMinutes * AppConstants.MILLIS_PER_MINUTE
            AppConstants.ALARM_MODE_BETWEEN -> {
                val event2Time = getSolarEventTime(state.solarEvent2, data)
                if (event2Time <= 0L) event1Time else (event1Time + event2Time) / 2
            }
            else -> event1Time
        }
        val resolved = if (raw <= System.currentTimeMillis()) {
            Calendar.getInstance().apply {
                timeInMillis = raw
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        } else {
            raw
        }
        _uiState.update { it.copy(calculatedTime = resolved) }
    }

    private fun getSolarEventTime(event: String, data: AstronomyDataEntity): Long {
        val original = when (event.trim()) {
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN -> data.astroDawn
            AppConstants.SOLAR_EVENT_NAUTICAL_DAWN -> data.nauticalDawn
            AppConstants.SOLAR_EVENT_CIVIL_DAWN -> data.civilDawn
            AppConstants.SOLAR_EVENT_SUNRISE -> data.sunrise
            AppConstants.SOLAR_EVENT_SOLAR_NOON -> data.solarNoon
            AppConstants.SOLAR_EVENT_SUNSET -> data.sunset
            AppConstants.SOLAR_EVENT_CIVIL_DUSK -> data.civilDusk
            AppConstants.SOLAR_EVENT_NAUTICAL_DUSK -> data.nauticalDusk
            AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK -> data.astroDusk
            else -> 0L
        }
        if (original <= 0L) return 0L
        val cal = Calendar.getInstance().apply { timeInMillis = original }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun snapToOffsetStep(minutes: Int): Int {
        val step = 5
        return ((minutes + step / 2) / step * step).coerceIn(5, 720)
    }

    private fun weekdayPreset(): Map<String, Boolean> = mapOf(
        "monday" to true, "tuesday" to true, "wednesday" to true, "thursday" to true,
        "friday" to true, "saturday" to false, "sunday" to false,
    )

    private fun weekendPreset(): Map<String, Boolean> = mapOf(
        "monday" to false, "tuesday" to false, "wednesday" to false, "thursday" to false,
        "friday" to false, "saturday" to true, "sunday" to true,
    )

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
            a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)

    private data class PersistedFields(
        val mode: String,
        val relative1: String,
        val relative2: String,
        val time1: Long,
        val time2: Long,
    )

    enum class DayPreset { WEEKDAYS, WEEKENDS, EVERYDAY }

    companion object {
        private const val TAG = "AlarmEditorViewModel"
        private const val SAVE_DEBOUNCE_MS = 300L
    }
}
