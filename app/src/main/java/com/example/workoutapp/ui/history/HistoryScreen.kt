package com.example.workoutapp.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.components.ExerciseTrendDialog
import com.example.workoutapp.ui.theme.NeonGreen
import com.example.workoutapp.util.formatDuration
import com.example.workoutapp.util.formatKg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val selectedSessionExercises by viewModel.selectedSessionExercises.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var trendExerciseName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "history",
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    ) { padding ->
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle(initialValue = PersonalRecords(
        heaviestLiftByExercise = emptyMap(),
        mostVolume = 0f,
        longestSession = 0,
        currentStreak = 0,
        totalWorkouts = 0
    ))

    val weeklyOverview by viewModel.weeklyOverview.collectAsStateWithLifecycle(initialValue = WeeklyOverview(
        workoutsThisWeek = 0, workoutsLastWeek = 0, volumeThisWeek = 0f,
        volumeLastWeek = 0f, avgDurationMin = 0, caloriesThisWeek = 0f,
        bestWeek = 0, totalWorkouts = 0
    ))
    val exercisePrs by viewModel.exercisePrs.collectAsStateWithLifecycle(initialValue = emptyList())
    val milestones by viewModel.milestones.collectAsStateWithLifecycle(initialValue = emptyList())
    val volumeTrend by viewModel.volumeTrend.collectAsStateWithLifecycle(initialValue = emptyList())
    val weeklyFrequency by viewModel.weeklyFrequency.collectAsStateWithLifecycle(initialValue = listOf(0, 0, 0, 0))
    val insights by viewModel.insights.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (sessions.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            EmptyHistoryCard(modifier = Modifier.padding(horizontal = 20.dp))
            return@Column
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnalyticsDashboard(
            personalRecords = personalRecords,
            weeklyOverview = weeklyOverview,
            exercisePrs = exercisePrs,
            volumeTrend = volumeTrend,
            weeklyFrequency = weeklyFrequency,
            insights = insights,
            modifier = Modifier.padding(horizontal = 0.dp),
            milestones = milestones,
            onSelectExercise = { trendExerciseName = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Workout Calendar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                currentMonth = newMonth
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = {
                val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                currentMonth = newMonth
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            CalendarGrid(
                currentMonth = currentMonth,
                sessions = sessions,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedDate != null) {
            val sd = selectedDate!!
            val dateSessions = sessions.filter { session: WorkoutSession -> isSameDay(session.date, sd) }
            Text(
                text = "Sessions on ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(sd.time)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (dateSessions.isEmpty()) {
                Text(
                    "No workouts recorded.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                dateSessions.forEach { session: WorkoutSession ->
                    SessionCard(
                        session = session,
                        onClick = { viewModel.openSessionDetail(session) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text(
                "Select a date to view details.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    }

    val openSession = selectedSession
    if (openSession != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.closeSessionDetail() }) {
            SessionDetailContent(
                session = openSession,
                exercises = selectedSessionExercises
            )
        }
    }

    val trendExercise = trendExerciseName
    if (trendExercise != null) {
        // Remember the flow. exerciseTrend is a combine over two Firestore
        // listeners, and collectAsState keys its effect on the flow instance, so
        // building it inline re-subscribed both listeners on every recomposition
        // of this screen while the dialog was open.
        val trendFlow = remember(trendExercise) { viewModel.exerciseTrend(trendExercise) }
        val trendPoints by trendFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        ExerciseTrendDialog(
            exerciseName = trendExercise,
            points = trendPoints,
            onDismiss = { trendExerciseName = null }
        )
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    sessions: List<WorkoutSession>,
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = getDaysInMonth(currentMonth)
    val sessionsInMonth = sessions.filter { session ->
        val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
        sessionCal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
        sessionCal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
    }

    val firstDayOfWeek = daysInMonth.firstOrNull()?.get(Calendar.DAY_OF_WEEK) ?: 1
    val leadingEmpty = firstDayOfWeek - 1
    val totalCells = leadingEmpty + daysInMonth.size
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.height(((rows * 44) + (rows - 1) * 4).dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayIndex = cellIndex - leadingEmpty
                    val day = daysInMonth.getOrNull(dayIndex)

                    if (day == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        return@Row
                    }

                    val isToday = isSameDay(day, Calendar.getInstance())
                    val hasWorkout = sessionsInMonth.any { isSameDay(it.date, day) }
                    val isSelected = selectedDate != null && isSameDay(selectedDate!!, day)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onDateSelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> NeonGreen
                                        hasWorkout -> NeonGreen.copy(alpha = 0.5f)
                                        isToday -> MaterialTheme.colorScheme.outlineVariant
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.get(Calendar.DAY_OF_MONTH).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete this workout session? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.date))}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(session.durationSeconds))
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Weight: ${formatKg(session.totalWeightLifted)}", color = NeonGreen)
                Text("Cals: ${String.format("%.0f", session.caloriesBurned)}")
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    session: WorkoutSession,
    exercises: List<SessionExercise>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault()).format(Date(session.date)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Started at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.date))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SessionStat(label = "Duration", value = formatDuration(session.durationSeconds))
            SessionStat(label = "Volume", value = formatKg(session.totalWeightLifted))
            SessionStat(label = "Calories", value = String.format("%.0f", session.caloriesBurned))
        }

        if (session.rpe != null || !session.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (session.rpe != null) {
                    Text(
                        text = "Session RPE: ${session.rpe}/10",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!session.notes.isNullOrBlank()) {
                    Text(
                        text = session.notes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Exercises",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val workingExercises = exercises.sortedBy { it.sortOrder }.filterNot { it.isWarmUp }
        val warmUpExercises = exercises.sortedBy { it.sortOrder }.filter { it.isWarmUp }
        if (exercises.isEmpty()) {
            Text(
                text = "No exercise data was recorded for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            workingExercises.forEachIndexed { index, exercise ->
                if (index > 0) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                SessionExerciseRow(exercise = exercise)
            }

            if (warmUpExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Warm-up",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                warmUpExercises.forEach { exercise ->
                    SessionExerciseRow(exercise = exercise)
                }
            }
        }
    }
}

@Composable
private fun SessionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NeonGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionExerciseRow(exercise: SessionExercise) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (exercise.rpe != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RPE ${exercise.rpe}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }
            Text(
                text = "${exercise.sets} × ${exercise.reps} @ ${formatKg(exercise.weight)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!exercise.notes.isNullOrBlank()) {
                Text(
                    text = exercise.notes!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = formatKg(exercise.volume),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = NeonGreen
        )
    }
}

@Composable
private fun EmptyHistoryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Complete your first session and it will show up here with trends, records, and a per-exercise breakdown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getDaysInMonth(calendar: Calendar): List<Calendar> {
    val days = mutableListOf<Calendar>()
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in 1..maxDay) {
        days.add(cal.clone() as Calendar)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return days
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isSameDay(timestamp: Long, cal: Calendar): Boolean {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return isSameDay(c, cal)
}

