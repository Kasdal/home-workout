package com.example.workoutapp.ui.workout

import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.domain.session.CountdownType
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.navigation.Screen
import com.example.workoutapp.ui.theme.NeonGreen
import com.example.workoutapp.util.formatDuration
import com.example.workoutapp.util.formatKg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompleteSessionDialog(
    onConfirm: (Int?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var sessionRpe by remember { mutableStateOf<Int?>(null) }
    var sessionNotes by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How hard was this session? (RPE 1-10, optional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    (1..10).forEach { value ->
                        FilterChip(
                            selected = sessionRpe == value,
                            onClick = { sessionRpe = if (sessionRpe == value) null else value },
                            label = { Text("$value") }
                        )
                    }
                }
                OutlinedTextField(
                    value = sessionNotes,
                    onValueChange = { sessionNotes = it },
                    label = { Text("Session notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(sessionRpe, sessionNotes.ifBlank { null }) }) {
                Text("Save Session")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndOfExerciseDialog(
    exerciseName: String,
    onConfirm: (Int?, String?) -> Unit,
    onSkipPrompt: () -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseRpe by remember { mutableStateOf<Int?>(null) }
    var exerciseNote by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How did $exerciseName feel?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How hard was it? (RPE 1-10, optional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    (1..10).forEach { value ->
                        FilterChip(
                            selected = exerciseRpe == value,
                            onClick = { exerciseRpe = if (exerciseRpe == value) null else value },
                            label = { Text("$value") }
                        )
                    }
                }
                OutlinedTextField(
                    value = exerciseNote,
                    onValueChange = { exerciseNote = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(exerciseRpe, exerciseNote.ifBlank { null }) }) {
                Text("Save & continue")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onSkipPrompt) {
                Text("Skip prompt")
            }
        }
    )
}

/**
 * Intercepts system Back for as long as a workout session is running.
 *
 * `WorkoutViewModel` is scoped to the workout navigation entry, so a Back press
 * pops that entry, Hilt clears the ViewModel, and every field of the in-progress
 * session is destroyed with no confirmation and no way to recover. Completed sets,
 * RPE, notes and the exercise queue all vanish from a single mis-tap.
 *
 * While a session is active this handler consumes Back and pauses instead. The
 * session stays intact and the user returns through the existing Resume control.
 * When no session is running the handler is disabled, so Back navigates normally.
 */
@Composable
internal fun WorkoutSessionBackHandler(
    sessionStarted: Boolean,
    isSessionPaused: Boolean,
    onPauseRequested: () -> Unit
) {
    BackHandler(enabled = sessionStarted) {
        if (!isSessionPaused) {
            onPauseRequested()
        }
    }
}

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val timerTotalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isTimerPaused by viewModel.isTimerPaused.collectAsStateWithLifecycle()
    val completedSets by viewModel.completedSets.collectAsStateWithLifecycle()
    val sessionStarted by viewModel.sessionStarted.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val warmUpOnlyIds by viewModel.warmUpOnlyExerciseIds.collectAsStateWithLifecycle()
    val exerciseRpeMap by viewModel.exerciseRpe.collectAsStateWithLifecycle()
    val exerciseNotesMap by viewModel.exerciseNotes.collectAsStateWithLifecycle()
    val sessionElapsedSeconds by viewModel.sessionElapsedSeconds.collectAsStateWithLifecycle()
    val undoLastSetEnabled by viewModel.undoLastSetEnabled.collectAsStateWithLifecycle()
    val sensorReps by viewModel.sensorReps.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
    val sensorDistance by viewModel.sensorDistance.collectAsStateWithLifecycle()
    val sensorConnected by viewModel.sensorConnected.collectAsStateWithLifecycle()
    val activeExerciseId by viewModel.activeExerciseId.collectAsStateWithLifecycle()
    val activeExerciseMode by viewModel.activeExerciseMode.collectAsStateWithLifecycle()
    val sensorFallbackDismissed by viewModel.sensorFallbackDismissed.collectAsStateWithLifecycle()
    val showExerciseCounter by viewModel.showExerciseCounter.collectAsStateWithLifecycle()
    val showInlineRpe by viewModel.showInlineRpe.collectAsStateWithLifecycle()
    val showExerciseNotes by viewModel.showExerciseNotes.collectAsStateWithLifecycle()
    val sessionExercises by viewModel.sessionExercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val skippedExerciseIds by viewModel.skippedExerciseIds.collectAsStateWithLifecycle()
    val isSessionPaused by viewModel.isSessionPaused.collectAsStateWithLifecycle()
    val timerType by viewModel.timerType.collectAsStateWithLifecycle()

    var showSummary by remember { mutableStateOf(false) }
    var lastSession by remember { mutableStateOf<WorkoutSession?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(sessionStarted) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        if (sessionStarted) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // System Back must never pop the workout destination while a session is
    // running. See WorkoutSessionBackHandler for why.
    WorkoutSessionBackHandler(
        sessionStarted = sessionStarted,
        isSessionPaused = isSessionPaused,
        onPauseRequested = viewModel::pauseSession
    )

    LaunchedEffect(Unit) {
        viewModel.sessionStartErrors.collect { error ->
            when (error) {
                SessionStartError.NoActiveExercises -> snackbarHostState.showSnackbar(
                    message = "No exercises are marked active. Toggle a few in the library.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncErrorEvents.collect { event ->
            snackbarScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = if (event.canRetry) {
                        "Sync failed. Changes were not saved."
                    } else {
                        "Workout not saved. Tap Finish to try again."
                    },
                    actionLabel = if (event.canRetry) "Retry" else null,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.retryPendingWrites()
                }
            }
        }
    }

    if (showSummary && lastSession != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSummary = false },
            title = { Text("Workout Completed! 💪") },
            text = {
                Column {
                    Text("Total Weight: ${formatKg(lastSession?.totalWeightLifted ?: 0f)}")
                    Text("Calories Burned: ${String.format("%.1f", lastSession?.caloriesBurned)} kcal")
                    Text("Total Time: ${formatDuration(lastSession?.durationSeconds ?: 0L)}")
                    if (!lastSession?.skippedExerciseNames.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Skipped: " + lastSession?.skippedExerciseNames?.joinToString(", "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSummary = false }) {
                    Text("Awesome!")
                }
            }
        )
    }

    WorkoutScreenContent(
        exercises = exercises,
        sessionExercises = sessionExercises,
        timerSeconds = timerSeconds,
        timerTotalSeconds = timerTotalSeconds,
        isTimerRunning = isTimerRunning,
        isTimerPaused = isTimerPaused,
        completedSets = completedSets,
        sessionStarted = sessionStarted,
        sessionElapsedSeconds = sessionElapsedSeconds,
        undoLastSetEnabled = undoLastSetEnabled,
        snackbarHostState = snackbarHostState,
        onNavigate = { route -> navController.navigate(route) },
        onOpenLibrary = { navController.navigate(Screen.Workouts.route) },
        onStartSession = { viewModel.startSession() },
        onStartSessionWithTemplate = { viewModel.startSession(it) },
        templates = templates,
        onCompleteSession = {
            viewModel.completeSession(
                onComplete = { session ->
                    lastSession = session
                    showSummary = true
                }
            )
        },
        onCompleteSessionWithDetails = { rpe, notes ->
            viewModel.completeSession(
                onComplete = { session ->
                    lastSession = session
                    showSummary = true
                },
                sessionRpe = rpe,
                sessionNotes = notes
            )
        },
        onCompleteNextSet = { viewModel.completeNextSet(it) },
        onUndoSet = { viewModel.undoSet(it) },
        getExerciseHistory = viewModel::getExerciseHistory,
        onSkipExercise = { viewModel.skipCurrentExercise() },
        onFinishExercise = { rpe, note -> viewModel.finishCurrentExercise(rpe, note) },
        onFallbackToManualReps = { viewModel.fallbackToManualReps() },
        skippedExerciseIds = skippedExerciseIds,
        isSessionPaused = isSessionPaused,
        onToggleSessionPause = {
            if (isSessionPaused) viewModel.resumeSession() else viewModel.pauseSession()
        },
        timerType = timerType,
        onSkipTimer = { viewModel.skipActiveTimer() },
        onUpdateExercise = { viewModel.updateExercise(it) },
        onRemoveExercisePhoto = { viewModel.removeExercisePhoto(it) },
        warmUpOnlyIds = warmUpOnlyIds,
        exerciseRpe = exerciseRpeMap,
        exerciseNotes = exerciseNotesMap,
        onWarmUpOnlyChange = viewModel::setExerciseWarmUpOnly,
        onExerciseRpeChange = viewModel::setExerciseRpe,
        onExerciseNoteChange = viewModel::setExerciseNote,
        onResetSensorCounter = {
            viewModel.resetSensorCounter()
            snackbarScope.launch {
                snackbarHostState.showSnackbar("Resetting sensor counter")
            }
        },
        sensorReps = sensorReps,
        sensorState = sensorState,
        sensorDistance = sensorDistance,
        sensorConnected = sensorConnected,
        activeExerciseId = activeExerciseId,
        activeExerciseMode = activeExerciseMode,
        sensorFallbackDismissed = sensorFallbackDismissed,
        showExerciseCounter = showExerciseCounter,
        showInlineRpe = showInlineRpe,
        showExerciseNotes = showExerciseNotes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    exercises: List<Exercise>,
    sessionExercises: List<Exercise>,
    timerSeconds: Int,
    timerTotalSeconds: Int = 0,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,
    completedSets: Map<Int, Int>,
    sessionStarted: Boolean,
    sessionElapsedSeconds: Int,
    undoLastSetEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    templates: List<WorkoutTemplate> = emptyList(),
    onStartSessionWithTemplate: (WorkoutTemplate?) -> Unit = {},
    warmUpOnlyIds: Set<Int> = emptySet(),
    exerciseRpe: Map<Int, Int?> = emptyMap(),
    exerciseNotes: Map<Int, String> = emptyMap(),
    onWarmUpOnlyChange: (Int, Boolean) -> Unit = { _, _ -> },
    onExerciseRpeChange: (Int, Int?) -> Unit = { _, _ -> },
    onExerciseNoteChange: (Int, String) -> Unit = { _, _ -> },
    onNavigate: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onStartSession: () -> Unit,
    onCompleteSession: () -> Unit,
    onCompleteNextSet: (Int) -> Unit,
    onUndoSet: (Int) -> Unit,
    getExerciseHistory: (String) -> Flow<List<SessionExercise>>,
    onSkipExercise: () -> Unit,
    onFinishExercise: (Int?, String?) -> Unit,
    onFallbackToManualReps: () -> Unit = {},
    skippedExerciseIds: Set<Int>,
    isSessionPaused: Boolean,
    onToggleSessionPause: () -> Unit,
    timerType: CountdownType,
    onSkipTimer: () -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onRemoveExercisePhoto: (Int) -> Unit,
    onCompleteSessionWithDetails: (Int?, String?) -> Unit = { _, _ -> },
    onResetSensorCounter: () -> Unit,
    sensorReps: Int,
    sensorState: String,
    sensorDistance: Int,
    sensorConnected: Boolean,
    activeExerciseId: Int?,
    activeExerciseMode: ExerciseSessionMode,
    sensorFallbackDismissed: Boolean = false,
    showExerciseCounter: Boolean = false,
    showInlineRpe: Boolean = false,
    showExerciseNotes: Boolean = false
) {
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showEndOfExerciseDialog by remember { mutableStateOf(false) }

    if (showEndOfExerciseDialog) {
        val pendingExercise = activeExerciseId?.let { id -> sessionExercises.firstOrNull { it.id == id } }
        EndOfExerciseDialog(
            exerciseName = pendingExercise?.name ?: "exercise",
            onConfirm = { rpe, note ->
                showEndOfExerciseDialog = false
                onFinishExercise(rpe, note)
            },
            onSkipPrompt = {
                showEndOfExerciseDialog = false
                onFinishExercise(null, null)
            },
            onDismiss = { showEndOfExerciseDialog = false }
        )
    }

    if (showCompleteDialog) {
        CompleteSessionDialog(
            onConfirm = { rpe, notes ->
                showCompleteDialog = false
                onCompleteSessionWithDetails(rpe, notes)
            },
            onDismiss = { showCompleteDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(currentRoute = "workout", onNavigate = onNavigate)
        },
        topBar = {
            Column {
                if (sessionStarted) {
                    Surface(
                        color = if (isSessionPaused) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isSessionPaused) "PAUSED" else "Session Time: ${String.format("%02d:%02d", sessionElapsedSeconds / 60, sessionElapsedSeconds % 60)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSessionPaused) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        NeonGreen
                                    }
                                )
                                if (isSessionPaused) {
                                    Text(
                                        text = "Session Time: ${String.format("%02d:%02d", sessionElapsedSeconds / 60, sessionElapsedSeconds % 60)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = onToggleSessionPause,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSessionPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSessionPaused) "Resume" else "Pause")
                            }
                        }
                    }

                    TimerHeader(
                        seconds = timerSeconds,
                        totalSeconds = timerTotalSeconds,
                        isRunning = isTimerRunning,
                        isPaused = isTimerPaused,
                        timerType = timerType,
                        onSkipTimer = onSkipTimer
                    )
                } else {
                    TopAppBar(
                        title = { Text("Workout") },
                        actions = {
                            Text(
                                text = "${exercises.size} exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (sessionStarted) {
            val activeExercise = activeExerciseId?.let { id ->
                sessionExercises.firstOrNull { it.id == id }
            } ?: sessionExercises.firstOrNull { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                setCount < exercise.sets && exercise.id !in skippedExerciseIds
            }

            if (activeExercise != null) {
                val setCount = completedSets[activeExercise.id] ?: 0
                val isCompleted = setCount >= activeExercise.sets

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showExerciseCounter) {
                        SessionProgressHeader(
                            exercises = sessionExercises,
                            completedSets = completedSets,
                            skippedExerciseIds = skippedExerciseIds,
                            activeExerciseId = activeExercise.id
                        )
                    }

                    ExerciseCard(
                        exercise = activeExercise,
                        completedSetCount = setCount,
                        isCompleted = isCompleted,
                        onCompleteSet = { onCompleteNextSet(activeExercise.id) },
                        onUndoSet = { onUndoSet(activeExercise.id) },
                        onUpdate = onUpdateExercise,
                        onDelete = {},
                        undoEnabled = undoLastSetEnabled,
                        cardMode = ExerciseCardMode.SESSION,
                        sensorReps = sensorReps,
                        sensorState = sensorState,
                        sensorDistance = sensorDistance,
                        sensorConnected = sensorConnected &&
                            activeExerciseId == activeExercise.id &&
                            activeExerciseMode == ExerciseSessionMode.SENSOR_REPS,
                        activeExerciseMode = activeExerciseMode,
                        onResetSensorCounter = onResetSensorCounter,
                        onRemovePhoto = { onRemoveExercisePhoto(activeExercise.id) },
                        historyProvider = getExerciseHistory,
                        warmUpOnly = activeExercise.id in warmUpOnlyIds,
                        onWarmUpOnlyChange = { onWarmUpOnlyChange(activeExercise.id, it) },
                        rpe = exerciseRpe[activeExercise.id],
                        onRpeChange = if (showInlineRpe) {
                            { onExerciseRpeChange(activeExercise.id, it) }
                        } else null,
                        note = exerciseNotes[activeExercise.id] ?: "",
                        onNoteChange = if (showExerciseNotes) {
                            { onExerciseNoteChange(activeExercise.id, it) }
                        } else null,
                        showHoldButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activeExerciseMode == ExerciseSessionMode.SENSOR_REPS &&
                        !sensorConnected &&
                        !sensorFallbackDismissed
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensor disconnected. Sets already done are safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onFallbackToManualReps) {
                                    Text("Use manual counting")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEndOfExerciseDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finish exercise")
                        }
                        OutlinedButton(
                            onClick = onSkipExercise,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip exercise")
                        }
                    }

                    HoldToCompleteButton(
                        completedSets = setCount,
                        totalSets = activeExercise.sets,
                        isHoldExercise = activeExercise.exerciseType == ExerciseType.HOLD.name,
                        onCompleteSet = { onCompleteNextSet(activeExercise.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "All exercises completed! 🎉",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = "COMPLETE SESSION",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Ready to train?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage your exercise library from Workouts tab, then start session here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
                        var templateMenuExpanded by remember { mutableStateOf(false) }
                        if (templates.isNotEmpty()) {
                            Box {
                                OutlinedButton(
                                    onClick = { templateMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Checklist, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedTemplate?.name ?: "All active exercises",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose template")
                                }
                                DropdownMenu(
                                    expanded = templateMenuExpanded,
                                    onDismissRequest = { templateMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All active exercises") },
                                        onClick = {
                                            selectedTemplate = null
                                            templateMenuExpanded = false
                                        }
                                    )
                                    templates.forEach { template ->
                                        DropdownMenuItem(
                                            text = { Text(template.name) },
                                            onClick = {
                                                selectedTemplate = template
                                                templateMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onStartSessionWithTemplate(selectedTemplate)
                                    selectedTemplate = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("START SESSION", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenLibrary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WORKOUT LIBRARY")
                            }
                        }
                    }
                }

                if (exercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No exercises yet. Add from Workout Library.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onOpenLibrary) {
                                Icon(Icons.Default.Book, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Workout Library")
                            }
                        }
                    }
                } else {
                    val previewExercises = exercises.take(8)

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 86.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(previewExercises) { exercise ->
                            ExerciseLibraryPreviewRow(
                                exercise = exercise,
                                onClick = onOpenLibrary
                            )
                        }
                        if (exercises.size > previewExercises.size) {
                            item {
                                Text(
                                    text = "Showing ${previewExercises.size} of ${exercises.size}. Manage all in Workout Library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryPreviewRow(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, fontWeight = FontWeight.Bold)
                Text(
                    text = when (exercise.exerciseType) {
                        ExerciseType.HOLD.name -> "${exercise.sets} sets × ${exercise.holdDurationSeconds}s hold"
                        ExerciseType.BODYWEIGHT.name -> "${exercise.sets} sets × ${exercise.reps} reps (bodyweight)"
                        else -> "${exercise.sets} sets × ${exercise.reps} reps @ ${formatKg(exercise.weight)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
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
            Text(
                text = "Edit in Library",
                style = MaterialTheme.typography.bodySmall,
                color = NeonGreen
            )
        }
    }
}

@Composable
private fun SessionProgressHeader(
    exercises: List<Exercise>,
    completedSets: Map<Int, Int>,
    skippedExerciseIds: Set<Int>,
    activeExerciseId: Int
) {
    val position = exercises.indexOfFirst { it.id == activeExerciseId } + 1

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Exercise $position of ${exercises.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            exercises.forEachIndexed { index, exercise ->
                val done = (completedSets[exercise.id] ?: 0) >= exercise.sets
                val skipped = exercise.id in skippedExerciseIds
                val active = exercise.id == activeExerciseId
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        active -> MaterialTheme.colorScheme.primary
                        done -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textDecoration = if (skipped) TextDecoration.LineThrough else null,
                        textAlign = TextAlign.Center,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
