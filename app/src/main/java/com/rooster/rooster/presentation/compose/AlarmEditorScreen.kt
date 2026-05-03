package com.rooster.rooster.presentation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.NumberPicker
import com.rooster.rooster.SunCourseView
import com.rooster.rooster.presentation.viewmodel.AlarmEditorUiState
import com.rooster.rooster.presentation.viewmodel.AlarmEditorViewModel.DayPreset
import com.rooster.rooster.ui.SolarRingTimePickerView
import com.rooster.rooster.util.AppConstants
import com.rooster.rooster.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val SolarEvents = listOf(
    AppConstants.SOLAR_EVENT_ASTRONOMICAL_DAWN to "🌄",
    AppConstants.SOLAR_EVENT_NAUTICAL_DAWN to "🌅",
    AppConstants.SOLAR_EVENT_CIVIL_DAWN to "🌆",
    AppConstants.SOLAR_EVENT_SUNRISE to "🌅",
    AppConstants.SOLAR_EVENT_SOLAR_NOON to "☀️",
    AppConstants.SOLAR_EVENT_SUNSET to "🌇",
    AppConstants.SOLAR_EVENT_CIVIL_DUSK to "🌆",
    AppConstants.SOLAR_EVENT_NAUTICAL_DUSK to "🌃",
    AppConstants.SOLAR_EVENT_ASTRONOMICAL_DUSK to "🌌",
)

private val DaysOfWeek = listOf(
    "sunday" to "S",
    "monday" to "M",
    "tuesday" to "T",
    "wednesday" to "W",
    "thursday" to "T",
    "friday" to "F",
    "saturday" to "S",
)

private val OffsetPresets = listOf(15, 30, 60, 120, 180, 360, 480, 720)

