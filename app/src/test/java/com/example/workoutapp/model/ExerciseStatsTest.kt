package com.example.workoutapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ExerciseStats.mergedWith] is the only place the denormalised aggregates are
 * built, so its behaviour is pinned here rather than only through a mocked
 * Firestore. The totals it produces must match exactly what the previous
 * implementation computed by scanning every SessionExercise document.
 */
class ExerciseStatsTest {

    private fun entry(
        sessionId: Int,
        weight: Float,
        volume: Float,
        name: String = "Bench Press"
    ) = SessionExercise(
        id = sessionId * 10,
        sessionId = sessionId,
        exerciseName = name,
        weight = weight,
        sets = 3,
        reps = 10,
        volume = volume
    )

    @Test
    fun `first session seeds the aggregate`() {
        val stats = ExerciseStats(name = "Bench Press")
            .mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f), entry(1, 80f, 2_400f)))

        assertEquals("Bench Press", stats.name)
        assertEquals(100f, stats.bestWeight, 0.001f)
        assertEquals(5_400f, stats.totalVolume, 0.001f)
        assertEquals(1, stats.sessionCount)
        assertEquals(1, stats.recent.size)
    }

    @Test
    fun `best weight is a running maximum across sessions`() {
        var stats = ExerciseStats(name = "Bench Press")
        stats = stats.mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))
        stats = stats.mergedWith(2, 2_000L, listOf(entry(2, 120f, 3_600f)))
        stats = stats.mergedWith(3, 3_000L, listOf(entry(3, 90f, 2_700f)))

        assertEquals(120f, stats.bestWeight, 0.001f)
    }

    @Test
    fun `total volume sums every session`() {
        var stats = ExerciseStats(name = "Bench Press")
        stats = stats.mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))
        stats = stats.mergedWith(2, 2_000L, listOf(entry(2, 110f, 3_300f)))
        stats = stats.mergedWith(3, 3_000L, listOf(entry(3, 120f, 3_600f)))

        assertEquals(9_900f, stats.totalVolume, 0.001f)
        assertEquals(3, stats.sessionCount)
    }

    @Test
    fun `re-saving the same session does not inflate totals or the count`() {
        var stats = ExerciseStats(name = "Bench Press")
        stats = stats.mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))
        stats = stats.mergedWith(2, 2_000L, listOf(entry(2, 110f, 3_300f)))

        // The app re-saves a session when a user retries after a sync failure.
        val afterRetry = stats.mergedWith(2, 2_000L, listOf(entry(2, 110f, 3_300f)))

        assertEquals(6_300f, afterRetry.totalVolume, 0.001f)
        assertEquals(2, afterRetry.sessionCount)
        assertEquals(2, afterRetry.recent.size)
    }

    @Test
    fun `re-saving a session with a corrected weight updates the point and the total`() {
        var stats = ExerciseStats(name = "Bench Press")
        stats = stats.mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))
        stats = stats.mergedWith(2, 2_000L, listOf(entry(2, 110f, 3_300f)))

        val corrected = stats.mergedWith(2, 2_000L, listOf(entry(2, 125f, 3_750f)))

        assertEquals(125f, corrected.bestWeight, 0.001f)
        assertEquals(6_750f, corrected.totalVolume, 0.001f)
        assertEquals(2, corrected.sessionCount)
    }

    @Test
    fun `recent points are ordered by session id so the trend halves stay stable`() {
        var stats = ExerciseStats(name = "Bench Press")
        stats = stats.mergedWith(3, 3_000L, listOf(entry(3, 120f, 3_600f)))
        stats = stats.mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))
        stats = stats.mergedWith(2, 2_000L, listOf(entry(2, 110f, 3_300f)))

        assertEquals(listOf(1, 2, 3), stats.recent.map { it.sessionId })
    }

    @Test
    fun `an empty entry list leaves the aggregate untouched`() {
        val seeded = ExerciseStats(name = "Bench Press")
            .mergedWith(1, 1_000L, listOf(entry(1, 100f, 3_000f)))

        val unchanged = seeded.mergedWith(2, 2_000L, emptyList())

        assertEquals(seeded, unchanged)
    }

    @Test
    fun `one aggregate per exercise name matches a full scan of the source rows`() {
        val rows = listOf(
            entry(1, 100f, 3_000f),
            entry(1, 80f, 2_400f),
            entry(2, 120f, 3_600f),
            entry(3, 90f, 2_700f)
        )

        // Incremental path.
        var stats = ExerciseStats(name = "Bench Press")
        listOf(1 to 1_000L, 2 to 2_000L, 3 to 3_000L).forEach { (sessionId, date) ->
            stats = stats.mergedWith(
                sessionId = sessionId,
                dateMillis = date,
                entries = rows.filter { it.sessionId == sessionId }
            )
        }

        // Full-scan path, which is what the app used to do.
        val expectedBest = rows.maxOf { it.weight }
        val expectedVolume = rows.sumOf { it.volume.toDouble() }.toFloat()
        val expectedSessions = rows.map { it.sessionId }.distinct().size

        assertEquals(expectedBest, stats.bestWeight, 0.001f)
        assertEquals(expectedVolume, stats.totalVolume, 0.001f)
        assertEquals(expectedSessions, stats.sessionCount)
        assertTrue(stats.recent.isNotEmpty())
    }
}
