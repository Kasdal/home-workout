package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks elapsed session time from timestamps rather than by counting ticks.
 *
 * The previous implementation ran `while (true) { delay(1000); elapsed++ }`.
 * That counts iterations, not elapsed time, which produced two defects:
 *
 *  - Drift. Every iteration lasts 1000 ms plus the scheduling latency of resuming
 *    and calling delay again. Nothing corrects it, so a long session
 *    systematically under-reports, by roughly 3 to 14 seconds over 45 minutes.
 *  - Suspension. If the CPU is suspended by Doze, App Standby, or process
 *    pressure, delay does not fire at all. When the CPU resumes the loop ticks
 *    exactly once, so a 20 minute gap adds one second instead of 1200. That
 *    elapsed value becomes `WorkoutSession.durationSeconds` and the break
 *    calorie figure, so a single backgrounded stretch of a session permanently
 *    records wrong data with no error surfaced.
 *
 * Reading elapsed from a monotonic clock on every tick makes both cases
 * self-correcting: the first tick after the CPU wakes reports the true elapsed
 * time rather than one more second.
 *
 * [nowElapsedRealtime] must be a monotonic millisecond source such as
 * `SystemClock.elapsedRealtime()`. A wall clock is wrong here, because the device
 * time or an NTP correction can move it mid-session. The source is injected so
 * this class keeps no android.* imports and stays testable on a plain JVM.
 */
open class WorkoutSessionClock(
    private val scope: CoroutineScope,
    private val nowElapsedRealtime: () -> Long
) {
    protected val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    /** Time banked from run segments that have already been paused or stopped. */
    private var accumulatedMillis = 0L

    /** Start of the segment currently being timed, or null while not running. */
    private var segmentStartMillis: Long? = null

    open fun start() {
        timerJob?.cancel()
        timerJob = null
        accumulatedMillis = 0L
        segmentStartMillis = nowElapsedRealtime()
        _elapsedSeconds.value = 0
        startTimerJob()
    }

    open fun pause() {
        bankCurrentSegment()
        timerJob?.cancel()
        timerJob = null
    }

    open fun resume() {
        if (timerJob?.isActive == true) return
        if (segmentStartMillis != null) return
        segmentStartMillis = nowElapsedRealtime()
        publishElapsed()
        startTimerJob()
    }

    open fun stop() {
        timerJob?.cancel()
        timerJob = null
        accumulatedMillis = 0L
        segmentStartMillis = null
        _elapsedSeconds.value = 0
    }

    /**
     * Folds the running segment into [accumulatedMillis] and closes it, so time
     * spent paused is never billed to the session.
     */
    private fun bankCurrentSegment() {
        val start = segmentStartMillis ?: return
        accumulatedMillis += (nowElapsedRealtime() - start).coerceAtLeast(0L)
        segmentStartMillis = null
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                publishElapsed()
                delay(TICK_MILLIS)
            }
        }
    }

    private fun publishElapsed() {
        val start = segmentStartMillis ?: return
        val total = accumulatedMillis + (nowElapsedRealtime() - start).coerceAtLeast(0L)
        _elapsedSeconds.value = (total / 1000L).toInt()
    }

    private companion object {
        const val TICK_MILLIS = 1000L
    }
}
