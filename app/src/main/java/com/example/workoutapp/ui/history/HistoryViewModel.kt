package com.example.workoutapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {
    val sessions = repository.getSessions()
    
    // Personal Records
    val personalRecords: Flow<PersonalRecords> = sessions.map { sessionList ->
        calculatePersonalRecords(sessionList)
    }
    
    private fun calculatePersonalRecords(
        sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>
    ): PersonalRecords {
        if (sessions.isEmpty()) {
            return PersonalRecords(
                heaviestLiftByExercise = emptyMap(),
                mostVolume = 0f,
                longestSession = 0,
                currentStreak = 0,
                totalWorkouts = 0
            )
        }
        
        // For now, we'll calculate heaviest lifts from session data
        // In future, we can enhance this with SessionExercise data
        val heaviestLifts = emptyMap<String, Float>() // Will be populated when we have session exercise data
        
        // Most volume in a single session
        val mostVolume = sessions.maxOfOrNull { it.totalVolume } ?: 0f
        
        // Longest session in minutes
        val longestSession = (sessions.maxOfOrNull { it.durationSeconds } ?: 0L).toInt() / 60
        
        // Current streak
        val currentStreak = calculateCurrentStreak(sessions)
        
        // Total workouts
        val totalWorkouts = sessions.size
        
        return PersonalRecords(
            heaviestLiftByExercise = heaviestLifts,
            mostVolume = mostVolume,
            longestSession = longestSession,
            currentStreak = currentStreak,
            totalWorkouts = totalWorkouts
        )
    }
    
    private fun calculateCurrentStreak(sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sortedSessions = sessions.sortedByDescending { it.date }
        val today = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)
        
        // Check if the most recent workout was today or yesterday
        val mostRecentDate = sortedSessions.first().date
        val daysSinceLastWorkout = (today - mostRecentDate) / oneDayMillis
        
        if (daysSinceLastWorkout > 1) {
            return 0 // Streak broken
        }
        
        // Count consecutive days with workouts
        val workoutDates = sortedSessions.map { session ->
            (session.date / oneDayMillis) * oneDayMillis // Normalize to day
        }.toSet().sorted().reversed()
        
        var streak = 0
        var expectedDate = (today / oneDayMillis) * oneDayMillis
        
        for (workoutDate in workoutDates) {
            val diff = (expectedDate - workoutDate) / oneDayMillis
            if (diff == 0L || diff == 1L) {
                streak++
                expectedDate = workoutDate - oneDayMillis
            } else {
                break
            }
        }
        
        return streak
    }
    
    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
