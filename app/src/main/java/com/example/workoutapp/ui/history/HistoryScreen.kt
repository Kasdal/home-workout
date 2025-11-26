package com.example.workoutapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Workout Calendar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Calendar Header (Month Navigation)
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

        // Days of Week Header
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

        // Calendar Grid
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

        // Selected Date Sessions
        if (selectedDate != null) {
            val dateSessions = sessions.filter { isSameDay(it.date, selectedDate!!) }
            Text(
                text = "Sessions on ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(selectedDate!!.time)}",
                style = MaterialTheme.typography.titleMedium
            )
            
            if (dateSessions.isEmpty()) {
                Text("No workouts recorded.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            } else {
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(dateSessions) { session ->
                        SessionCard(session)
                    }
                }
            }
        } else {
            Text("Select a date to view details.", color = Color.Gray)
        }
    }
}

@Composable
fun SessionCard(session: WorkoutSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
