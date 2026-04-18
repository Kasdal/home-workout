package com.example.workoutapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sound Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Enable/Disable Sounds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Sounds")
                        Switch(
                            checked = settings.soundsEnabled,
                            onCheckedChange = { viewModel.toggleSounds(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Volume Slider
                    Text("Volume: ${(settings.soundVolume * 100).toInt()}%")
                    Slider(
                        value = settings.soundVolume,
                        onValueChange = { viewModel.setSoundVolume(it) },
                        enabled = settings.soundsEnabled
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Timer Sound Selection
                    Text("Timer Sound")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("beep", "chime", "loud").forEach { sound ->
                            FilterChip(
                                selected = settings.timerSoundType == sound,
                                onClick = { 
                                    viewModel.setTimerSound(sound)
                                    viewModel.previewTimerSound(sound)
                                },
                                label = { Text(sound.capitalize()) },
                                enabled = settings.soundsEnabled
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Celebration Sound
                    Text("Completion Sound")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("cheer", "victory", "congrats").forEach { sound ->
                            FilterChip(
                                selected = settings.celebrationSoundType == sound,
                                onClick = { 
                                    viewModel.setCelebrationSound(sound)
                                    viewModel.previewCelebrationSound(sound)
                                },
                                label = { Text(sound.capitalize()) },
                                enabled = settings.soundsEnabled
                            )
                        }
                    }
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
                        text = "Note: Theme changes take effect on app restart",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.setSensorIpAddress(ipAddress) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.sensorEnabled && ipAddress != settings.sensorIpAddress
                    ) {
                        Text("Save IP Address")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val connectionState by viewModel.sensorConnectionState.collectAsState()
                    Button(
                        onClick = { viewModel.testSensorConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.sensorEnabled
                    ) {
                        Text(connectionState ?: "Test Connection")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Note: Sensor will automatically count reps during workouts",
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
