package com.example.workoutapp.ui.restdays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
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
import com.example.workoutapp.ui.theme.NeonGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestDaysScreen(
    navController: NavController,
    viewModel: RestDaysViewModel = hiltViewModel()
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val restDays by viewModel.restDays.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val noteText by viewModel.noteText.collectAsState()
    var showNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rest Days") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.getRestDaysThisWeek()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text("This Week", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.getRestDaysThisMonth()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text("This Month", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ArrowBackIosNew, "Previous Month")
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ArrowForwardIos, "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            CalendarGrid(
                currentMonth = currentMonth,
                viewModel = viewModel,
                onDateClick = { date ->
                    viewModel.selectDate(date)
                    showNoteDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            Text(
                text = "Tap a date to mark/unmark as rest day and add notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Note dialog
    if (showNoteDialog && selectedDate != null) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Rest Day - ${selectedDate}") },
            text = {
                Column {
                    Text("Mark this day as a rest day?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { viewModel.updateNote(it) },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleRestDay(selectedDate!!)
                        if (noteText.isNotBlank()) {
                            viewModel.saveNote()
                        }
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text(
                        if (viewModel.isRestDay(selectedDate!!)) "Remove" else "Mark as Rest Day",
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    viewModel: RestDaysViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
    val daysInMonth = currentMonth.lengthOfMonth()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(4.dp)
    ) {
        // Empty cells before first day
        items(firstDayOfWeek - 1) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        // Days of the month
        items(daysInMonth) { index ->
            val day = index + 1
            val date = currentMonth.atDay(day)
            val isRestDay = viewModel.isRestDay(date)
            val isToday = date == LocalDate.now()

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRestDay -> NeonGreen.copy(alpha = 0.3f)
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = if (isToday) 2.dp else 0.dp,
                        color = if (isToday) NeonGreen else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onDateClick(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isRestDay || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRestDay) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
