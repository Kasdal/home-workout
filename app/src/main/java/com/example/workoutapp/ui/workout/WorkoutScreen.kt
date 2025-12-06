package com.example.workoutapp.ui.workout

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.navigation.Screen
import com.example.workoutapp.ui.theme.NeonGreen
import kotlinx.coroutines.launch

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
    
    var showSummary by remember { mutableStateOf(false) }
    var lastSession by remember { mutableStateOf<com.example.workoutapp.data.local.entity.WorkoutSession?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Keep screen on during workout
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
    
    // Track exercise completions and show toast messages
    val previousCompletedExercises = remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    LaunchedEffect(completedSets) {
        val currentCompletedExercises = exercises.filter { exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            setCount >= exercise.sets
        }.map { it.id }.toSet()
        
        // Find newly completed exercises
        val newlyCompleted = currentCompletedExercises - previousCompletedExercises.value
        
        // Show toast for each newly completed exercise
        newlyCompleted.forEach { exerciseId ->
            val exercise = exercises.find { it.id == exerciseId }
            exercise?.let {
                snackbarHostState.showSnackbar(
                    message = "${it.name} completed! 💪",
                    duration = SnackbarDuration.Short
                )
            }
        }
        
        previousCompletedExercises.value = currentCompletedExercises
    }

    if (showSummary && lastSession != null) {
        AlertDialog(
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
        snackbarHostState = snackbarHostState,
        onNavigate = { route -> navController.navigate(route) },
        onStartSession = { viewModel.startSession() },
        onCompleteSession = {
            viewModel.completeSession { session ->
                lastSession = session
                showSummary = true
            }
        },
        onAddExercise = { viewModel.addExercise() },
        onCompleteNextSet = { viewModel.completeNextSet(it) },
        onUndoSet = { viewModel.undoSet(it) },
        onUpdateExercise = { viewModel.updateExercise(it) },
        onDeleteExercise = { viewModel.deleteExercise(it) },
        onStartTimer = { viewModel.startTimer(it) },
        onPauseTimer = { viewModel.pauseTimer() },
        onResumeTimer = { viewModel.resumeTimer() },
        onStopTimer = { viewModel.stopTimer() },
        onSetRestDuration = { viewModel.setRestTimerDuration(it) },
        onSetExerciseSwitchDuration = { viewModel.setExerciseSwitchDuration(it) }
    )
}

@Composable
fun WorkoutScreenContent(
    exercises: List<com.example.workoutapp.data.local.entity.Exercise>,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,
    completedSets: Map<Int, Int>,
    sessionStarted: Boolean,
    sessionElapsedSeconds: Int,
    restTimerDuration: Int,
    exerciseSwitchDuration: Int,
    snackbarHostState: SnackbarHostState,
    onNavigate: (String) -> Unit,
    onStartSession: () -> Unit,
    onCompleteSession: () -> Unit,
    onAddExercise: () -> Unit,
    onCompleteNextSet: (Int) -> Unit,
    onUndoSet: (Int) -> Unit,
    onUpdateExercise: (com.example.workoutapp.data.local.entity.Exercise) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onStartTimer: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onSetRestDuration: (Int) -> Unit,
    onSetExerciseSwitchDuration: (Int) -> Unit
) {
    // Removed ModalNavigationDrawer - using bottom nav instead
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                currentRoute = "workout",
                onNavigate = onNavigate
            )
        },
        topBar = {
            Column {
                // Removed hamburger menu - using bottom nav instead
                
                // Session Timer Display
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
            }
        },

        floatingActionButton = {
            if (!sessionStarted) {
                FloatingActionButton(onClick = onAddExercise) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        }
    ) { padding ->

        if (sessionStarted) {
            // Focus Mode: Show active exercise with completed exercises at top
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
                    // Show last 2 completed exercises at top in collapsed form
                    visibleCompletedExercises.forEach { exercise ->
                        val completedSetCount = completedSets[exercise.id] ?: 0
                        ExerciseCard(
                            exercise = exercise,
                            completedSetCount = completedSetCount,
                            isCompleted = true,
                            onCompleteSet = { },
                            onUndoSet = { },
                            onUpdate = { },
                            onDelete = { },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Show active exercise
                    ExerciseCard(
                        exercise = activeExercise,
                        completedSetCount = setCount,
                        isCompleted = isCompleted,
                        onCompleteSet = { onCompleteNextSet(activeExercise.id) },
                        onUndoSet = { onUndoSet(activeExercise.id) },
                        onUpdate = { onUpdateExercise(it) },
                        onDelete = { onDeleteExercise(activeExercise.id) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    
                    // COMPLETE SESSION Button
                    Button(
                        onClick = { onCompleteSession() },
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
                // All exercises completed in this session
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
                        onClick = { onCompleteSession() },
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
            // Normal Mode: List all exercises
            // Separate completed and incomplete exercises
            val completedExercises = exercises.filter { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                setCount >= exercise.sets
            }
            val incompleteExercises = exercises.filter { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                setCount < exercise.sets
            }
            
            // Show only last 2 completed exercises
            val visibleCompletedExercises = completedExercises.takeLast(1)
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Show last 2 completed exercises at the top
                items(visibleCompletedExercises) { exercise ->
                    val setCount = completedSets[exercise.id] ?: 0
                    val isCompleted = setCount >= exercise.sets
                    
                    ExerciseCard(
                        exercise = exercise,
                        completedSetCount = setCount,
                        isCompleted = isCompleted,
                        onCompleteSet = { },
                        onUndoSet = { },
                        onUpdate = { },
                        onDelete = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
                
                // Show all incomplete exercises
                items(incompleteExercises) { exercise ->
                    val setCount = completedSets[exercise.id] ?: 0
                    val isCompleted = setCount >= exercise.sets
                    
                    ExerciseCard(
                        exercise = exercise,
                        completedSetCount = setCount,
                        isCompleted = isCompleted,
                        onCompleteSet = { onCompleteNextSet(exercise.id) },
                        onUndoSet = { onUndoSet(exercise.id) },
                        onUpdate = { onUpdateExercise(it) },
                        onDelete = { onDeleteExercise(exercise.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
                
                // START SESSION / COMPLETE SESSION Button
                item {
                    Button(
                        onClick = {
                            if (sessionStarted) {
                                onCompleteSession()
                            } else {
                                onStartSession()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sessionStarted) Color.Red else NeonGreen,
                            contentColor = if (sessionStarted) Color.White else Color.Black
                        )
                    ) {
                        Text(
                            text = if (sessionStarted) "COMPLETE SESSION" else "START SESSION",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Footer
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Developed by Milan Ples @2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
