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
    
    // Weekly and Monthly Summaries
    val weeklySummary: Flow<SummaryComparison> = sessions.map { sessionList ->
        calculateWeeklySummary(sessionList)
    }
    
    val monthlySummary: Flow<SummaryComparison> = sessions.map { sessionList ->
        calculateMonthlySummary(sessionList)
    }
    
    private fun calculateWeeklySummary(sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>): SummaryComparison {
        val now = System.currentTimeMillis()
        val oneWeekMillis = TimeUnit.DAYS.toMillis(7)
        
        val thisWeekStart = now - oneWeekMillis
        val lastWeekStart = thisWeekStart - oneWeekMillis
        
        val thisWeekSessions = sessions.filter { it.date >= thisWeekStart }
        val lastWeekSessions = sessions.filter { it.date >= lastWeekStart && it.date < thisWeekStart }
        
        val currentSummary = createPeriodSummary(thisWeekSessions, "This Week")
        val previousSummary = createPeriodSummary(lastWeekSessions, "Last Week")
        
        return createComparison(currentSummary, previousSummary)
    }
    
    private fun calculateMonthlySummary(sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>): SummaryComparison {
        val now = System.currentTimeMillis()
        val oneMonthMillis = TimeUnit.DAYS.toMillis(30)
        
        val thisMonthStart = now - oneMonthMillis
        val lastMonthStart = thisMonthStart - oneMonthMillis
        
        val thisMonthSessions = sessions.filter { it.date >= thisMonthStart }
        val lastMonthSessions = sessions.filter { it.date >= lastMonthStart && it.date < thisMonthStart }
        
        val currentSummary = createPeriodSummary(thisMonthSessions, "This Month")
        val previousSummary = createPeriodSummary(lastMonthSessions, "Last Month")
        
        return createComparison(currentSummary, previousSummary)
    }
    
    private fun createPeriodSummary(sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>, label: String): PeriodSummary {
        if (sessions.isEmpty()) {
            return PeriodSummary(
                totalWorkouts = 0,
                totalVolume = 0f,
                totalDuration = 0L,
                avgWorkoutDuration = 0L,
                periodLabel = label
            )
        }
        
        val totalWorkouts = sessions.size
        val totalVolume = sessions.sumOf { it.totalVolume.toDouble() }.toFloat()
        val totalDuration = sessions.sumOf { it.durationSeconds }
        val avgDuration = totalDuration / totalWorkouts
        
        return PeriodSummary(
            totalWorkouts = totalWorkouts,
            totalVolume = totalVolume,
            totalDuration = totalDuration,
            avgWorkoutDuration = avgDuration,
            periodLabel = label
        )
    }
    
    private fun createComparison(current: PeriodSummary, previous: PeriodSummary): SummaryComparison {
        val volumeChange = if (previous.totalVolume > 0) {
            ((current.totalVolume - previous.totalVolume) / previous.totalVolume) * 100
        } else if (current.totalVolume > 0) {
            100f // 100% increase from 0
        } else {
            0f
        }
        
        val frequencyChange = current.totalWorkouts - previous.totalWorkouts
        
        val durationChange = if (previous.avgWorkoutDuration > 0) {
            ((current.avgWorkoutDuration - previous.avgWorkoutDuration).toFloat() / previous.avgWorkoutDuration) * 100
        } else if (current.avgWorkoutDuration > 0) {
            100f
        } else {
            0f
        }
        
        return SummaryComparison(
            current = current,
            previous = previous,
            volumeChangePercent = volumeChange,
            frequencyChange = frequencyChange,
            durationChangePercent = durationChange
        )
    }
    
    // Motivational Insights
    val insights: Flow<List<WorkoutInsight>> = combine(
        sessions,
        personalRecords,
        weeklySummary,
        monthlySummary
    ) { sessionList, records, weekly, monthly ->
        generateInsights(sessionList, records, weekly, monthly)
    }
    
    private fun generateInsights(
        sessions: List<com.example.workoutapp.data.local.entity.WorkoutSession>,
        records: PersonalRecords,
        weekly: SummaryComparison,
        monthly: SummaryComparison
    ): List<WorkoutInsight> {
        val insightsList = mutableListOf<WorkoutInsight>()
        
        // Achievement: Milestone workouts
        when (records.totalWorkouts) {
            1 -> insightsList.add(WorkoutInsight(
                type = InsightType.ACHIEVEMENT,
                title = "First Workout! 🎉",
                message = "You've started your fitness journey. Every champion starts somewhere!",
                emoji = "🎉"
            ))
            10 -> insightsList.add(WorkoutInsight(
                type = InsightType.ACHIEVEMENT,
                title = "10 Workouts Complete!",
                message = "You're building consistency. Keep it up!",
                emoji = "🔥"
            ))
            25 -> insightsList.add(WorkoutInsight(
                type = InsightType.ACHIEVEMENT,
                title = "25 Workouts Milestone!",
                message = "You're crushing it! A quarter century of gains!",
                emoji = "💪"
            ))
            50 -> insightsList.add(WorkoutInsight(
                type = InsightType.ACHIEVEMENT,
                title = "Half Century!",
                message = "50 workouts completed! You're unstoppable!",
                emoji = "🏆"
            ))
            100 -> insightsList.add(WorkoutInsight(
                type = InsightType.ACHIEVEMENT,
                title = "100 Workouts! 🎊",
                message = "Triple digits! You're a fitness legend!",
                emoji = "🎊"
            ))
        }
        
        // Progress: Weekly volume improvement
        if (weekly.volumeChangePercent > 10f) {
            insightsList.add(WorkoutInsight(
                type = InsightType.PROGRESS,
                title = "Volume Surge!",
                message = "You lifted ${String.format("%.0f", weekly.volumeChangePercent)}% more this week! 💪",
                emoji = "📈"
            ))
        }
        
        // Progress: Monthly volume improvement
        if (monthly.volumeChangePercent > 20f) {
            insightsList.add(WorkoutInsight(
                type = InsightType.PROGRESS,
                title = "Monthly Gains!",
                message = "Your volume is up ${String.format("%.0f", monthly.volumeChangePercent)}% this month!",
                emoji = "🚀"
            ))
        }
        
        // Streak: Current streak milestones
        when {
            records.currentStreak >= 7 -> insightsList.add(WorkoutInsight(
                type = InsightType.STREAK,
                title = "Week Streak! 🔥",
                message = "${records.currentStreak} days in a row! You're on fire!",
                emoji = "🔥"
            ))
            records.currentStreak >= 14 -> insightsList.add(WorkoutInsight(
                type = InsightType.STREAK,
                title = "Two Week Streak!",
                message = "${records.currentStreak} days straight! Unstoppable!",
                emoji = "⚡"
            ))
            records.currentStreak >= 30 -> insightsList.add(WorkoutInsight(
                type = InsightType.STREAK,
                title = "Month Streak! 🏆",
                message = "${records.currentStreak} days! You're a machine!",
                emoji = "🏆"
            ))
        }
        
        // Encouragement: Frequency increase
        if (weekly.frequencyChange > 0) {
            insightsList.add(WorkoutInsight(
                type = InsightType.ENCOURAGEMENT,
                title = "More Active!",
                message = "You worked out ${weekly.frequencyChange} more time${if (weekly.frequencyChange > 1) "s" else ""} this week!",
                emoji = "✨"
            ))
        }
        
        // Encouragement: Consistency
        if (sessions.size >= 5 && records.currentStreak >= 3) {
            insightsList.add(WorkoutInsight(
                type = InsightType.ENCOURAGEMENT,
                title = "Building Habits!",
                message = "Consistency is key. You're doing great!",
                emoji = "⭐"
            ))
        }
        
        // Default encouragement if no insights
        if (insightsList.isEmpty() && sessions.isNotEmpty()) {
            insightsList.add(WorkoutInsight(
                type = InsightType.ENCOURAGEMENT,
                title = "Keep Going!",
                message = "Every workout counts. You're making progress!",
                emoji = "💪"
            ))
        }
        
        return insightsList.take(3) // Limit to 3 insights
    }
    
    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
