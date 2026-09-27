package com.example.workoutapp.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * These tests drive an injected fake time source rather than relying on virtual
 * time alone, because the property under test is that elapsed time is READ FROM A
 * CLOCK rather than accumulated per tick.
 *
 * Two independent things are advanced on purpose:
 *
 *  - the fake monotonic clock, which is what the implementation reads, and
 *  - the coroutine scheduler, which is what wakes the ticker.
 *
 * That separation is what makes a CPU suspension reproducible. Advancing the
 * clock by 20 minutes while letting the ticker wake exactly once models Doze or
 * App Standby holding the CPU, after which a tick-counting implementation would
 * report one more second and this implementation reports the truth.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionClockTest {

    private companion object {
        const val TICK = 1_000L
    }

    /** Mutable monotonic clock the test controls, standing in for elapsedRealtime. */
    private class FakeTime(var millis: Long = 0L) {
        fun advance(millis: Long) { this.millis += millis }
        val source: () -> Long = { this.millis }
    }

    /** Advance the clock, then let the ticker wake exactly once. */
    private fun TestScope.tick(time: FakeTime, elapsedMillis: Long) {
        time.advance(elapsedMillis)
        advanceTimeBy(TICK)
        runCurrent()
    }

    @Test
    fun `start resets elapsed to zero`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()

        assertEquals(0, clock.elapsedSeconds.value)
    }

    @Test
    fun `elapsed follows the clock once per second`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()

        tick(time, 1_000)
        assertEquals(1, clock.elapsedSeconds.value)

        tick(time, 2_000)
        assertEquals(3, clock.elapsedSeconds.value)
    }

    @Test
    fun `pause banks elapsed time and resume continues from it`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()
        tick(time, 5_000)
        assertEquals(5, clock.elapsedSeconds.value)

        clock.pause()

        // Time that passes while paused must never be billed to the session.
        time.advance(60_000)
        advanceTimeBy(TICK)
        runCurrent()
        assertEquals(5, clock.elapsedSeconds.value)

        clock.resume()
        runCurrent()
        tick(time, 2_000)
        assertEquals(7, clock.elapsedSeconds.value)
    }

    @Test
    fun `stop cancels ticking and resets elapsed seconds`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()
        tick(time, 3_000)
        assertEquals(3, clock.elapsedSeconds.value)

        clock.stop()
        assertEquals(0, clock.elapsedSeconds.value)

        tick(time, 10_000)
        assertEquals(0, clock.elapsedSeconds.value)
    }

    /**
     * The core regression. A tick-counting clock adds one second per wake-up, so a
     * 20 minute CPU suspension reports 1 second. Reading the clock reports the
     * truth on the next tick.
     */
    @Test
    fun `a long CPU suspension reports true elapsed time not one tick`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()
        tick(time, 1_000)
        assertEquals(1, clock.elapsedSeconds.value)

        // The app is backgrounded for 20 minutes. The clock advances but the
        // ticker wakes only once. A tick counter would end this at 2.
        tick(time, 20 * 60 * 1_000)

        assertEquals(
            "A 20 minute suspension must not count as a single tick",
            20 * 60 + 1,
            clock.elapsedSeconds.value
        )
    }

    @Test
    fun `drift across many intervals does not accumulate`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()

        // 45 minutes in one-minute steps, with the ticker waking once per step.
        // Only the clock decides the total, so scheduler jitter cannot leak in.
        repeat(45) { tick(time, 60_000) }

        assertEquals(45 * 60, clock.elapsedSeconds.value)
    }

    @Test
    fun `resume is ignored when the clock is already running`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()
        tick(time, 4_000)

        // A double resume must not open a second run segment, which would reset
        // the segment start and lose the four seconds already banked.
        clock.resume()
        runCurrent()
        tick(time, 1_000)

        assertEquals(5, clock.elapsedSeconds.value)
    }

    @Test
    fun `a backwards time source cannot reduce elapsed seconds`() = runTest {
        val time = FakeTime()
        val clock = WorkoutSessionClock(backgroundScope, time.source)

        clock.start()
        runCurrent()
        tick(time, 5_000)
        assertEquals(5, clock.elapsedSeconds.value)

        // elapsedRealtime is monotonic, but the clamp means a bad source cannot
        // silently reduce a recorded session duration.
        time.millis -= 10_000
        runCurrent()

        assertEquals(5, clock.elapsedSeconds.value)
    }
}
