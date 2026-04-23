package com.example.workoutapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.data.repository.SessionHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SessionHistoryRepository
) : ViewModel() {
    val sessions = repository.getSessions()
        .catch { Timber.e(it, "sessions flow error") }
    val sessionExercises = repository.getAllSessionExercises()
        .catch { Timber.e(it, "sessionExercises flow error") }

    val personalRecords: Flow<PersonalRecords> = combine(sessions, sessionExercises) { sessionList, exercises ->
        try {
            calculatePersonalRecords(sessionList, exercises)
        } catch (e: Exception) {
            Timber.e(e, "calculatePersonalRecords error")
            PersonalRecords(emptyMap(), 0f, 0, 0, 0, 0f, 0)
        }
    }

    val exercisePrs: Flow<List<ExercisePr>> = sessionExercises.map { exercises ->
        try {
            exercises
                .groupBy { it.exerciseName }
                .map { (name, entries) ->
                    val best = entries.maxOfOrNull { it.weight } ?: 0f
                    val totalVolume = entries.sumOf { it.volume.toDouble() }.toFloat()
                    val sessionCount = entries.map { it.sessionId }.distinct().size
                    val trend = computeExerciseTrend(entries)
                    ExercisePr(name = name, bestWeight = best, totalVolume = totalVolume, sessionCount = sessionCount, trend = trend)
                }
                .sortedByDescending { it.sessionCount }
                .take(10)
        } catch (e: Exception) {
            Timber.e(e, "exercisePrs error")
            emptyList()
        }
    }

    val weeklySummary: Flow<SummaryComparison> = sessions.map { calculateWeeklySummary(it) }
    val monthlySummary: Flow<SummaryComparison> = sessions.map { calculateMonthlySummary(it) }

    val weeklyOverview: Flow<WeeklyOverview> = combine(sessions, weeklySummary) { _, summary ->
        WeeklyOverview(
            workoutsThisWeek = summary.current.totalWorkouts,
            workoutsLastWeek = summary.previous.totalWorkouts,
            volumeThisWeek = summary.current.totalVolume,
            volumeLastWeek = summary.previous.totalVolume,
            avgDurationMin = if (summary.current.avgWorkoutDuration > 0) (summary.current.avgWorkoutDuration / 60).toInt() else 0,
            caloriesThisWeek = 0f,
            bestWeek = 0,
            totalWorkouts = 0
        )
    }

    val volumeTrend: Flow<List<WeeklyVolumePoint>> = sessions.map { computeVolumeTrend(it) }

    val weeklyFrequency: Flow<List<Int>> = sessions.map { sessionList ->
        (0..3).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.WEEK_OF_YEAR, -offset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = cal.timeInMillis
            sessionList.count { it.date in weekStart..weekEnd }
        }.reversed()
    }

    val insights: Flow<List<WorkoutInsight>> = combine(
        sessions,
        personalRecords,
        weeklySummary,
        monthlySummary,
        exercisePrs
    ) { sessionList, records, weekly, monthly, prs ->
        generateInsights(sessionList, records, weekly, monthly, prs)
    }

    private fun computeVolumeTrend(sessions: List<WorkoutSession>): List<WeeklyVolumePoint> {
        return (0..7).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.WEEK_OF_YEAR, -offset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = cal.timeInMillis
            val weekSessions = sessions.filter { it.date in weekStart..weekEnd }
            val label = when (offset) {
                0 -> "This Week"
                1 -> "Last Week"
                else -> "${offset}w ago"
            }
            WeeklyVolumePoint(
                label = label,
                totalVolume = weekSessions.sumOf { it.totalVolume.toDouble() }.toFloat(),
                workoutCount = weekSessions.size,
                weekStart = weekStart
            )
        }.reversed()
    }

    private fun computeExerciseTrend(entries: List<SessionExercise>): ExerciseTrend {
        if (entries.size < 2) return ExerciseTrend.FLAT
        val sorted = entries.sortedBy { it.sessionId }
        val mid = sorted.size / 2
        val firstHalfAvg = sorted.take(mid).map { it.weight }.average().toFloat()
        val secondHalfAvg = sorted.drop(mid).map { it.weight }.average().toFloat()
        val pctChange = if (firstHalfAvg > 0) ((secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100) else 0f
        return when {
            pctChange > 5 -> ExerciseTrend.UP
            pctChange < -5 -> ExerciseTrend.DOWN
            else -> ExerciseTrend.FLAT
        }
    }

    private fun calculatePersonalRecords(
        sessions: List<WorkoutSession>,
        allExercises: List<SessionExercise>
    ): PersonalRecords {
        if (sessions.isEmpty()) {
            return PersonalRecords(
                heaviestLiftByExercise = emptyMap(),
                mostVolume = 0f,
                longestSession = 0,
                currentStreak = 0,
                totalWorkouts = 0,
                totalVolume = 0f,
                totalDurationMin = 0
            )
        }

        val heaviestByExercise = allExercises
            .groupBy { it.exerciseName }
            .mapValues { (_, entries) -> entries.maxOf { it.weight } }

        val mostVolume = sessions.maxOfOrNull { it.totalVolume } ?: 0f
        val longestSession = (sessions.maxOfOrNull { it.durationSeconds } ?: 0L).toInt() / 60
        val currentStreak = calculateCurrentStreak(sessions)
        val totalWorkouts = sessions.size
        val totalVolume = sessions.sumOf { it.totalVolume.toDouble() }.toFloat()
        val totalDurationMin = (sessions.sumOf { it.durationSeconds } / 60).toInt()

        return PersonalRecords(
            heaviestLiftByExercise = heaviestByExercise,
            mostVolume = mostVolume,
            longestSession = longestSession,
            currentStreak = currentStreak,
            totalWorkouts = totalWorkouts,
            totalVolume = totalVolume,
            totalDurationMin = totalDurationMin
        )
    }

    private fun calculateCurrentStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0

        val sortedSessions = sessions.sortedByDescending { it.date }
        val today = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        val mostRecentDate = sortedSessions.first().date
        val daysSinceLastWorkout = (today - mostRecentDate) / oneDayMillis

        if (daysSinceLastWorkout > 1) return 0

        val workoutDates = sortedSessions.map {
            (it.date / oneDayMillis) * oneDayMillis
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

    private fun calculateWeeklySummary(sessions: List<WorkoutSession>): SummaryComparison {
        val now = System.currentTimeMillis()
        val oneWeekMillis = TimeUnit.DAYS.toMillis(7)

        val thisWeekStart = now - oneWeekMillis
        val lastWeekStart = thisWeekStart - oneWeekMillis

        val thisWeekSessions = sessions.filter { it.date >= thisWeekStart }
        val lastWeekSessions = sessions.filter { it.date >= lastWeekStart && it.date < thisWeekStart }

        return createComparison(
            createPeriodSummary(thisWeekSessions, "This Week"),
            createPeriodSummary(lastWeekSessions, "Last Week")
        )
    }

    private fun calculateMonthlySummary(sessions: List<WorkoutSession>): SummaryComparison {
        val now = System.currentTimeMillis()
        val oneMonthMillis = TimeUnit.DAYS.toMillis(30)

        val thisMonthStart = now - oneMonthMillis
        val lastMonthStart = thisMonthStart - oneMonthMillis

        val thisMonthSessions = sessions.filter { it.date >= thisMonthStart }
        val lastMonthSessions = sessions.filter { it.date >= lastMonthStart && it.date < thisMonthStart }

        return createComparison(
            createPeriodSummary(thisMonthSessions, "This Month"),
            createPeriodSummary(lastMonthSessions, "Last Month")
        )
    }

    private fun createPeriodSummary(sessions: List<WorkoutSession>, label: String): PeriodSummary {
        if (sessions.isEmpty()) {
            return PeriodSummary(0, 0f, 0L, 0L, label)
        }

        val totalWorkouts = sessions.size
        val totalVolume = sessions.sumOf { it.totalVolume.toDouble() }.toFloat()
        val totalDuration = sessions.sumOf { it.durationSeconds }
        val avgDuration = totalDuration / totalWorkouts

        return PeriodSummary(
            totalWorkouts,
            totalVolume,
            totalDuration,
            avgDuration,
            label
        )
    }

    private fun createComparison(current: PeriodSummary, previous: PeriodSummary): SummaryComparison {
        val volumeChange = when {
            previous.totalVolume > 0 -> ((current.totalVolume - previous.totalVolume) / previous.totalVolume) * 100
            current.totalVolume > 0 -> 100f
            else -> 0f
        }

        val frequencyChange = current.totalWorkouts - previous.totalWorkouts

        val durationChange = when {
            previous.avgWorkoutDuration > 0 -> ((current.avgWorkoutDuration - previous.avgWorkoutDuration).toFloat() / previous.avgWorkoutDuration) * 100
            current.avgWorkoutDuration > 0 -> 100f
            else -> 0f
        }

        return SummaryComparison(current, previous, volumeChange, frequencyChange, durationChange)
    }

    private fun generateInsights(
        sessions: List<WorkoutSession>,
        records: PersonalRecords,
        weekly: SummaryComparison,
        monthly: SummaryComparison,
        prs: List<ExercisePr>
    ): List<WorkoutInsight> {
        val insightsList = mutableListOf<WorkoutInsight>()

        when (records.totalWorkouts) {
            1 -> insightsList.add(WorkoutInsight(InsightType.ACHIEVEMENT, "First Workout!", "You've started your fitness journey. Every champion starts somewhere!", "🎉"))
            10 -> insightsList.add(WorkoutInsight(InsightType.ACHIEVEMENT, "10 Workouts!", "You're building real consistency. Keep showing up!", "🔥"))
            25 -> insightsList.add(WorkoutInsight(InsightType.ACHIEVEMENT, "25 Workouts!", "A quarter century of sessions. You're building serious momentum!", "💪"))
            50 -> insightsList.add(WorkoutInsight(InsightType.ACHIEVEMENT, "50 Workouts!", "Halfway to 100. You're in elite territory now!", "🏆"))
            100 -> insightsList.add(WorkoutInsight(InsightType.ACHIEVEMENT, "100 Workouts!", "Triple digits. You're a machine. Plain and simple.", "🏆"))
        }

        val topExercises = prs.filter { it.trend == ExerciseTrend.UP }.take(2)
        topExercises.forEach { pr ->
            insightsList.add(WorkoutInsight(
                InsightType.PROGRESS,
                "${pr.name} improving!",
                "Your ${pr.name} weight is trending up. Keep pushing!",
                "📈"
            ))
        }

        if (weekly.volumeChangePercent > 10f) {
            insightsList.add(WorkoutInsight(
                InsightType.PROGRESS,
                "Volume Surge!",
                "You lifted ${String.format("%.0f", weekly.volumeChangePercent)}% more this week. That's real progress.",
                "💪"
            ))
        }

        if (weekly.volumeChangePercent < -20f && sessions.isNotEmpty()) {
            insightsList.add(WorkoutInsight(
                InsightType.ENCOURAGEMENT,
                "Recovery Week?",
                "Volume was lower this week. Rest is part of training — trust the process.",
                "🌿"
            ))
        }

        when {
            records.currentStreak >= 7 -> insightsList.add(WorkoutInsight(InsightType.STREAK, "Week Streak!", "${records.currentStreak} days in a row. You're on fire right now!", "🔥"))
            records.currentStreak >= 14 -> insightsList.add(WorkoutInsight(InsightType.STREAK, "Two Week Streak!", "${records.currentStreak} days straight. Consistency is your superpower!", "⚡"))
            records.currentStreak >= 30 -> insightsList.add(WorkoutInsight(InsightType.STREAK, "Month Streak!", "${records.currentStreak} days! You've built a real habit.", "🏆"))
        }

        if (weekly.frequencyChange > 0) {
            insightsList.add(WorkoutInsight(
                InsightType.ENCOURAGEMENT,
                "More Active!",
                "${weekly.frequencyChange} more workout${if (weekly.frequencyChange > 1) "s" else ""} this week vs last.",
                "✨"
            ))
        }

        if (records.totalWorkouts >= 20 && prs.size >= 3) {
            val avgTrend = prs.count { it.trend == ExerciseTrend.UP }
            if (avgTrend >= prs.size / 2) {
                insightsList.add(WorkoutInsight(
                    InsightType.ENCOURAGEMENT,
                    "Strength Building!",
                    "${avgTrend} of your exercises are on an upward trend. Keep it up!",
                    "⭐"
                ))
            }
        }

        if (insightsList.isEmpty() && sessions.isNotEmpty()) {
            insightsList.add(WorkoutInsight(
                InsightType.ENCOURAGEMENT,
                "Keep Showing Up!",
                "Every session builds on the last. You're doing the work.",
                "💪"
            ))
        }

        return insightsList.take(4)
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}

data class WeeklyVolumePoint(
    val label: String,
    val totalVolume: Float,
    val workoutCount: Int,
    val weekStart: Long
)

data class WeeklyOverview(
    val workoutsThisWeek: Int,
    val workoutsLastWeek: Int,
    val volumeThisWeek: Float,
    val volumeLastWeek: Float,
    val avgDurationMin: Int,
    val caloriesThisWeek: Float,
    val bestWeek: Int,
    val totalWorkouts: Int
) {
    val workoutsDelta: Int get() = workoutsThisWeek - workoutsLastWeek
    val volumeDelta: Float get() = if (volumeLastWeek > 0) ((volumeThisWeek - volumeLastWeek) / volumeLastWeek * 100) else if (volumeThisWeek > 0) 100f else 0f
}

data class ExercisePr(
    val name: String,
    val bestWeight: Float,
    val totalVolume: Float,
    val sessionCount: Int,
    val trend: ExerciseTrend
)

enum class ExerciseTrend { UP, DOWN, FLAT }
