package com.example.workoutapp.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseType

@Composable
fun ExerciseWizardDialog(
    onDismiss: () -> Unit,
    onCreate: (Exercise) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var exerciseType by remember { mutableStateOf(ExerciseType.STANDARD) }
    var usesSensor by remember { mutableStateOf(true) }
    var weightText by remember { mutableStateOf("20") }
    var repsText by remember { mutableStateOf("13") }
    var setsText by remember { mutableStateOf("4") }
    var holdDurationText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Exercise (${step}/3)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            when (step) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Exercise Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Quick option:", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = {
                                if (name.isBlank()) name = "New Exercise"
                                onCreate(
                                    Exercise(
                                        name = name.ifBlank { "New Exercise" },
                                        weight = 20f,
                                        reps = 13,
                                        sets = 4,
                                        exerciseType = ExerciseType.STANDARD.name,
                                        usesSensor = true,
                                        holdDurationSeconds = 30
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Default (Sensor Standard)")
                        }
                    }
                }

                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Exercise Type")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = exerciseType == ExerciseType.STANDARD,
                                onClick = { exerciseType = ExerciseType.STANDARD },
                                label = { Text("Standard") }
                            )
                            FilterChip(
                                selected = exerciseType == ExerciseType.BODYWEIGHT,
                                onClick = { exerciseType = ExerciseType.BODYWEIGHT },
                                label = { Text("Bodyweight") }
                            )
                            FilterChip(
                                selected = exerciseType == ExerciseType.HOLD,
                                onClick = { exerciseType = ExerciseType.HOLD },
                                label = { Text("Hold") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Use Sensor")
                            Switch(
                                checked = usesSensor,
                                onCheckedChange = { usesSensor = it },
                                enabled = exerciseType != ExerciseType.HOLD
                            )
                        }

                        if (exerciseType == ExerciseType.HOLD) {
                            Text(
                                "Hold exercises ignore reps and sensor, and use hold duration per set.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = setsText,
                            onValueChange = { setsText = it },
                            label = { Text("Sets") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (exerciseType == ExerciseType.HOLD) {
                            OutlinedTextField(
                                value = holdDurationText,
                                onValueChange = { holdDurationText = it },
                                label = { Text("Hold Duration (seconds)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            OutlinedTextField(
                                value = repsText,
                                onValueChange = { repsText = it },
                                label = { Text("Reps") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            if (exerciseType == ExerciseType.STANDARD) {
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    label = { Text("Weight (kg)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step > 1) {
                    TextButton(onClick = { step-- }) {
                        Text("Back")
                    }
                }

                if (step < 3) {
                    Button(
                        onClick = { step++ },
                        enabled = if (step == 1) name.isNotBlank() else true
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = {
                            val sets = setsText.toIntOrNull()?.coerceAtLeast(1) ?: 4
                            val reps = repsText.toIntOrNull()?.coerceAtLeast(1) ?: 13
                            val holdDuration = holdDurationText.toIntOrNull()?.coerceAtLeast(5) ?: 30
                            val weight = weightText.toFloatOrNull()?.coerceAtLeast(0f) ?: 20f

                            onCreate(
                                Exercise(
                                    name = name,
                                    weight = when (exerciseType) {
                                        ExerciseType.BODYWEIGHT, ExerciseType.HOLD -> 0f
                                        ExerciseType.STANDARD -> weight
                                    },
                                    reps = if (exerciseType == ExerciseType.HOLD) 1 else reps,
                                    sets = sets,
                                    exerciseType = exerciseType.name,
                                    usesSensor = exerciseType != ExerciseType.HOLD && usesSensor,
                                    holdDurationSeconds = if (exerciseType == ExerciseType.HOLD) holdDuration else 30
                                )
                            )
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
