package com.example.workoutapp.domain.stats

import com.example.workoutapp.model.SessionExercise

object MilestoneCalculator {

    private val WORKOUT_THRESHOLDS = listOf(1, 10, 25, 50, 100)
    private val STREAK_THRESHOLDS = listOf(3, 7, 14, 30)

    fun unlockedMilestones(
        totalWorkouts: Int,
        currentStreakDays: Int,
        hasAnyLoggedLift: Boolean
    ): List<String> {
        val milestones = mutableListOf<String>()
        WORKOUT_THRESHOLDS.filter { totalWorkouts >= it }.forEach { milestones.add("$it sessions") }
        STREAK_THRESHOLDS.filter { currentStreakDays >= it }.forEach { milestones.add("$it-day streak") }
        if (hasAnyLoggedLift) milestones.add("First PR")
        return milestones
    }

    /**
     * True when the newest session's heaviest set matches the all-time heaviest set.
     * Matching (not beating) an existing best still counts as holding the record.
     */
    fun holdsPersonalRecord(entries: List<SessionExercise>): Boolean {
        if (entries.isEmpty()) return false
        val latestSessionId = entries.maxOf { it.sessionId }
        val latestTopWeight = entries
            .filter { it.sessionId == latestSessionId }
            .maxOf { it.weight }
        if (latestTopWeight <= 0f) return false
        return latestTopWeight >= entries.maxOf { it.weight }
    }
}
