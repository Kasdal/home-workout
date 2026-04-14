package com.example.workoutapp.ui.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    exercise: Exercise,
    completedSetCount: Int,
    isCompleted: Boolean,
    onCompleteSet: () -> Unit,
    onUpdate: (Exercise) -> Unit,
    onDelete: () -> Unit,
    cardMode: ExerciseCardMode = ExerciseCardMode.LIST_COMPACT,
    sensorReps: Int = 0,
    sensorState: String = "REST",
    sensorDistance: Int = 0,
    sensorConnected: Boolean = false,
    onPhotoUpload: (() -> Unit)? = null,
    activeExerciseMode: ExerciseSessionMode? = null,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHolding) 0.95f else 1f,
        animationSpec = tween(200), label = ""
    )

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / 250f).coerceIn(0f, 1f)
                
                if (holdProgress >= 1f) {
                    onCompleteSet()
                    holdProgress = 0f
                    isHolding = false
                }
                delay(16)
            }
        } else {
            holdProgress = 0f
        }
    }

    if (showEditDialog) {
        ExerciseEditDialog(
            exercise = exercise,
            onDismiss = { showEditDialog = false },
                onSave = { name, weight, reps, sets ->
                onUpdate(
                    exercise.copy(
                        name = name,
                        weight = if (exercise.exerciseType == ExerciseType.BODYWEIGHT.name || exercise.exerciseType == ExerciseType.HOLD.name) 0f else weight,
                        reps = if (exercise.exerciseType == ExerciseType.HOLD.name) 1 else reps,
                        holdDurationSeconds = if (exercise.exerciseType == ExerciseType.HOLD.name) reps else exercise.holdDurationSeconds,
                        sets = sets
                    )
                )
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
            },
            title = { Text("Delete Exercise?") },
            text = { Text("Are you sure you want to remove ${exercise.name} from this workout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show completed exercises in collapsed form with green tint
    if (isCompleted) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = NeonGreen.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = exerciseSummary(exercise),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "✓ DONE",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        val cardHeightModifier = when (cardMode) {
            ExerciseCardMode.SESSION -> Modifier.fillMaxHeight()
            ExerciseCardMode.LIST_COMPACT -> Modifier.heightIn(min = 220.dp, max = 250.dp)
            ExerciseCardMode.LIST_EXPANDED -> Modifier.fillMaxHeight(0.7f)
        }

        Card(
            modifier = modifier.then(cardHeightModifier),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Compact header: Name, Weight controls, and Checkmarks in one row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Fixed height to prevent layout shift
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showEditDialog = true }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercise name
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    
                    // Weight controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUpdate(exercise.copy(weight = (exercise.weight - 5f).coerceAtLeast(0f))) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "-5kg",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Text(
                            text = "${exercise.weight} kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = { onUpdate(exercise.copy(weight = exercise.weight + 5f)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "+5kg",
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Checkmark indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until exercise.sets) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < completedSetCount) NeonGreen
                                        else Color.Gray.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (i < completedSetCount) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (cardMode != ExerciseCardMode.SESSION) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    when (exercise.exerciseType) {
                                        ExerciseType.HOLD.name -> "Hold"
                                        ExerciseType.BODYWEIGHT.name -> "Bodyweight"
                                        else -> "Standard"
                                    }
                                )
                            }
                        )
                        if (exercise.usesSensor) {
                            AssistChip(onClick = {}, label = { Text("Sensor") })
                        }
                        if (exercise.exerciseType == ExerciseType.HOLD.name) {
                            AssistChip(
                                onClick = {},
                                label = { Text("${exercise.holdDurationSeconds}s hold") },
                                leadingIcon = {
                                    Icon(Icons.Default.HourglassBottom, contentDescription = null)
                                }
                            )
                        }
                    }
                } else if (activeExerciseMode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when (activeExerciseMode) {
                                    ExerciseSessionMode.SENSOR_REPS -> "Sensor Tracked"
                                    ExerciseSessionMode.HOLD_TIMER -> "Hold Timer"
                                    ExerciseSessionMode.MANUAL_REPS -> "Manual Reps"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (activeExerciseMode) {
                                    ExerciseSessionMode.SENSOR_REPS -> Icons.Default.Sensors
                                    ExerciseSessionMode.HOLD_TIMER -> Icons.Default.HourglassBottom
                                    ExerciseSessionMode.MANUAL_REPS -> Icons.Default.Repeat
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // Photo area (shown in SESSION and LIST_EXPANDED modes)
                val photoSpaceHeight = when (cardMode) {
                    ExerciseCardMode.SESSION -> 200.dp
                    ExerciseCardMode.LIST_COMPACT -> 0.dp
                    ExerciseCardMode.LIST_EXPANDED -> 300.dp
                }
                
                if (photoSpaceHeight > 0.dp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(photoSpaceHeight)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.shapes.medium
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (exercise.photoUri != null) {
                            // Display photo with long-press menu
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { showPhotoMenu = true }
                                    )
                            ) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(exercise.photoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = exercise.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            
                            // Photo menu dropdown
                            DropdownMenu(
                                expanded = showPhotoMenu,
                                onDismissRequest = { showPhotoMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Replace Photo") },
                                    onClick = {
                                        showPhotoMenu = false
                                        onPhotoUpload?.invoke()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Image, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove Photo") },
                                    onClick = {
                                        showPhotoMenu = false
                                        onUpdate(exercise.copy(photoUri = null))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                            }
                        } else {
                            // Placeholder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "No photo",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                
                                if (cardMode == ExerciseCardMode.LIST_EXPANDED && onPhotoUpload != null) {
                                    OutlinedButton(onClick = onPhotoUpload) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Upload Photo")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No photo space in compact mode, just small spacer
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (cardMode == ExerciseCardMode.SESSION && sensorConnected) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (sensorState == "LIFTING") {
                                NeonGreen.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonGreen
                                )
                                Text(
                                    text = sensorState,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sensorState == "LIFTING") NeonGreen else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$sensorReps",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Text(
                                text = "reps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "Distance: ${sensorDistance}mm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (cardMode == ExerciseCardMode.LIST_EXPANDED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete exercise",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Exercise")
                    }
                }

                // Large "Next Set" Button with Hold Progress
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (completedSetCount >= exercise.sets) Color.Gray else NeonGreen) // Green when available
                        .pointerInput(completedSetCount) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    tryAwaitRelease()
                                    isHolding = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle overlay that appears during hold for better feedback
                    if (holdProgress > 0f && completedSetCount < exercise.sets) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = holdProgress * 0.15f))
                        )
                    }
                    
                    // Dark overlay that shrinks as you hold (visual feedback)
                    if (holdProgress > 0f && completedSetCount < exercise.sets) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(1f - holdProgress) // Shrinks from right
                                .fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .align(Alignment.CenterEnd)
                        )
                    }
                    
                    Text(
                        text = if (completedSetCount >= exercise.sets) "COMPLETED" else {
                            if (exercise.exerciseType == ExerciseType.HOLD.name) "NEXT HOLD" else "NEXT SET"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (completedSetCount >= exercise.sets) Color.White else Color.Black,
                        modifier = Modifier.scale(scale)
                    )
                }

            }
        }
    }
}

@Composable
fun ExerciseEditDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onSave: (String, Float, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(exercise.name) }
    var weight by remember { mutableStateOf(exercise.weight.toString()) }
    var reps by remember { mutableStateOf(exercise.reps.toString()) }
    var sets by remember { mutableStateOf(exercise.sets.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = {
                        Text(
                            if (exercise.exerciseType == ExerciseType.HOLD.name) {
                                "Hold Duration (sec)"
                            } else {
                                "Reps per Set"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Number of Sets") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toFloatOrNull() ?: exercise.weight
                    val r = reps.toIntOrNull() ?: exercise.reps
                    val s = sets.toIntOrNull() ?: exercise.sets
                    onSave(name, w, r, s)
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

private fun exerciseSummary(exercise: Exercise): String {
    return when (exercise.exerciseType) {
        ExerciseType.HOLD.name -> "${exercise.sets} sets × ${exercise.holdDurationSeconds}s hold"
        ExerciseType.BODYWEIGHT.name -> "${exercise.sets} sets × ${exercise.reps} reps (bodyweight)"
        else -> "${exercise.sets} sets × ${exercise.reps} reps @ ${exercise.weight}kg"
    }
}
