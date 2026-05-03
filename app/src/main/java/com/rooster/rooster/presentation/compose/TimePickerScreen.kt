package com.rooster.rooster.presentation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.ui.SolarRingTimePickerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class PickerMode { TRADITIONAL, SOLAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerScreen(
    mode: PickerMode,
    selectedTime: Long,
    astronomyData: AstronomyDataEntity?,
    onModeChange: (PickerMode) -> Unit,
    onPickTraditionalTime: () -> Unit,
    onSolarTimeSelected: (Long) -> Unit,
    onContinue: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pick Time",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onContinue,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp),
                text = { Text("Continue", fontWeight = FontWeight.Bold) },
                icon = {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_directions),
                        contentDescription = null,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            ModeCard(currentMode = mode, onModeChange = onModeChange)
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(visible = mode == PickerMode.TRADITIONAL, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    TraditionalCard(
                        selectedTime = selectedTime,
                        onPickTime = onPickTraditionalTime,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            AnimatedVisibility(visible = mode == PickerMode.SOLAR, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    SolarCard(
                        selectedTime = selectedTime,
                        astronomyData = astronomyData,
                        onSolarTimeSelected = onSolarTimeSelected,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun ModeCard(currentMode: PickerMode, onModeChange: (PickerMode) -> Unit) {
    GlassCard {
        Column {
            Text(
                text = "Choose Time Picker",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeButton(
                    text = "Time",
                    selected = currentMode == PickerMode.TRADITIONAL,
                    onClick = { onModeChange(PickerMode.TRADITIONAL) },
                    modifier = Modifier.weight(1f),
                )
                ModeButton(
                    text = "Solar",
                    selected = currentMode == PickerMode.SOLAR,
                    onClick = { onModeChange(PickerMode.SOLAR) },
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
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
private fun TraditionalCard(selectedTime: Long, onPickTime: () -> Unit) {
    GlassCard {
        Column {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = formatTime(selectedTime),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onPickTime,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(text = "Change Time", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SolarCard(
    selectedTime: Long,
    astronomyData: AstronomyDataEntity?,
    onSolarTimeSelected: (Long) -> Unit,
) {
    GlassCard {
        Column {
            Text(
                text = "Select Time on Sun Course",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = 600.dp),
                factory = { context ->
                    SolarRingTimePickerView(context).apply {
                        setOnTimeSelectedListener(onSolarTimeSelected)
                    }
                },
                update = { view ->
                    view.setOnTimeSelectedListener(onSolarTimeSelected)
                    view.setAstronomyData(astronomyData)
                    if (selectedTime > 0L) {
                        view.setSelectedTime(selectedTime)
                    }
                },
            )
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    if (timeInMillis <= 0L) return "12:00 PM"
    val cal = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}
