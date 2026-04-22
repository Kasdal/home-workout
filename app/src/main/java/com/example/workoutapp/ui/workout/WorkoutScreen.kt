package com.example.workoutapp.ui.workout

import android.view.WindowManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.navigation.Screen
import com.example.workoutapp.ui.theme.NeonGreen

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState(initial = emptyList())
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val isTimerPaused by viewModel.isTimerPaused.collectAsState()
    val completedSets by viewModel.completedSets.collectAsState()
    val sessionStarted by viewModel.sessionStarted.collectAsState()
    val sessionElapsedSeconds by viewModel.sessionElapsedSeconds.collectAsState()
    val restTimerDuration by viewModel.restTimerDuration.collectAsState()
    val exerciseSwitchDuration by viewModel.exerciseSwitchDuration.collectAsState()
    val undoLastSetEnabled by viewModel.undoLastSetEnabled.collectAsState()
    val sensorReps by viewModel.sensorReps.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val sensorDistance by viewModel.sensorDistance.collectAsState()
    val sensorConnected by viewModel.sensorConnected.collectAsState()
    val activeExerciseId by viewModel.activeExerciseId.collectAsState()
    val activeExerciseMode by viewModel.activeExerciseMode.collectAsState()

    var showSummary by remember { mutableStateOf(false) }
    var lastSession by remember { mutableStateOf<WorkoutSession?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
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

    if (showSummary && lastSession != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSummary = false },
            title = { Text("Workout Completed! 💪") },
            text = {
                Column {
                    Text("Total Weight: ${lastSession?.totalWeightLifted} kg")
                    Text("Calories Burned: ${String.format("%.1f", lastSession?.caloriesBurned)} kcal")
                    Text("Total Time: ${lastSession?.durationSeconds?.div(60)} mins")
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
        timerSeconds = timerSeconds,
        isTimerRunning = isTimerRunning,
        isTimerPaused = isTimerPaused,
        completedSets = completedSets,
        sessionStarted = sessionStarted,
        sessionElapsedSeconds = sessionElapsedSeconds,
        restTimerDuration = restTimerDuration,
        exerciseSwitchDuration = exerciseSwitchDuration,
        undoLastSetEnabled = undoLastSetEnabled,
        snackbarHostState = snackbarHostState,
        onNavigate = { route -> navController.navigate(route) },
        onOpenLibrary = { navController.navigate(Screen.Workouts.route) },
        onStartSession = { viewModel.startSession() },
        onCompleteSession = {
            viewModel.completeSession { session ->
                lastSession = session
                showSummary = true
            }
        },
        onCompleteNextSet = { viewModel.completeNextSet(it) },
        onUndoSet = { viewModel.undoSet(it) },
        onStartTimer = { viewModel.startTimer(it) },
        onPauseTimer = { viewModel.pauseTimer() },
        onResumeTimer = { viewModel.resumeTimer() },
        onStopTimer = { viewModel.stopTimer() },
        onSetRestDuration = { viewModel.setRestTimerDuration(it) },
        onSetExerciseSwitchDuration = { viewModel.setExerciseSwitchDuration(it) },
        sensorReps = sensorReps,
        sensorState = sensorState,
        sensorDistance = sensorDistance,
        sensorConnected = sensorConnected,
        activeExerciseId = activeExerciseId,
        activeExerciseMode = activeExerciseMode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    exercises: List<Exercise>,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,
    completedSets: Map<Int, Int>,
    sessionStarted: Boolean,
    sessionElapsedSeconds: Int,
    restTimerDuration: Int,
    exerciseSwitchDuration: Int,
    undoLastSetEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onNavigate: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onStartSession: () -> Unit,
    onCompleteSession: () -> Unit,
    onCompleteNextSet: (Int) -> Unit,
    onUndoSet: (Int) -> Unit,
    onStartTimer: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onSetRestDuration: (Int) -> Unit,
    onSetExerciseSwitchDuration: (Int) -> Unit,
    sensorReps: Int,
    sensorState: String,
    sensorDistance: Int,
    sensorConnected: Boolean,
    activeExerciseId: Int?,
    activeExerciseMode: ExerciseSessionMode
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(currentRoute = "workout", onNavigate = onNavigate)
        },
        topBar = {
            Column {
                if (sessionStarted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Session Time: ${String.format("%02d:%02d", sessionElapsedSeconds / 60, sessionElapsedSeconds % 60)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }

                    TimerHeader(
                        seconds = timerSeconds,
                        isRunning = isTimerRunning,
                        isPaused = isTimerPaused,
                        restTimerDuration = restTimerDuration,
                        exerciseSwitchDuration = exerciseSwitchDuration,
                        onStartRest = { onStartTimer(restTimerDuration) },
                        onStartExerciseSwitch = { onStartTimer(exerciseSwitchDuration) },
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onStop = onStopTimer,
                        onSetRestDuration = onSetRestDuration,
                        onSetExerciseSwitchDuration = onSetExerciseSwitchDuration
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
        },
        floatingActionButton = {}
    ) { padding ->
        if (sessionStarted) {
            val completedExercises = exercises.filter { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                setCount >= exercise.sets
            }
            val activeExercise = exercises.firstOrNull { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                setCount < exercise.sets
            }
            val visibleCompletedExercises = completedExercises.takeLast(1)

            if (activeExercise != null) {
                val setCount = completedSets[activeExercise.id] ?: 0
                val isCompleted = setCount >= activeExercise.sets

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleCompletedExercises.forEach { exercise ->
                        val completedSetCount = completedSets[exercise.id] ?: 0
                        ExerciseCard(
                            exercise = exercise,
                            completedSetCount = completedSetCount,
                            isCompleted = true,
                            onCompleteSet = {},
                            onUndoSet = {},
                            onUpdate = {},
                            onDelete = {},
                            undoEnabled = false,
                            cardMode = ExerciseCardMode.LIST_COMPACT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ExerciseCard(
                        exercise = activeExercise,
                        completedSetCount = setCount,
                        isCompleted = isCompleted,
                        onCompleteSet = { onCompleteNextSet(activeExercise.id) },
                        onUndoSet = { onUndoSet(activeExercise.id) },
                        onUpdate = {},
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onCompleteSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "COMPLETE SESSION",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                        onClick = onCompleteSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onStartSession,
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
                        else -> "${exercise.sets} sets × ${exercise.reps} reps @ ${exercise.weight}kg"
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
