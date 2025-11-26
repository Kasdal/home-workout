package com.example.workoutapp.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutapp.ui.theme.NeonGreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimerHeader(
    seconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    restTimerDuration: Int,
    exerciseSwitchDuration: Int,
    onStartRest: () -> Unit,
    onStartExerciseSwitch: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSetRestDuration: (Int) -> Unit,
    onSetExerciseSwitchDuration: (Int) -> Unit
) {
    var showRestDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    if (showRestDialog) {
        TimerCustomDialog(
            title = "Set Rest Timer",
            currentValue = restTimerDuration,
            onDismiss = { showRestDialog = false },
            onSave = { 
                onSetRestDuration(it)
                showRestDialog = false
            }
        )
    }

    if (showExerciseDialog) {
        TimerCustomDialog(
            title = "Set Exercise Switch Timer",
            currentValue = exerciseSwitchDuration,
            onDismiss = { showExerciseDialog = false },
            onSave = { 
                onSetExerciseSwitchDuration(it)
                showExerciseDialog = false
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = when {
                        isRunning -> "RESTING"
                        isPaused -> "PAUSED"
                        else -> "READY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRunning || isPaused) NeonGreen else Color.Gray
                )
                Text(
                    text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning || isPaused) NeonGreen else MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRunning) {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                    ) {
                        Text("PAUSE")
                    }
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("STOP")
                    }
                } else if (isPaused) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("RESUME")
                    }
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("STOP")
                    }
                } else {
                    // Rest timer button
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = onStartRest,
                                onLongClick = { showRestDialog = true }
                            )
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "${restTimerDuration}s",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Exercise switch timer button
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = onStartExerciseSwitch,
                                onLongClick = { showExerciseDialog = true }
                            )
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "${exerciseSwitchDuration}s",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerCustomDialog(
    title: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Seconds") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val seconds = value.toIntOrNull() ?: currentValue
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
