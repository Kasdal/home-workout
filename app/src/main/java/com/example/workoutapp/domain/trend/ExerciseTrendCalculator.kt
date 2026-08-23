package com.example.workoutapp.domain.trend

import com.example.workoutapp.model.SessionExercise
import java.util.concurrent.TimeUnit

data class ExerciseTrendPoint(
    val sessionId: Int,
    val dateMillis: Long,
    val weight: Float,
    val volume: Float
)

object ExerciseTrendCalculator {

    /**
     * Joins a single exercise's [SessionExercise] entries with their session dates.
     * One point per session; weight takes the heaviest set, volume sums all sets.
     * Points come back sorted oldest to newest.
     */
    fun build(
        entries: List<SessionExercise>,
        sessionDates: Map<Int, Long>
    ): List<ExerciseTrendPoint> {
        return entries
            .filter { sessionDates.containsKey(it.sessionId) }
            .groupBy { it.sessionId }
            .map { (sessionId, group) ->
                ExerciseTrendPoint(
                    sessionId = sessionId,
                    dateMillis = sessionDates.getValue(sessionId),
                    weight = group.maxOf { it.weight },
                    volume = group.sumOf { it.volume.toDouble() }.toFloat()
                )
            }
            .sortedBy { it.dateMillis }
    }

    /** Keeps points inside the last [rangeDays] days; [rangeDays] = null means no limit. */
    fun filterByRange(
        points: List<ExerciseTrendPoint>,
        nowMillis: Long,
        rangeDays: Int?
    ): List<ExerciseTrendPoint> {
        if (rangeDays == null) return points
        val cutoff = nowMillis - TimeUnit.DAYS.toMillis(rangeDays.toLong())
        return points.filter { it.dateMillis >= cutoff }
    }
}
