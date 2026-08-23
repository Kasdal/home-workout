package com.example.workoutapp.domain.trend

import com.example.workoutapp.model.SessionExercise
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTrendCalculatorTest {

    private val sessionDates = mapOf(
        1 to 1_000_000L,
        2 to 2_000_000L,
        3 to 3_000_000L
    )

    @Test
    fun `build joins entries with session dates and sorts oldest first`() {
        val points = ExerciseTrendCalculator.build(
            entries = listOf(
                entry(sessionId = 3),
                entry(sessionId = 1),
                entry(sessionId = 2)
            ),
            sessionDates = sessionDates
        )

        assertEquals(listOf(1, 2, 3), points.map { it.sessionId })
        assertEquals(1_000_000L, points.first().dateMillis)
    }

    @Test
    fun `build aggregates sets within one session`() {
        val points = ExerciseTrendCalculator.build(
            entries = listOf(
                entry(sessionId = 1, weight = 60f, volume = 480f),
                entry(sessionId = 1, weight = 70f, volume = 560f)
            ),
            sessionDates = sessionDates
        )

        val only = points.single()
        assertEquals(70f, only.weight)
        assertEquals(1040f, only.volume)
    }

    @Test
    fun `build drops entries whose session has no date`() {
        val points = ExerciseTrendCalculator.build(
            entries = listOf(entry(sessionId = 9)),
            sessionDates = sessionDates
        )

        assertEquals(emptyList<ExerciseTrendPoint>(), points)
    }

    @Test
    fun `filterByRange keeps only recent sessions and null means all`() {
        val dayMillis = 86_400_000L
        val now = 10 * dayMillis
        val recentSessionDates = mapOf(
            1 to now - 5 * dayMillis,
            2 to now - 2 * dayMillis,
            3 to now
        )
        val points = ExerciseTrendCalculator.build(
            entries = listOf(entry(1), entry(2), entry(3)),
            sessionDates = recentSessionDates
        )

        val lastTwo = ExerciseTrendCalculator.filterByRange(points, now, rangeDays = 3)
        assertEquals(listOf(2, 3), lastTwo.map { it.sessionId })

        val all = ExerciseTrendCalculator.filterByRange(points, now, rangeDays = null)
        assertEquals(points, all)
    }

    private fun entry(
        sessionId: Int,
        weight: Float = 50f,
        volume: Float = 500f
    ) = SessionExercise(
        id = 0,
        sessionId = sessionId,
        exerciseName = "Bench Press",
        weight = weight,
        sets = 3,
        reps = 8,
        volume = volume
    )
}
