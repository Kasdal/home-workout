package com.example.workoutapp.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutCountdownOrchestratorTest {

    @Test
    fun `startTimer counts down, beeps near the end, and stops when finished`() = runTest {
        val beeps = mutableListOf<Unit>()
        val orchestrator = WorkoutCountdownOrchestrator(
            scope = backgroundScope,
            onTimerSound = { beeps += Unit }
        )

        orchestrator.startTimer(5)

        assertEquals(5, orchestrator.timerSeconds.value)
        assertTrue(orchestrator.isTimerRunning.value)
        assertFalse(orchestrator.isTimerPaused.value)

        advanceTimeBy(2000)
        runCurrent()

        assertEquals(3, orchestrator.timerSeconds.value)
        assertEquals(1, beeps.size)

        advanceTimeBy(3000)
        runCurrent()

        assertEquals(0, orchestrator.timerSeconds.value)
        assertFalse(orchestrator.isTimerRunning.value)
        assertFalse(orchestrator.isTimerPaused.value)
        assertEquals(4, beeps.size)
    }

    @Test
    fun `pauseTimer halts countdown until resumeTimer restarts it`() = runTest {
        val orchestrator = WorkoutCountdownOrchestrator(
            scope = backgroundScope,
            onTimerSound = {}
        )

        orchestrator.startTimer(5)
        advanceTimeBy(1000)
        runCurrent()

        orchestrator.pauseTimer()
        assertEquals(4, orchestrator.timerSeconds.value)
        assertFalse(orchestrator.isTimerRunning.value)
        assertTrue(orchestrator.isTimerPaused.value)

        advanceTimeBy(3000)
        runCurrent()
        assertEquals(4, orchestrator.timerSeconds.value)

        orchestrator.resumeTimer()
        assertTrue(orchestrator.isTimerRunning.value)
        assertFalse(orchestrator.isTimerPaused.value)

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(3, orchestrator.timerSeconds.value)
    }

    @Test
    fun `stopTimer cancels countdown without resetting remaining seconds`() = runTest {
        val orchestrator = WorkoutCountdownOrchestrator(
            scope = backgroundScope,
            onTimerSound = {}
        )

        orchestrator.startTimer(5)
        advanceTimeBy(1000)
        runCurrent()

        orchestrator.stopTimer()

        assertEquals(4, orchestrator.timerSeconds.value)
        assertFalse(orchestrator.isTimerRunning.value)
        assertFalse(orchestrator.isTimerPaused.value)

        advanceTimeBy(3000)
        runCurrent()
        assertEquals(4, orchestrator.timerSeconds.value)
    }
}
