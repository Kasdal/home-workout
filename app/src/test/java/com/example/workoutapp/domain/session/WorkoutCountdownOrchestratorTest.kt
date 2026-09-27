package com.example.workoutapp.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The injected clock and the coroutine scheduler are advanced independently on
 * purpose. That separation is what makes a CPU suspension reproducible: advance
 * the clock past a deadline while letting the countdown wake only once.
 *
 * A tick-decrementing implementation cannot be distinguished by virtual time
 * alone, because every delay fires exactly once per virtual second. That is why
 * the previous tests passed against an implementation that drifted and
 * mis-completed after a real suspension.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutCountdownOrchestratorTest {

    private companion object {
        const val TICK = 1_000L
    }

    private class FakeTime(var millis: Long = 0L) {
        fun advance(millis: Long) { this.millis += millis }
        val source: () -> Long = { this.millis }
    }

    private class Harness(
        val time: FakeTime = FakeTime(),
        val beeps: MutableList<Unit> = mutableListOf(),
        val completions: MutableList<Unit> = mutableListOf()
    )

    private fun TestScope.orchestrator(h: Harness): WorkoutCountdownOrchestrator =
        WorkoutCountdownOrchestrator(
            scope = backgroundScope,
            onCountdownWarning = { h.beeps += Unit },
            onTimerComplete = { h.completions += Unit },
            nowElapsedRealtime = h.time.source
        )

    /**
     * Normal running: the clock and the coroutine scheduler advance together in
     * one second steps, so the ticker wakes once per real second and each wake-up
     * observes a distinct clock reading. Advancing the clock to its final value
     * first would make every tick read the same instant.
     */
    private fun TestScope.run(h: Harness, elapsedMillis: Long) {
        var left = elapsedMillis
        while (left > 0) {
            val step = minOf(TICK, left)
            h.time.advance(step)
            advanceTimeBy(step)
            runCurrent()
            left -= step
        }
    }

    /**
     * A CPU suspension: the clock jumps forward while the ticker wakes only once.
     * This is the case a tick-decrementing implementation gets wrong, and the
     * reason the clock and the scheduler are advanced independently.
     */
    private fun TestScope.suspendFor(h: Harness, elapsedMillis: Long) {
        h.time.advance(elapsedMillis)
        advanceTimeBy(TICK)
        runCurrent()
    }

    @Test
    fun `startTimer counts down, beeps near the end, and completes`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(5)

        assertEquals(5, o.timerSeconds.value)
        assertTrue(o.isTimerRunning.value)
        assertFalse(o.isTimerPaused.value)

        run(h, 2_000)
        assertEquals(3, o.timerSeconds.value)
        assertEquals(1, h.beeps.size)

        run(h, 3_000)
        assertEquals(0, o.timerSeconds.value)
        assertFalse(o.isTimerRunning.value)
        assertFalse(o.isTimerPaused.value)
        assertEquals(CountdownType.NONE, o.timerType.value)
        assertEquals(3, h.beeps.size)
        assertEquals(listOf(Unit), h.completions)
    }

    @Test
    fun `pauseTimer halts countdown until resumeTimer restarts it`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(5)
        run(h, 1_000)
        assertEquals(4, o.timerSeconds.value)

        o.pauseTimer()
        assertEquals(4, o.timerSeconds.value)
        assertFalse(o.isTimerRunning.value)
        assertTrue(o.isTimerPaused.value)

        // Time passing while paused must not consume the countdown.
        h.time.advance(3_000)
        advanceTimeBy(TICK)
        runCurrent()
        assertEquals(4, o.timerSeconds.value)
        assertTrue(h.completions.isEmpty())

        o.resumeTimer()
        assertTrue(o.isTimerRunning.value)
        assertFalse(o.isTimerPaused.value)

        run(h, 1_000)
        assertEquals(3, o.timerSeconds.value)
    }

    @Test
    fun `stopTimer cancels the countdown and leaves no way to resume it`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(5)
        run(h, 1_000)

        o.stopTimer()

        assertEquals(4, o.timerSeconds.value)
        assertFalse(o.isTimerRunning.value)
        assertFalse(o.isTimerPaused.value)
        assertEquals(CountdownType.NONE, o.timerType.value)

        run(h, 3_000)
        assertEquals(4, o.timerSeconds.value)

        // A stopped timer is not paused, so resume must be ignored. Previously a
        // stopped timer could be revived into a running countdown with no type.
        o.resumeTimer()
        assertFalse(o.isTimerRunning.value)

        run(h, 1_000)
        assertEquals(4, o.timerSeconds.value)
        assertTrue(h.completions.isEmpty())
    }

    @Test
    fun `skipTimer ends early, fires completion once, and clears state`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(30, CountdownType.REST)
        run(h, 2_000)
        assertEquals(28, o.timerSeconds.value)

        o.skipTimer()

        assertEquals(0, o.timerSeconds.value)
        assertFalse(o.isTimerRunning.value)
        assertFalse(o.isTimerPaused.value)
        assertEquals(CountdownType.NONE, o.timerType.value)
        assertEquals(listOf(Unit), h.completions)

        run(h, 5_000)
        assertEquals(1, h.completions.size)
    }

    @Test
    fun `startTimer records total seconds so the UI can draw progress`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(30, CountdownType.REST)
        assertEquals(30, o.timerTotalSeconds.value)

        run(h, 5_000)
        assertEquals(25, o.timerSeconds.value)
        assertEquals(30, o.timerTotalSeconds.value)

        o.skipTimer()
        assertEquals(0, o.timerTotalSeconds.value)
    }

    @Test
    fun `timer type is tracked while running and cleared on stop`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(10, CountdownType.SWITCH)
        assertEquals(CountdownType.SWITCH, o.timerType.value)

        o.stopTimer()
        assertEquals(CountdownType.NONE, o.timerType.value)
    }

    /**
     * The core regression. A tick-decrementing countdown removes one second per
     * wake-up, so a 20 minute suspension still leaves 80 of 90 seconds showing.
     * Reading the deadline completes it on the first tick after the CPU wakes.
     */
    @Test
    fun `a long CPU suspension completes the countdown instead of stalling it`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(90, CountdownType.REST)
        run(h, 1_000)
        assertEquals(89, o.timerSeconds.value)

        // Backgrounded for 20 minutes. The countdown wakes exactly once.
        suspendFor(h, 20 * 60 * 1_000)

        assertEquals(0, o.timerSeconds.value)
        assertEquals(listOf(Unit), h.completions)
    }

    @Test
    fun `a zero second timer does not fire a completion`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        // The settings dialog coerces the rest and switch durations to >= 0, so a
        // user can save 0.
        o.startTimer(0, CountdownType.REST)

        assertEquals(0, o.timerSeconds.value)
        assertFalse(o.isTimerRunning.value)
        assertEquals(CountdownType.NONE, o.timerType.value)
        assertTrue(
            "A zero second timer must not fire the completion callback",
            h.completions.isEmpty()
        )
        assertTrue(h.beeps.isEmpty())

        run(h, 5_000)
        assertTrue(h.completions.isEmpty())
    }

    @Test
    fun `a negative duration is treated as no timer`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(-10, CountdownType.REST)

        assertFalse(o.isTimerRunning.value)
        assertTrue(h.completions.isEmpty())
    }

    @Test
    fun `a double resume does not run the countdown at double speed`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(10, CountdownType.REST)
        run(h, 2_000)
        assertEquals(8, o.timerSeconds.value)

        o.pauseTimer()
        o.resumeTimer()
        o.resumeTimer()
        o.resumeTimer()

        // Two concurrent decrementing loops would burn four seconds here.
        run(h, 1_000)
        assertEquals(7, o.timerSeconds.value)
    }

    @Test
    fun `restarting a timer replaces the previous one and does not double complete`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(5)
        run(h, 1_000)
        o.startTimer(5)
        run(h, 1_000)

        assertEquals(4, o.timerSeconds.value)

        run(h, 4_000)
        assertEquals(0, o.timerSeconds.value)
        assertEquals(1, h.completions.size)
    }

    @Test
    fun `beeps are not repeated for a single suspended wake-up`() = runTest {
        val h = Harness()
        val o = orchestrator(h)

        o.startTimer(3, CountdownType.REST)

        // One wake-up, with 10 seconds of real time passing. A per-tick
        // implementation would beep once per virtual second and emit ten beeps.
        suspendFor(h, 10_000)

        assertTrue(
            "Expected at most one warning per wake-up, got ${h.beeps.size}",
            h.beeps.size <= 1
        )
        assertEquals(listOf(Unit), h.completions)
    }
}