private fun emojiFor(event: String): String =
    SolarEvents.firstOrNull { it.first == event.trim() }?.second ?: "🌅"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    state: AlarmEditorUiState,
    snackbarHostState: SnackbarHostState,
    onLabelChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onSunTimingChange: (String) -> Unit,
    onSolarEvent1Change: (String) -> Unit,
    onSolarEvent2Change: (String) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onSolarRingTimeSelected: (Long) -> Unit,
    onToggleDay: (String) -> Unit,
    onApplyDayPreset: (DayPreset) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    onAdjustSnoozeDuration: (Int) -> Unit,
    onAdjustSnoozeCount: (Int) -> Unit,
    onGradualVolumeChange: (Boolean) -> Unit,
    onPickClassicTime: () -> Unit,
    onPreviewRingtone: () -> Unit,
    onPickRingtone: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var showSolarEventPicker by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Alarm",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            LabelCard(label = state.label, onLabelChange = onLabelChange)
            Spacer(Modifier.height(20.dp))
            ModeCard(currentMode = state.mode, onModeChange = onModeChange)
            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = state.mode == "sun", enter = fadeIn(), exit = fadeOut()) {
                Column {
                    SunModeCard(
                        state = state,
                        onSunTimingChange = onSunTimingChange,
                        onSolarEvent1Click = { showSolarEventPicker = 1 },
                        onSolarEvent2Click = { showSolarEventPicker = 2 },
                        onOffsetChange = onOffsetChange,
                        onSolarRingTimeSelected = onSolarRingTimeSelected,
                        onPresetSolarEvent = onSolarEvent1Change,
                        onTimingMode = onSunTimingChange,
                    )
                    Spacer(Modifier.height(20.dp))
                    if (state.calculatedTime != null) {
                        CalculatedTimeCard(time = state.calculatedTime)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            AnimatedVisibility(visible = state.mode == "classic", enter = fadeIn(), exit = fadeOut()) {
                Column {
                    ClassicModeCard(
                        selectedTime = state.selectedTime,
                        onPickClassicTime = onPickClassicTime,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            RepeatDaysCard(
                days = state.days,
                onToggleDay = onToggleDay,
                onApplyPreset = onApplyDayPreset,
            )
            Spacer(Modifier.height(20.dp))
            AdditionalSettingsCard(
                state = state,
                onVibrateChange = onVibrateChange,
                onSnoozeEnabledChange = onSnoozeEnabledChange,
                onAdjustSnoozeDuration = onAdjustSnoozeDuration,
                onAdjustSnoozeCount = onAdjustSnoozeCount,
                onGradualVolumeChange = onGradualVolumeChange,
                onPreviewRingtone = onPreviewRingtone,
                onPickRingtone = onPickRingtone,
            )

            if (!state.isNew) {
                Spacer(Modifier.height(20.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Delete Alarm",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    val pickerNumber = showSolarEventPicker
    if (pickerNumber != null) {
        SolarEventPickerDialog(
            onSelect = { event ->
                if (pickerNumber == 1) onSolarEvent1Change(event) else onSolarEvent2Change(event)
                showSolarEventPicker = null
            },
            onDismiss = { showSolarEventPicker = null },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Alarm") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelCard(label: String, onLabelChange: (String) -> Unit) {
    GlassCard {
        Column {
            SectionLabel("Alarm Label")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                placeholder = { Text("Morning Alarm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModeCard(currentMode: String, onModeChange: (String) -> Unit) {
    GlassCard {
        Column {
            SectionLabel("Alarm Mode")
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeButton(
                    text = "☀️\nSun Mode",
                    selected = currentMode == "sun",
                    onClick = { onModeChange("sun") },
                    modifier = Modifier.weight(1f),
                )
                ModeButton(
                    text = "🕐\nClassic",
                    selected = currentMode == "classic",
                    onClick = { onModeChange("classic") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SunModeCard(
    state: AlarmEditorUiState,
    onSunTimingChange: (String) -> Unit,
    onSolarEvent1Click: () -> Unit,
    onSolarEvent2Click: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onSolarRingTimeSelected: (Long) -> Unit,
    onPresetSolarEvent: (String) -> Unit,
    onTimingMode: (String) -> Unit,
) {
    GlassCard {
        Column {
            Text(
                text = "Sun Course Timing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            SunCourseAndroidView(
                state = state,
                onSolarEventSelected = { event ->
                    onPresetSolarEvent(event)
                    onTimingMode(AppConstants.ALARM_MODE_AT)
                },
            )
            Spacer(Modifier.height(24.dp))

            SectionLabel("Quick Presets", Modifier)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PresetEventButton(
                    text = "🌅 Sunrise",
                    onClick = {
                        onPresetSolarEvent(AppConstants.SOLAR_EVENT_SUNRISE)
                        onTimingMode(AppConstants.ALARM_MODE_AT)
                    },
                    modifier = Modifier.weight(1f),
                )
                PresetEventButton(
                    text = "🌇 Sunset",
                    onClick = {
                        onPresetSolarEvent(AppConstants.SOLAR_EVENT_SUNSET)
                        onTimingMode(AppConstants.ALARM_MODE_AT)
                    },
                    modifier = Modifier.weight(1f),
                )
                PresetEventButton(
                    text = "🌆 Dawn",
                    onClick = {
                        onPresetSolarEvent(AppConstants.SOLAR_EVENT_CIVIL_DAWN)
                        onTimingMode(AppConstants.ALARM_MODE_AT)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Timing")
            Spacer(Modifier.height(8.dp))
            TimingToggle(
                selected = state.sunTimingMode,
                onSelect = onSunTimingChange,
            )
            Spacer(Modifier.height(20.dp))

            SectionLabel("Solar Event")
            Spacer(Modifier.height(8.dp))
            EventSelectorButton(
                event = state.solarEvent1,
                onClick = onSolarEvent1Click,
            )
            Spacer(Modifier.height(16.dp))

            val isOffsetMode = state.sunTimingMode == AppConstants.ALARM_MODE_BEFORE ||
                state.sunTimingMode == AppConstants.ALARM_MODE_AFTER
            AnimatedVisibility(visible = isOffsetMode, enter = fadeIn(), exit = fadeOut()) {
                OffsetEditor(
                    state = state,
                    onOffsetChange = onOffsetChange,
                    onSolarRingTimeSelected = onSolarRingTimeSelected,
                )
            }

            AnimatedVisibility(
                visible = state.sunTimingMode == AppConstants.ALARM_MODE_BETWEEN,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    SectionLabel("And")
                    Spacer(Modifier.height(8.dp))
                    EventSelectorButton(
                        event = state.solarEvent2,
                        onClick = onSolarEvent2Click,
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetEventButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}

@Composable
private fun TimingToggle(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        AppConstants.ALARM_MODE_AT to "At",
        AppConstants.ALARM_MODE_BEFORE to "Before",
        AppConstants.ALARM_MODE_AFTER to "After",
        AppConstants.ALARM_MODE_BETWEEN to "Between",
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                options.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                else -> RoundedCornerShape(0.dp)
            }
            val isSelected = value == selected
            val container = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
            val content = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            OutlinedButton(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = container,
                    contentColor = content,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(label, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EventSelectorButton(event: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "${emojiFor(event)} $event",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
        )
        Icon(
            painter = painterResource(com.rooster.rooster.R.drawable.down_arrow),
            contentDescription = null,
        )
    }
}

@Composable
private fun OffsetEditor(
    state: AlarmEditorUiState,
    onOffsetChange: (Int) -> Unit,
    onSolarRingTimeSelected: (Long) -> Unit,
) {
    var showDurationPicker by remember { mutableStateOf(false) }

    Column {
        SectionLabel("Select Time Offset")
        Spacer(Modifier.height(12.dp))
        SolarRingAndroidView(
            state = state,
            onTimeSelected = onSolarRingTimeSelected,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Or use slider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { showDurationPicker = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = TimeUtils.formatMinutesAsHours(state.offsetMinutes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Slider(
            value = state.offsetMinutes.toFloat(),
            onValueChange = { onOffsetChange(it.toInt()) },
            valueRange = 5f..720f,
            steps = ((720 - 5) / 5) - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Text(
            text = "Drag slider or tap button for precise time",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        SectionLabel("Quick presets")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OffsetPresets.take(4).forEach { minutes ->
                    OffsetPresetButton(minutes = minutes, onClick = { onOffsetChange(minutes) }, modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OffsetPresets.drop(4).forEach { minutes ->
                    OffsetPresetButton(minutes = minutes, onClick = { onOffsetChange(minutes) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            initialMinutes = state.offsetMinutes,
            onConfirm = { minutes ->
                onOffsetChange(minutes)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false },
        )
    }
}

@Composable
private fun OffsetPresetButton(minutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = TimeUtils.formatMinutesAsHours(minutes), fontSize = 14.sp)
    }
}

@Composable
private fun ClassicModeCard(selectedTime: Long, onPickClassicTime: () -> Unit) {
    GlassCard {
        Column {
            Text(
                text = "Set Time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap to set alarm time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { onPickClassicTime() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatTime(selectedTime),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatedTimeCard(time: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Next alarm at",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatTime(time),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RepeatDaysCard(
    days: Map<String, Boolean>,
    onToggleDay: (String) -> Unit,
    onApplyPreset: (DayPreset) -> Unit,
) {
    GlassCard {
        Column {
            SectionLabel("Repeat")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onApplyPreset(DayPreset.WEEKDAYS) },
                    modifier = Modifier.weight(1f),
                ) { Text("Weekdays", fontSize = 12.sp) }
                TextButton(
                    onClick = { onApplyPreset(DayPreset.WEEKENDS) },
                    modifier = Modifier.weight(1f),
                ) { Text("Weekends", fontSize = 12.sp) }
                TextButton(
                    onClick = { onApplyPreset(DayPreset.EVERYDAY) },
                    modifier = Modifier.weight(1f),
                ) { Text("Every Day", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DaysOfWeek.forEach { (day, label) ->
                    DayButton(
                        label = label,
                        selected = days[day] ?: false,
                        onClick = { onToggleDay(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AdditionalSettingsCard(
    state: AlarmEditorUiState,
    onVibrateChange: (Boolean) -> Unit,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    onAdjustSnoozeDuration: (Int) -> Unit,
    onAdjustSnoozeCount: (Int) -> Unit,
    onGradualVolumeChange: (Boolean) -> Unit,
    onPreviewRingtone: () -> Unit,
    onPickRingtone: () -> Unit,
) {
    GlassCard {
        Column {
            RingtoneRow(
                onPreview = onPreviewRingtone,
                onPick = onPickRingtone,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchRow(
                title = "📳 Vibrate",
                checked = state.vibrate,
                onCheckedChange = onVibrateChange,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchRow(
                title = "⏰ Snooze",
                checked = state.snoozeEnabled,
                onCheckedChange = onSnoozeEnabledChange,
            )
            AnimatedVisibility(visible = state.snoozeEnabled, enter = fadeIn(), exit = fadeOut()) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    StepperRow(
                        label = "Snooze Duration",
                        value = "${state.snoozeDuration} min",
                        onDecrement = { onAdjustSnoozeDuration(-5) },
                        onIncrement = { onAdjustSnoozeDuration(5) },
                    )
                    Spacer(Modifier.height(12.dp))
                    StepperRow(
                        label = "Max Snoozes",
                        value = "${state.snoozeCount} times",
                        onDecrement = { onAdjustSnoozeCount(-1) },
                        onIncrement = { onAdjustSnoozeCount(1) },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SwitchRow(
                title = "📈 Gradual Volume",
                checked = state.gradualVolume,
                onCheckedChange = onGradualVolumeChange,
            )
        }
    }
}

@Composable
private fun RingtoneRow(onPreview: () -> Unit, onPick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🎵 Sound",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onPreview,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.clickable(onClick = onPreview),
            ) {
                Text(text = "▶ Default", fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tap to preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onPick) {
                Text(text = "Change", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(text = "-", onClick = onDecrement)
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
            )
            StepperButton(text = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
private fun SunCourseAndroidView(
    state: AlarmEditorUiState,
    onSolarEventSelected: (String) -> Unit,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { context ->
            SunCourseView(context).apply {
                isClickable = true
                isFocusable = true
            }
        },
        update = { view ->
            view.onSolarEventSelected = { onSolarEventSelected(it) }
            val data = state.astronomyData
            if (data != null) {
                view.setAllSunTimes(
                    data.astroDawn, data.nauticalDawn, data.civilDawn,
                    data.sunrise, data.solarNoon, data.sunset,
                    data.civilDusk, data.nauticalDusk, data.astroDusk,
                )
                if (state.calculatedTime != null) {
                    val markerLabel = when (state.sunTimingMode) {
                        AppConstants.ALARM_MODE_AT -> "Alarm"
                        AppConstants.ALARM_MODE_BEFORE ->
                            "${TimeUtils.formatMinutesAsHours(state.offsetMinutes)} before"
                        AppConstants.ALARM_MODE_AFTER ->
                            "${TimeUtils.formatMinutesAsHours(state.offsetMinutes)} after"
                        AppConstants.ALARM_MODE_BETWEEN -> "Between"
                        else -> "Alarm"
                    }
                    view.setMarker(state.calculatedTime, markerLabel)
                }
            }
        },
    )
}

@Composable
private fun SolarRingAndroidView(
    state: AlarmEditorUiState,
    onTimeSelected: (Long) -> Unit,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 400.dp),
        factory = { context ->
            SolarRingTimePickerView(context).apply {
                setOnTimeSelectedListener(onTimeSelected)
            }
        },
        update = { view ->
            view.setOnTimeSelectedListener(onTimeSelected)
            view.setAstronomyData(state.astronomyData)
            if (state.calculatedTime != null) {
                view.setSelectedTime(state.calculatedTime)
            }
        },
    )
}

@Composable
private fun SolarEventPickerDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Solar Event") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(SolarEvents) { (event, emoji) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(event) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(text = event, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DurationPickerDialog(
    initialMinutes: Int,
    minMinutes: Int = 5,
    maxMinutes: Int = 720,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hours by remember { mutableStateOf((initialMinutes / 60).coerceIn(0, 12)) }
    var minutes by remember { mutableStateOf((initialMinutes % 60).coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Duration",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AndroidView(
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = 12
                                wrapSelectorWheel = false
                                value = hours
                                setOnValueChangedListener { _, _, newVal -> hours = newVal }
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Hours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = ":",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AndroidView(
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = 59
                                wrapSelectorWheel = false
                                value = minutes
                                setOnValueChangedListener { _, _, newVal -> minutes = newVal }
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = (hours * 60 + minutes).coerceIn(minMinutes, maxMinutes)
                onConfirm(total)
            }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatTime(timeInMillis: Long): String {
    if (timeInMillis <= 0L) return "--:--"
    val cal = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}
