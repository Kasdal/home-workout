package com.example.workoutapp.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionClockTest {

    @Test
    fun `start increments elapsed seconds every second`() = runTest {
        val clock = WorkoutSessionClock(backgroundScope)

        clock.start()
        assertEquals(0, clock.elapsedSeconds.value)

        advanceTimeBy(2000)
        runCurrent()

        assertEquals(2, clock.elapsedSeconds.value)
    }

    @Test
    fun `pause halts increments until resume restarts them`() = runTest {
        val clock = WorkoutSessionClock(backgroundScope)

        clock.start()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, clock.elapsedSeconds.value)

        clock.pause()
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(1, clock.elapsedSeconds.value)

        clock.resume()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(3, clock.elapsedSeconds.value)
    }

    @Test
    fun `stop cancels increments and resets elapsed seconds`() = runTest {
        val clock = WorkoutSessionClock(backgroundScope)

        clock.start()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, clock.elapsedSeconds.value)

        clock.stop()
        assertEquals(0, clock.elapsedSeconds.value)

        advanceTimeBy(2000)
        runCurrent()
        assertEquals(0, clock.elapsedSeconds.value)
    }
}
