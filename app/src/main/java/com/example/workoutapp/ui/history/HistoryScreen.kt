package com.example.workoutapp.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.workoutapp.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
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
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Personal Records Section
        item {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Workouts
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${personalRecords.totalWorkouts}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Current Streak
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${personalRecords.currentStreak} 🔥",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "Day Streak",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Longest Session
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${personalRecords.longestSession}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Longest",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Highest Volume
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${personalRecords.mostVolume.toInt()}kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Max Volume",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Weekly Summary
        item {
            val weeklySummary by viewModel.weeklySummary.collectAsState(initial = SummaryComparison(
                current = PeriodSummary(0, 0f, 0L, 0L, "This Week"),
                previous = PeriodSummary(0, 0f, 0L, 0L, "Last Week"),
                volumeChangePercent = 0f,
                frequencyChange = 0,
                durationChangePercent = 0f
            ))
            
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = weeklySummary.current.periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${weeklySummary.current.totalWorkouts} workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Volume comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${weeklySummary.current.totalVolume.toInt()}kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (weeklySummary.volumeChangePercent >= 0) "+" else ""}${String.format("%.1f", weeklySummary.volumeChangePercent)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (weeklySummary.volumeChangePercent >= 0) NeonGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Frequency comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Frequency:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${if (weeklySummary.frequencyChange >= 0) "+" else ""}${weeklySummary.frequencyChange} workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (weeklySummary.frequencyChange >= 0) NeonGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Monthly Summary
        item {
            val monthlySummary by viewModel.monthlySummary.collectAsState(initial = SummaryComparison(
                current = PeriodSummary(0, 0f, 0L, 0L, "This Month"),
                previous = PeriodSummary(0, 0f, 0L, 0L, "Last Month"),
                volumeChangePercent = 0f,
                frequencyChange = 0,
                durationChangePercent = 0f
            ))
            
            Text(
                text = "Monthly Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = monthlySummary.current.periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${monthlySummary.current.totalWorkouts} workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Volume comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${monthlySummary.current.totalVolume.toInt()}kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (monthlySummary.volumeChangePercent >= 0) "+" else ""}${String.format("%.1f", monthlySummary.volumeChangePercent)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (monthlySummary.volumeChangePercent >= 0) NeonGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Frequency comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Frequency:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${if (monthlySummary.frequencyChange >= 0) "+" else ""}${monthlySummary.frequencyChange} workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (monthlySummary.frequencyChange >= 0) NeonGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        item {
            Text(
                text = "Workout Calendar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Calendar Header (Month Navigation)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
        }

        // Days of Week Header
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Calendar Grid
        item {
            val daysInMonth = getDaysInMonth(currentMonth)
            val sessionsInMonth = sessions.filter { session ->
                val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
                sessionCal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                sessionCal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(300.dp)
            ) {
                // Empty cells for offset
                val firstDayOfWeek = daysInMonth.firstOrNull()?.get(Calendar.DAY_OF_WEEK) ?: 1
                items(firstDayOfWeek - 1) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                items(daysInMonth) { day ->
                    val isToday = isSameDay(day, Calendar.getInstance())
                    val hasWorkout = sessionsInMonth.any { isSameDay(it.date, day) }
                    val isSelected = selectedDate != null && isSameDay(selectedDate!!, day)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> NeonGreen
                                    hasWorkout -> NeonGreen.copy(alpha = 0.5f)
                                    isToday -> Color.Gray.copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { selectedDate = day },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.get(Calendar.DAY_OF_MONTH).toString(),
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Selected Date Sessions
        if (selectedDate != null) {
            val dateSessions = sessions.filter { isSameDay(it.date, selectedDate!!) }
            item {
                Text(
                    text = "Sessions on ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(selectedDate!!.time)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            if (dateSessions.isEmpty()) {
                item {
                    Text("No workouts recorded.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                items(dateSessions) { session ->
                    SessionCard(
                        session = session,
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            item {
                Text("Select a date to view details.", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Statistics Section (Moved to bottom)
        item {
            StatisticsSection(sessions = sessions)
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
            .padding(vertical = 4.dp)
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

// Helper Functions
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

        // Volume Chart
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

        // Frequency Chart
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
        
        // Draw lines
        val path = androidx.compose.ui.graphics.Path()
        sortedSessions.forEachIndexed { index, session ->
            val x = index * spacing
            val y = height - (session.totalWeightLifted / maxVolume * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            // Draw point
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
    // Group sessions by week (last 4 weeks)
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
