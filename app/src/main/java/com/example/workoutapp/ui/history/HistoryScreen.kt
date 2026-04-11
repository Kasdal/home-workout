package com.example.workoutapp.ui.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.ui.components.BottomNavBar
import com.example.workoutapp.ui.theme.NeonGreen
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
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

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
    val personalRecords by viewModel.personalRecords.collectAsState(initial = PersonalRecords(
        heaviestLiftByExercise = emptyMap(),
        mostVolume = 0f,
        longestSession = 0,
        currentStreak = 0,
        totalWorkouts = 0
    ))

    val weeklySummary by viewModel.weeklySummary.collectAsState(initial = SummaryComparison(
        current = PeriodSummary(0, 0f, 0L, 0L, "This Week"),
        previous = PeriodSummary(0, 0f, 0L, 0L, "Last Week"),
        volumeChangePercent = 0f,
        frequencyChange = 0,
        durationChangePercent = 0f
    ))

    val monthlySummary by viewModel.monthlySummary.collectAsState(initial = SummaryComparison(
        current = PeriodSummary(0, 0f, 0L, 0L, "This Month"),
        previous = PeriodSummary(0, 0f, 0L, 0L, "Last Month"),
        volumeChangePercent = 0f,
        frequencyChange = 0,
        durationChangePercent = 0f
    ))

    val weeklyOverview by viewModel.weeklyOverview.collectAsState(initial = WeeklyOverview(
        workoutsThisWeek = 0, workoutsLastWeek = 0, volumeThisWeek = 0f,
        volumeLastWeek = 0f, avgDurationMin = 0, caloriesThisWeek = 0f,
        bestWeek = 0, totalWorkouts = 0
    ))
    val exercisePrs by viewModel.exercisePrs.collectAsState(initial = emptyList())
    val volumeTrend by viewModel.volumeTrend.collectAsState(initial = emptyList())
    val weeklyFrequency by viewModel.weeklyFrequency.collectAsState(initial = listOf(0, 0, 0, 0))
    val insights by viewModel.insights.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        AnalyticsDashboard(
            sessions = sessions,
            personalRecords = personalRecords,
            weeklyOverview = weeklyOverview,
            exercisePrs = exercisePrs,
            volumeTrend = volumeTrend,
            weeklyFrequency = weeklyFrequency,
            insights = insights,
            modifier = Modifier.padding(horizontal = 0.dp)
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
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                dateSessions.forEach { session: WorkoutSession ->
                    SessionCard(
                        session = session,
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
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayIndex in daysInMonth.indices) {
                            val day = daysInMonth[dayIndex]
                            val isToday = isSameDay(day, Calendar.getInstance())
                            val hasWorkout = sessionsInMonth.any { isSameDay(it.date, day) }
                            val isSelected = selectedDate != null && isSameDay(selectedDate!!, day)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> NeonGreen
                                            hasWorkout -> NeonGreen.copy(alpha = 0.5f)
                                            isToday -> Color.Gray.copy(alpha = 0.3f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(day) },
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionCard(session: WorkoutSession, onDelete: () -> Unit) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
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
                onClick = {},
                onLongClick = { showDeleteDialog = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Time: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.date))}")
                Text("${session.durationSeconds / 60} mins")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Weight: ${session.totalWeightLifted} kg", color = NeonGreen)
                Text("Cals: ${String.format("%.0f", session.caloriesBurned)}")
            }
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

@Composable
fun StatisticsSection(sessions: List<WorkoutSession>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trends",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Volume (Last 10 Sessions)",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                VolumeChart(sessions = sessions)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Frequency (Last 4 Weeks)",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                FrequencyChart(sessions = sessions)
            }
        }
    }
}

@Composable
fun VolumeChart(sessions: List<WorkoutSession>) {
    val sortedSessions = sessions.sortedBy { it.date }.takeLast(10)
    if (sortedSessions.isEmpty()) {
        Text("No data available", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val maxVolume = sortedSessions.maxOfOrNull { it.totalWeightLifted } ?: 1f

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (sortedSessions.size - 1).coerceAtLeast(1)

        val path = androidx.compose.ui.graphics.Path()
        sortedSessions.forEachIndexed { index, session ->
            val x = index * spacing
            val y = height - (session.totalWeightLifted / maxVolume * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = NeonGreen,
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = NeonGreen,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun FrequencyChart(sessions: List<WorkoutSession>) {
    val currentCal = Calendar.getInstance()
    val weeks = (0..3).map { offset ->
        val weekStart = currentCal.clone() as Calendar
        weekStart.add(Calendar.WEEK_OF_YEAR, -offset)
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek())

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)

        val count = sessions.count { session ->
            val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
            sessionCal.timeInMillis >= weekStart.timeInMillis &&
            sessionCal.timeInMillis <= weekEnd.timeInMillis
        }
        count
    }.reversed()

    val maxFreq = weeks.maxOrNull()?.coerceAtLeast(1) ?: 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeks.forEachIndexed { index, count ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(count.toFloat() / maxFreq)
                        .background(NeonGreen, shape = MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "W${4-index}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
