package com.example.workoutapp.domain.stats

import com.example.workoutapp.model.SessionExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneCalculatorTest {

    @Test
    fun `no milestones before first workout`() {
        val milestones = MilestoneCalculator.unlockedMilestones(
            totalWorkouts = 0,
            currentStreakDays = 0,
            hasAnyLoggedLift = false
        )

        assertTrue(milestones.isEmpty())
    }

    @Test
    fun `session and streak thresholds unlock in order`() {
        val milestones = MilestoneCalculator.unlockedMilestones(
            totalWorkouts = 25,
            currentStreakDays = 7,
            hasAnyLoggedLift = true
        )

        assertEquals(
            listOf("1 sessions", "10 sessions", "25 sessions", "3-day streak", "7-day streak", "First PR"),
            milestones
        )
    }

    @Test
    fun `first PR milestone needs at least one logged lift`() {
        val withoutLift = MilestoneCalculator.unlockedMilestones(1, 1, hasAnyLoggedLift = false)
        val withLift = MilestoneCalculator.unlockedMilestones(1, 1, hasAnyLoggedLift = true)

        assertFalse(withoutLift.contains("First PR"))
        assertTrue(withLift.contains("First PR"))
    }

    @Test
    fun `holdsPersonalRecord is false with no history`() {
        assertFalse(MilestoneCalculator.holdsPersonalRecord(emptyList()))
    }

    @Test
    fun `holdsPersonalRecord is true when latest session matches all-time top weight`() {
        val history = listOf(
            entry(sessionId = 1, weight = 80f),
            entry(sessionId = 2, weight = 90f),
            entry(sessionId = 2, weight = 85f)
        )

        assertTrue(MilestoneCalculator.holdsPersonalRecord(history))
    }

    @Test
    fun `holdsPersonalRecord is false when best came from an older session`() {
        val history = listOf(
            entry(sessionId = 1, weight = 100f),
            entry(sessionId = 2, weight = 90f)
        )

        assertFalse(MilestoneCalculator.holdsPersonalRecord(history))
    }

    @Test
    fun `holdsPersonalRecord ignores zero-weight entries`() {
        val history = listOf(entry(sessionId = 1, weight = 0f))

        assertFalse(MilestoneCalculator.holdsPersonalRecord(history))
    }

    private fun entry(sessionId: Int, weight: Float) = SessionExercise(
        id = 0,
        sessionId = sessionId,
        exerciseName = "Squat",
        weight = weight,
        sets = 3,
        reps = 5,
        volume = weight * 15
    )
}
