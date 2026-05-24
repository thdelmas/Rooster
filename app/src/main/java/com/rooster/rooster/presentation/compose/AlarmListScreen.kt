package com.rooster.rooster.presentation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooster.rooster.Alarm
import com.rooster.rooster.R
import com.rooster.rooster.presentation.viewmodel.AlarmListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onAlarmEnabledToggled: (Alarm, Boolean) -> Unit,
) {
    val alarms by viewModel.allAlarms.observeAsState(emptyList())
    val sortedAlarms = sortByTimeOfDay(alarms)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alarms",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateAlarm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.add_alarm_button),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                text = { Text(text = "New Alarm") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (sortedAlarms.isEmpty()) {
                EmptyAlarmState(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            } else {
                AlarmList(
                    alarms = sortedAlarms,
                    onToggleEnabled = onAlarmEnabledToggled,
                    onEditAlarm = onEditAlarm,
                )
            }
        }
    }
}

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onToggleEnabled: (Alarm, Boolean) -> Unit,
    onEditAlarm: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 100.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onToggleEnabled = { isChecked -> onToggleEnabled(alarm, isChecked) },
                onClick = { onEditAlarm(alarm.id) },
            )
        }
    }
}

@Composable
private fun EmptyAlarmState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🌅", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Alarms Yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap New Alarm to wake up naturally with the sun",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.9f),
        )
    }
}
