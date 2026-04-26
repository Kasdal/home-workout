package com.example.workoutapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.ui.components.BottomNavBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showRestTimerDialog by remember { mutableStateOf(false) }
    var showSwitchTimerDialog by remember { mutableStateOf(false) }

    if (showRestTimerDialog) {
        TimerSettingDialog(
            title = "Set Rest Timer",
            currentValue = settings.restTimerDuration,
            onDismiss = { showRestTimerDialog = false },
            onSave = {
                viewModel.setRestTimerDuration(it)
                showRestTimerDialog = false
            }
        )
    }

    if (showSwitchTimerDialog) {
        TimerSettingDialog(
            title = "Set Exercise Switch Timer",
            currentValue = settings.exerciseSwitchDuration,
            onDismiss = { showSwitchTimerDialog = false },
            onSave = {
                viewModel.setExerciseSwitchDuration(it)
                showSwitchTimerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "settings",
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sound Settings Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sound Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    SettingsSwitchRow(
                        title = "Enable sounds",
                        description = "Play workout cues and completion alerts.",
                        checked = settings.soundsEnabled,
                        onCheckedChange = { viewModel.toggleSounds(it) }
                    )

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume: ${(settings.soundVolume * 100).toInt()}%")
                        TextButton(
                            onClick = { viewModel.previewTimerSound(settings.timerSoundType) },
                            enabled = settings.soundsEnabled
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test")
                        }
                    }
                    Slider(
                        value = settings.soundVolume,
                        onValueChange = { viewModel.setSoundVolume(it) },
                        enabled = settings.soundsEnabled
                    )

                    SettingsSwitchRow(
                        title = "Vibration cues",
                        description = "Vibrate for alerts, especially when the phone is muted.",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = {
                            viewModel.setVibrationEnabled(it)
                            if (it) viewModel.previewVibration()
                        }
                    )

                    SettingsSwitchRow(
                        title = "Final countdown",
                        description = "Play countdown cues during the final 3 seconds.",
                        checked = settings.finalCountdownEnabled,
                        onCheckedChange = { viewModel.setFinalCountdownEnabled(it) }
                    )

                    ChoiceChipSelector(
                        title = "Silent mode",
                        description = "Choose how workout alerts behave when your phone is muted.",
                        options = silentModeOptions,
                        selectedId = settings.silentModeBehavior,
                        onSelect = { viewModel.setSilentModeBehavior(it) }
                    )

                    SoundOptionSelector(
                        title = "Timer countdown",
                        description = "Played when workout timers need your attention.",
                        options = timerSoundOptions,
                        selectedId = settings.timerSoundType,
                        enabled = settings.soundsEnabled,
                        onSelect = { viewModel.setTimerSound(it) },
                        onPreview = { viewModel.previewTimerSound(it) }
                    )

                    SoundOptionSelector(
                        title = "Rest complete",
                        description = "Played when a normal rest timer reaches zero.",
                        options = timerSoundOptions,
                        selectedId = settings.restCompleteSoundType,
                        enabled = settings.soundsEnabled,
                        onSelect = { viewModel.setRestCompleteSound(it) },
                        onPreview = { viewModel.previewTimerSound(it) }
                    )

                    SoundOptionSelector(
                        title = "Exercise switch",
                        description = "Played when it is time to move to the next exercise.",
                        options = timerSoundOptions,
                        selectedId = settings.exerciseSwitchSoundType,
                        enabled = settings.soundsEnabled,
                        onSelect = { viewModel.setExerciseSwitchSound(it) },
                        onPreview = { viewModel.previewTimerSound(it) }
                    )

                    SoundOptionSelector(
                        title = "Workout complete",
                        description = "Played after finishing a full session.",
                        options = celebrationSoundOptions,
                        selectedId = settings.celebrationSoundType,
                        enabled = settings.soundsEnabled,
                        onSelect = { viewModel.setCelebrationSound(it) },
                        onPreview = { viewModel.previewCelebrationSound(it) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Workout Session",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Undo Last Set")
                            Text(
                                text = "Show an undo button during a session after a completed set.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.undoLastSetEnabled,
                            onCheckedChange = { viewModel.toggleUndoLastSet(it) }
                        )
                    }

                    TimerSettingRow(
                        label = "Rest timer",
                        value = settings.restTimerDuration,
                        description = "Used after a normal set.",
                        onClick = { showRestTimerDialog = true }
                    )

                    TimerSettingRow(
                        label = "Exercise switch timer",
                        value = settings.exerciseSwitchDuration,
                        description = "Used after the last set of an exercise.",
                        onClick = { showSwitchTimerDialog = true }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Calorie intensity")
                        Text(
                            text = "Adjusts calorie estimates when your workouts are usually easier or harder than average.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("easy" to "Easy", "normal" to "Normal", "hard" to "Hard").forEach { (value, label) ->
                                FilterChip(
                                    selected = settings.calorieIntensity == value,
                                    onClick = { viewModel.setCalorieIntensity(value) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Theme Settings Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("dark", "light", "auto").forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.capitalize()) }
                            )
                        }
                    }
                    
                    Text(
                        text = "Theme changes apply immediately on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ESP Rep Counter Sensor Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESP Rep Counter",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Sensor")
                        Switch(
                            checked = settings.sensorEnabled,
                            onCheckedChange = { viewModel.toggleSensor(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var ipAddress by remember { mutableStateOf(settings.sensorIpAddress) }
                    LaunchedEffect(settings.sensorIpAddress) {
                        ipAddress = settings.sensorIpAddress
                    }

                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("Sensor IP Address") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.sensorEnabled,
                        singleLine = true,
                        placeholder = { Text("192.168.0.125") }
                    )

                    val connectionState by viewModel.sensorConnectionState.collectAsState()
                    val sensorActionInProgress = connectionState == "Testing..." || connectionState == "Searching local network..."
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setSensorIpAddress(ipAddress) },
                            modifier = Modifier.weight(1f),
                            enabled = settings.sensorEnabled && ipAddress != settings.sensorIpAddress && !sensorActionInProgress
                        ) {
                            Text("Save IP")
                        }
                        OutlinedButton(
                            onClick = { viewModel.discoverSensor() },
                            modifier = Modifier.weight(1f),
                            enabled = settings.sensorEnabled && !sensorActionInProgress
                        ) {
                            Text("Find ESP")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.testSensorConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.sensorEnabled && !sensorActionInProgress
                    ) {
                        Text(connectionState ?: "Test Connection")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Use Find ESP while your phone is on the same Wi-Fi as the sensor. Manual IP remains available as a fallback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Data Management Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.exportData { csvContent ->
                                // Save to file and share
                                val file = File(context.cacheDir, "workout_export.csv")
                                file.writeText(csvContent)
                                
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Workout Data"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Data (CSV)")
                    }
                }
            }
            
            // About Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Workout Tracker v${com.example.workoutapp.BuildConfig.VERSION_NAME}")
                    Text("Developed by Milan Ples @2025")
                }
            }
        }
    }
}

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }

