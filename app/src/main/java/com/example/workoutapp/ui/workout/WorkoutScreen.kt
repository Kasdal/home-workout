package com.example.workoutapp.ui.workout

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
    
    var showSummary by remember { mutableStateOf(false) }
    var lastSession by remember { mutableStateOf<com.example.workoutapp.data.local.entity.WorkoutSession?>(null) }

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

    if (showSummary && lastSession != null) {
        AlertDialog(
            onDismissRequest = { showSummary = false },
            title = { Text("Workout Completed! \uD83D\uDCAA") },
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

    Scaffold(
        topBar = {
            Column {
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
                    onStartRest = { viewModel.startTimer(restTimerDuration) },
                    onStartExerciseSwitch = { viewModel.startTimer(exerciseSwitchDuration) },
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onStop = { viewModel.stopTimer() },
                    onSetRestDuration = { viewModel.setRestTimerDuration(it) },
                    onSetExerciseSwitchDuration = { viewModel.setExerciseSwitchDuration(it) }
                )
                Button(
                    onClick = { navController.navigate(Screen.History.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("View History")
                }
                Button(
                    onClick = { navController.navigate(Screen.Profile.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Manage Profiles")
                }
                Button(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Settings")
                }
            }
        },

        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addExercise() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        },
        bottomBar = {
            Button(
                onClick = { 
                    if (sessionStarted) {
                        viewModel.completeSession { session ->
                            lastSession = session
                            showSummary = true
                        }
                    } else {
                        viewModel.startSession()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(64.dp),
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
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(exercises) { exercise ->
                val setCount = completedSets[exercise.id] ?: 0
                val isCompleted = setCount >= 4
                
                ExerciseCard(
                    exercise = exercise,
                    completedSetCount = setCount,
                    isCompleted = isCompleted,
                    onCompleteSet = { viewModel.completeNextSet(exercise.id) },
                    onUndoSet = { viewModel.undoSet(exercise.id) },
                    onUpdate = { viewModel.updateExercise(it) },
                    onDelete = { viewModel.deleteExercise(exercise.id) }
                )
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
