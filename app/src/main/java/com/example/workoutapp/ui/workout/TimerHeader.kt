package com.example.workoutapp.ui.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutapp.domain.session.CountdownType
import com.example.workoutapp.ui.theme.NeonGreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimerHeader(
    seconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    timerType: CountdownType,
    restTimerDuration: Int,
    exerciseSwitchDuration: Int,
    onStartRest: () -> Unit,
    onStartExerciseSwitch: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipTimer: () -> Unit,
    onSetRestDuration: (Int) -> Unit,
    onSetExerciseSwitchDuration: (Int) -> Unit
) {
    var showRestDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    val showCountdown = isRunning || isPaused

    // Flash effect confined to the digits badge
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning && seconds <= 3 && seconds > 0) 0.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

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
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showCountdown && timerType != CountdownType.NONE) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (timerType) {
                                CountdownType.SWITCH -> MaterialTheme.colorScheme.tertiaryContainer
                                CountdownType.HOLD -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = when (timerType) {
                                    CountdownType.SWITCH -> "SWITCH"
                                    CountdownType.HOLD -> "HOLD"
                                    else -> "REST"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isRunning && seconds <= 3 && seconds > 0) {
                            NeonGreen.copy(alpha = flashAlpha)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Text(
                            text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (showCountdown) NeonGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isRunning) {
                        Button(
                            onClick = onSkipTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("SKIP")
                        }
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Text("PAUSE")
                        }
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("STOP")
                        }
                    } else if (isPaused) {
                        Button(
                            onClick = onSkipTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("SKIP")
                        }
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                        ) {
                            Text("RESUME")
                        }
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("STOP")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
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
                                        text = "Rest: ${restTimerDuration}s",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = { showRestDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit rest timer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
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
                                        text = "Switch: ${exerciseSwitchDuration}s",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = { showExerciseDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit switch timer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showCountdown) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = onStartRest,
                        label = { Text("Rest: ${restTimerDuration}s") }
                    )
                    AssistChip(
                        onClick = onStartExerciseSwitch,
                        label = { Text("Switch: ${exerciseSwitchDuration}s") }
                    )
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
