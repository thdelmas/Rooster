package com.rooster.rooster.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rooster.rooster.R
import com.rooster.rooster.util.SleepProfileHelper
import com.rooster.rooster.util.ThemeHelper

data class CoordinatesUi(
    val altitude: Double?,
    val latitude: Double?,
    val longitude: Double?,
)

data class AstronomyTimes(
    val astroDawn: Long,
    val nauticalDawn: Long,
    val civilDawn: Long,
    val sunrise: Long,
    val solarNoon: Long,
    val sunset: Long,
    val civilDusk: Long,
    val nauticalDusk: Long,
    val astroDusk: Long,
    val dayLengthMillis: Long,
)

private val SunriseColor = Color(0xFFFF8E53)
private val SunsetColor = Color(0xFFFF8E53)
private val GlassBorderColor = Color(0xFFFF8E53)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    coordinates: CoordinatesUi,
    astronomy: AstronomyTimes,
    onNavigateBack: () -> Unit,
    onSyncGps: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenLinkedIn: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenEmail: () -> Unit,
    onDynamicColorsChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(ThemeHelper.getThemeMode(context)) }
    var dynamicColorsEnabled by remember { mutableStateOf(ThemeHelper.isDynamicColorsEnabled(context)) }
    var sleepProfile by remember { mutableStateOf(SleepProfileHelper.getSleepProfile(context)) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionHeader("Location")
            GlassCard {
                CoordinateRow("Altitude", coordinates.altitude, "m")
                Divider()
                CoordinateRow("Latitude", coordinates.latitude, "°")
                Divider()
                CoordinateRow("Longitude", coordinates.longitude, "°")
                Divider()
                SettingRow(
                    title = "Location",
                    valueText = "Earth",
                )
            }
            Spacer(Modifier.height(16.dp))
            RefreshGpsCard(onClick = onSyncGps)
            Spacer(Modifier.height(24.dp))

            SectionHeader("Astronomy Data")
            DayLengthCard(astronomy.dayLengthMillis)
            Spacer(Modifier.height(16.dp))
            GlassCard {
                AstronomyRow("Astronomical Dawn", astronomy.astroDawn)
                Divider()
                AstronomyRow("Nautical Dawn", astronomy.nauticalDawn)
                Divider()
                AstronomyRow("Civil Dawn", astronomy.civilDawn)
                Divider()
                AstronomyRow(
                    title = "☀️ Sunrise",
                    timeMillis = astronomy.sunrise,
                    titleStyle = HighlightStyle.Sunrise,
                )
                Divider()
                AstronomyRow("Solar Noon", astronomy.solarNoon)
                Divider()
                AstronomyRow(
                    title = "🌅 Sunset",
                    timeMillis = astronomy.sunset,
                    titleStyle = HighlightStyle.Sunset,
                )
                Divider()
                AstronomyRow("Civil Dusk", astronomy.civilDusk)
                Divider()
                AstronomyRow("Nautical Dusk", astronomy.nauticalDusk)
                Divider()
                AstronomyRow("Astronomical Dusk", astronomy.astroDusk)
            }
            Spacer(Modifier.height(24.dp))

            SectionHeader("Appearance")
            GlassCard {
                SettingRow(
                    title = "Theme",
                    description = "Choose your preferred theme",
                    valueText = ThemeHelper.getThemeModeName(themeMode),
                    onClick = { showThemeDialog = true },
                )
                if (ThemeHelper.supportsDynamicColors()) {
                    Divider()
                    SettingRow(
                        title = "Dynamic Colors",
                        description = "Match system wallpaper colors (Android 12+)",
                        trailing = {
                            Switch(
                                checked = dynamicColorsEnabled,
                                onCheckedChange = { checked ->
                                    dynamicColorsEnabled = checked
                                    onDynamicColorsChanged(checked)
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            SectionHeader("Sleep Profile")
            GlassCard {
                SettingRow(
                    title = "Sleep Profile",
                    description = "Set defaults for new alarms",
                    valueText = SleepProfileHelper.getProfileName(sleepProfile),
                    onClick = { showSleepDialog = true },
                )
            }
            Spacer(Modifier.height(24.dp))

            SectionHeader("About")
            NavigationCard(
                title = "Support Rooster",
                description = "Tip the developer, rate the app, or send feedback",
                onClick = onOpenSupport,
            )
            Spacer(Modifier.height(8.dp))
            NavigationCard(
                title = "Credits & Licenses",
                description = "Audio sources, attributions, and licenses",
                onClick = onOpenCredits,
            )
            Spacer(Modifier.height(16.dp))
            SocialIconsCard(
                onLinkedIn = onOpenLinkedIn,
                onGitHub = onOpenGitHub,
                onEmail = onOpenEmail,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Choose Theme",
            options = listOf("Auto (System)", "Light", "Dark"),
            selectedIndex = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelected = { idx ->
                ThemeHelper.setThemeMode(context, idx)
                themeMode = idx
                showThemeDialog = false
            },
        )
    }
    if (showSleepDialog) {
        SingleChoiceDialog(
            title = "Choose Sleep Profile",
            options = listOf("None", "Early Bird (Sunrise)", "Night Owl (Sunset)"),
            selectedIndex = sleepProfile,
            onDismiss = { showSleepDialog = false },
            onSelected = { idx ->
                SleepProfileHelper.setSleepProfile(context, idx)
                sleepProfile = idx
                showSleepDialog = false
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
    )
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
            .background(Color.Transparent, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    )
}

@Composable
private fun CoordinateRow(label: String, value: Double?, suffix: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (value != null) String.format("%.4f $suffix", value) else "0.0 $suffix",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private enum class HighlightStyle { None, Sunrise, Sunset }

@Composable
private fun AstronomyRow(
    title: String,
    timeMillis: Long,
    titleStyle: HighlightStyle = HighlightStyle.None,
) {
    val highlightColor = when (titleStyle) {
        HighlightStyle.Sunrise -> SunriseColor
        HighlightStyle.Sunset -> SunsetColor
        HighlightStyle.None -> null
    }
    val titleTextStyle = if (highlightColor != null) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val valueStyle = if (highlightColor != null) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.titleMedium
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = titleTextStyle,
            fontWeight = if (highlightColor != null) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatTimeOfDay(timeMillis),
            color = highlightColor ?: MaterialTheme.colorScheme.primary,
            style = valueStyle,
            fontWeight = if (highlightColor != null) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String? = null,
    valueText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .padding(vertical = 12.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (valueText != null) {
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshGpsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_mylocation),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Refresh Coordinates",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun DayLengthCard(dayLengthMillis: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Day Length",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatDayLength(dayLengthMillis),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = "›",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun SocialIconsCard(
    onLinkedIn: () -> Unit,
    onGitHub: () -> Unit,
    onEmail: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialIcon(
                drawableRes = R.drawable.linkedin_icon,
                contentDescription = "LinkedIn",
                onClick = onLinkedIn,
            )
            Spacer(Modifier.width(16.dp))
            SocialIcon(
                drawableRes = R.drawable.github_icon,
                contentDescription = "GitHub",
                onClick = onGitHub,
            )
            Spacer(Modifier.width(16.dp))
            SocialIcon(
                drawableRes = R.drawable.email_icon,
                contentDescription = "Email",
                onClick = onEmail,
            )
        }
    }
}

@Composable
private fun SocialIcon(drawableRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelected(index) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatTimeOfDay(timeMillis: Long): String {
    if (timeMillis == 0L) return "--:--"
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    sdf.timeZone = java.util.TimeZone.getDefault()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timeMillis }
    if (sdf.timeZone.inDaylightTime(cal.time)) {
        cal.add(java.util.Calendar.MILLISECOND, sdf.timeZone.dstSavings)
    }
    return sdf.format(cal.time)
}

private fun formatDayLength(dayLengthMillis: Long): String {
    val seconds = dayLengthMillis / 1000
    val hours = seconds / 3600
    val minutes = (seconds / 60) % 60
    return String.format("%02d:%02d", hours, minutes)
}
