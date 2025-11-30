package com.example.workoutapp.ui.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
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
import com.example.workoutapp.ui.theme.NeonGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    exercise: Exercise,
    completedSetCount: Int,
    isCompleted: Boolean,
    onCompleteSet: () -> Unit,
    onUndoSet: () -> Unit,
    onUpdate: (Exercise) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isHolding) 0.95f else 1f,
        animationSpec = tween(200), label = ""
    )

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / 500f).coerceIn(0f, 1f)
                
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
                onUpdate(exercise.copy(name = name, weight = weight, reps = reps, sets = sets))
                showEditDialog = false
            }
        )
    }

    // Hide if completed
    if (!isCompleted) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Exercise Name and Weight on Top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showEditDialog = true }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -5kg button
                            IconButton(
                                onClick = { onUpdate(exercise.copy(weight = (exercise.weight - 5f).coerceAtLeast(0f))) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "-5kg",
                                    tint = Color.Red
                                )
                            }
                            
                            Text(
                                text = "${exercise.weight} kg",
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // +5kg button
                            IconButton(
                                onClick = { onUpdate(exercise.copy(weight = exercise.weight + 5f)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "+5kg",
                                    tint = NeonGreen
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand"
                            )
                        }
                    }
                }

                // Checkmark indicators (dynamic based on exercise.sets)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until exercise.sets) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .padding(4.dp)
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
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Undo Last Set Button (visible when sets are completed)
                if (completedSetCount > 0 && completedSetCount < exercise.sets) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onUndoSet,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Undo Last Set")
                    }
                }

                // Large "Next Set" Button with Hold Progress
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
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
                        text = if (completedSetCount >= exercise.sets) "COMPLETED" else "NEXT SET",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (completedSetCount >= exercise.sets) Color.White else Color.Black,
                        modifier = Modifier.scale(scale)
                    )
                }

                // Expanded Content (Delete option)
                if (expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Divider()
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Exercise")
                        }
                    }
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
                    label = { Text("Reps per Set") },
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
