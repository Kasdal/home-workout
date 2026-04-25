package com.example.workoutapp.ui.workouts

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.workout.ExerciseEditDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    navController: NavController,
    viewModel: com.example.workoutapp.ui.workout.WorkoutViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState(initial = emptyList())

    WorkoutsScreenContent(
        exercises = exercises,
        onNavigateToRoute = navController::navigate,
        onAddExercise = viewModel::addExercise,
        onUpdateExercise = viewModel::updateExercise,
        onDeleteExercise = viewModel::deleteExercise,
        onUpdateExercisePhoto = viewModel::updateExercisePhoto,
        getExerciseHistory = viewModel::getExerciseHistory,
        onReorderExercises = viewModel::updateExerciseOrder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreenContent(
    exercises: List<Exercise>,
    onNavigateToRoute: (String) -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onUpdateExercisePhoto: (Int, String) -> Unit,
    getExerciseHistory: (String) -> Flow<List<SessionExercise>>,
    onReorderExercises: (List<Exercise>) -> Unit
) {
    val context = LocalContext.current
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var showExerciseWizard by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var orderedExercises by remember { mutableStateOf(exercises) }

    LaunchedEffect(exercises, reorderMode) {
        if (!reorderMode) {
            orderedExercises = exercises
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedExerciseId?.let { exerciseId ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                onUpdateExercisePhoto(exerciseId, uri.toString())
            }
        }
    }

    if (showExerciseWizard) {
        ExerciseWizardDialog(
            onDismiss = { showExerciseWizard = false },
            onCreate = {
                onAddExercise(it)
                showExerciseWizard = false
            }
        )
    }

    fun enterReorderMode() {
        orderedExercises = exercises
        reorderMode = true
    }

    fun moveExerciseInOrder(exerciseId: Int, direction: Int) {
        val fromIndex = orderedExercises.indexOfFirst { it.id == exerciseId }
        val toIndex = fromIndex + direction
        if (fromIndex !in orderedExercises.indices || toIndex !in orderedExercises.indices) return

        orderedExercises = orderedExercises.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Library") },
                actions = {
                    Text(
                        text = "${exercises.size} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "workouts",
                onNavigate = onNavigateToRoute
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                FloatingActionButton(onClick = { showExerciseWizard = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No exercises in your library yet.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap + to add your first exercise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (reorderMode) {
                    ReorderModeBanner(
                        onDone = {
                            onReorderExercises(orderedExercises)
                            reorderMode = false
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(orderedExercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                        WorkoutLibraryItem(
                            exercise = exercise,
                            exerciseHistory = getExerciseHistory(exercise.name),
                            canMoveUp = index > 0,
                            canMoveDown = index < orderedExercises.lastIndex,
                            reorderMode = reorderMode,
                            onEnterReorderMode = ::enterReorderMode,
                            onUpdate = onUpdateExercise,
                            onDelete = { onDeleteExercise(exercise.id) },
                            onMoveUp = { moveExerciseInOrder(exercise.id, -1) },
                            onMoveDown = { moveExerciseInOrder(exercise.id, 1) },
                            onUploadPhoto = {
                                selectedExerciseId = exercise.id
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutLibraryItem(
    exercise: Exercise,
    exerciseHistory: Flow<List<SessionExercise>>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    reorderMode: Boolean,
    onEnterReorderMode: () -> Unit,
    onUpdate: (Exercise) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var dragDistance by remember { mutableStateOf(0f) }
    var cardHeightPx by remember { mutableStateOf(0f) }
    val touchSlop = LocalViewConfiguration.current.touchSlop

    LaunchedEffect(reorderMode) {
        dragDistance = 0f
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise?") },
            text = { Text("Remove ${exercise.name} from your workout library?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDetailsDialog) {
        val history by exerciseHistory.collectAsState(initial = emptyList())
        ExerciseDetailsDialog(
            exercise = exercise,
            history = history,
            onDismiss = { showDetailsDialog = false }
        )
    }

    val dragState = rememberDraggableState { delta ->
        dragDistance += delta
        val reorderThreshold = (cardHeightPx * 0.5f).coerceAtLeast(120f)
        when {
            dragDistance <= -reorderThreshold && canMoveUp -> {
                dragDistance = 0f
                onMoveUp()
            }
            dragDistance >= reorderThreshold && canMoveDown -> {
                dragDistance = 0f
                onMoveDown()
            }
        }
    }

    val interactionModifier = if (reorderMode) {
        Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            enabled = true,
            onDragStopped = { dragDistance = 0f }
        )
    } else {
        Modifier.pointerInput(exercise.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var movedLikeScroll = false
                val upOrMoveBeforeHold = withTimeoutOrNull(3000) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull null
                        if (!change.pressed) return@withTimeoutOrNull change
                        if ((change.position - down.position).getDistance() > touchSlop) {
                            movedLikeScroll = true
                            return@withTimeoutOrNull change
                        }
                    }
                }
                if (upOrMoveBeforeHold == null && !movedLikeScroll) {
                    onEnterReorderMode()
                    waitForUpOrCancellation()
                } else if (!movedLikeScroll) {
                    showDetailsDialog = true
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(interactionModifier)
            .onGloballyPositioned { cardHeightPx = it.size.height.toFloat() }
            .offset { IntOffset(x = 0, y = if (reorderMode) dragDistance.roundToInt() else 0) }
            .zIndex(if (reorderMode && dragDistance != 0f) 1f else 0f)
            .border(
                width = if (reorderMode) 1.dp else 0.dp,
                color = if (reorderMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (reorderMode) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = when (exercise.exerciseType) {
                            ExerciseType.HOLD.name -> "${exercise.sets} sets × ${exercise.holdDurationSeconds}s hold"
                            ExerciseType.BODYWEIGHT.name -> "${exercise.sets} sets × ${exercise.reps} reps (bodyweight)"
                            else -> "${exercise.sets} sets × ${exercise.reps} reps @ ${exercise.weight}kg"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = when (exercise.exerciseType) {
                                ExerciseType.HOLD.name -> "Hold"
                                ExerciseType.BODYWEIGHT.name -> "Bodyweight"
                                else -> "Standard"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (exercise.usesSensor) {
                            Text(
                                text = "Sensor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (reorderMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Drag",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (exercise.photoUri != null) {
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (reorderMode) {
                Text(
                    text = "Drag this card up or down to move it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showDetailsDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Details")
                    }
                    Button(onClick = onUploadPhoto, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo")
                    }
                    Button(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Exercise")
                }
            }
        }
    }
}

@Composable
private fun ReorderModeBanner(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reorder mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Drag exercises up or down. Tap Done when finished.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onDone) {
            Text("Done")
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(
    exercise: Exercise,
    history: List<SessionExercise>,
    onDismiss: () -> Unit
) {
    val recentHistory = history.sortedByDescending { it.sessionId }
    val lastEntry = recentHistory.firstOrNull()
    val bestWeight = history.maxOfOrNull { it.weight } ?: 0f
    val bestVolume = history.maxOfOrNull { it.volume } ?: 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DetailSection(title = "Current Setup") {
                    DetailText(exerciseSummary(exercise))
                    DetailText(
                        when (exercise.exerciseType) {
                            ExerciseType.HOLD.name -> "Hold exercise"
                            ExerciseType.BODYWEIGHT.name -> "Bodyweight exercise"
                            else -> "Weighted exercise"
                        }
                    )
                    DetailText(if (exercise.usesSensor) "Sensor enabled" else "Manual tracking")
                }

                DetailSection(title = "History") {
                    if (history.isEmpty()) {
                        DetailText("No completed sessions yet.")
                    } else {
                        DetailText("Completed ${history.size} time${if (history.size == 1) "" else "s"}")
                        DetailText("Last: ${lastEntry?.sets ?: 0} x ${lastEntry?.reps ?: 0} @ ${formatKg(lastEntry?.weight ?: 0f)}")
                        DetailText("Best weight: ${formatKg(bestWeight)}")
                        DetailText("Best volume: ${formatKg(bestVolume)}")
                    }
                }

                if (recentHistory.isNotEmpty()) {
                    DetailSection(title = "Recent Performances") {
                        recentHistory.take(5).forEach { entry ->
                            DetailText("${entry.sets} x ${entry.reps} @ ${formatKg(entry.weight)} - ${formatKg(entry.volume)} volume")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun DetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun exerciseSummary(exercise: Exercise): String {
    return when (exercise.exerciseType) {
        ExerciseType.HOLD.name -> "${exercise.sets} sets x ${exercise.holdDurationSeconds}s hold"
        ExerciseType.BODYWEIGHT.name -> "${exercise.sets} sets x ${exercise.reps} reps (bodyweight)"
        else -> "${exercise.sets} sets x ${exercise.reps} reps @ ${formatKg(exercise.weight)}"
    }
}

private fun formatKg(value: Float): String {
    return if (value % 1f == 0f) {
        "${value.toInt()}kg"
    } else {
        "${String.format(Locale.US, "%.1f", value)}kg"
    }
}