private data class SoundOption(
    val id: String,
    val label: String,
    val description: String
)

private data class ChoiceOption(
    val id: String,
    val label: String
)

private val timerSoundOptions = listOf(
    SoundOption("beep", "Beep", "Short and minimal countdown cue."),
    SoundOption("chime", "Chime", "Softer tone for quieter training."),
    SoundOption("loud", "Loud", "Sharper alert for noisy rooms.")
)

private val celebrationSoundOptions = listOf(
    SoundOption("cheer", "Cheer", "Upbeat finish cue."),
    SoundOption("victory", "Victory", "Bigger session-complete moment.")
)

private val silentModeOptions = listOf(
    ChoiceOption("respect", "Respect"),
    ChoiceOption("always", "Always play"),
    ChoiceOption("vibrate", "Vibrate only")
)

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChipSelector(
    title: String,
    description: String,
    options: List<ChoiceOption>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selectedId == option.id,
                    onClick = { onSelect(option.id) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundOptionSelector(
    title: String,
    description: String,
    options: List<SoundOption>,
    selectedId: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit
) {
    val selectedOption = options.firstOrNull { it.id == selectedId } ?: options.first()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selectedOption.id == option.id,
                    onClick = { onSelect(option.id) },
                    label = { Text(option.label) },
                    enabled = enabled,
                    leadingIcon = if (selectedOption.id == option.id) {
                        {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                enabled = enabled,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedOption.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = { onPreview(selectedOption.id) },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Preview")
            }
        }
    }
}

@Composable
private fun TimerSettingRow(
    label: String,
    value: Int,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onClick) {
            Text("${value}s")
        }
    }
}

@Composable
private fun TimerSettingDialog(
    title: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Seconds") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val seconds = value.toIntOrNull()?.coerceAtLeast(0) ?: currentValue
                    onSave(seconds)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
