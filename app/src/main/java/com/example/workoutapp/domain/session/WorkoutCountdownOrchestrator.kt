package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CountdownType {
    NONE,
    REST,
    SWITCH,
    HOLD
}

/**
 * Runs a rest, switch or hold countdown.
 *
 * Remaining time is derived from a deadline on a monotonic clock rather than
 * decremented once per wake-up. The previous implementation ran
 * `while (seconds > 0) { delay(1000); seconds-- }`, which counted iterations:
 *
 *  - Drift. Each iteration is 1000 ms plus scheduling latency, uncorrected, so a
 *    countdown ran long and beeps landed late.
 *  - Suspension. If the CPU is suspended, delay never fires, so a 90 second rest
 *    could sit at 80 for twenty minutes and then complete in a single tick.
 *
 * Reading the deadline on every tick makes both self-correcting, and it also
 * keeps the warning beeps aligned to real seconds.
 *
 * [nowElapsedRealtime] must be monotonic, such as `SystemClock.elapsedRealtime()`.
 * A wall clock is wrong because the device time can move mid-countdown. It is
 * injected so this class keeps no android.* imports and stays JVM testable.
 */
class WorkoutCountdownOrchestrator(
    private val scope: CoroutineScope,
    private val onCountdownWarning: () -> Unit,
    private val onTimerComplete: () -> Unit,
    private val nowElapsedRealtime: () -> Long
) {

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(0)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()

    private val _timerType = MutableStateFlow(CountdownType.NONE)
    val timerType: StateFlow<CountdownType> = _timerType.asStateFlow()

    private var timerJob: Job? = null

    /** Wall position at which the countdown reaches zero, or null when not running. */
    private var deadlineMillis: Long? = null

    /** Time left captured at the moment the countdown was paused. */
    private var remainingWhenPausedMillis: Long = 0L

    /**
     * Last displayed second that produced a warning beep.
     *
     * Without this, a ticker that catches up after a CPU suspension can wake
     * twice while the clock reports the same remaining second, and the user hears
     * the warning beep doubled. Tracking the last warned value guarantees exactly
     * one beep per displayed second.
     */
    private var lastWarnedSecond: Int? = null

    fun startTimer(seconds: Int, type: CountdownType = CountdownType.REST) {
        timerJob?.cancel()
        timerJob = null

        val total = seconds.coerceAtLeast(0)
        _timerTotalSeconds.value = total
        _timerType.value = type
        _isTimerPaused.value = false

        // A zero or negative duration is reachable: the settings dialog coerces
        // the rest and switch durations to >= 0. Starting a countdown of zero must
        // not fire a completion callback, which would play the finish cue and
        // advance session state for a period that never ran.
        if (total <= 0) {
            clearCountdown()
            return
        }

        remainingWhenPausedMillis = total * 1000L
        deadlineMillis = nowElapsedRealtime() + remainingWhenPausedMillis
        _timerSeconds.value = total
        _isTimerRunning.value = true
        lastWarnedSecond = null
        startTimerJob()
    }

    fun pauseTimer() {
        if (deadlineMillis != null) {
            remainingWhenPausedMillis = millisRemaining()
        }
        deadlineMillis = null
        timerJob?.cancel()
        timerJob = null
        _isTimerPaused.value = true
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        // Only a paused countdown may be resumed. Without this guard a stopped
        // timer, which intentionally keeps its remaining seconds, could be
        // revived into a running countdown with no type.
        if (!_isTimerPaused.value) return
        if (remainingWhenPausedMillis <= 0L) return
        if (timerJob?.isActive == true) return

        deadlineMillis = nowElapsedRealtime() + remainingWhenPausedMillis
        _isTimerPaused.value = false
        _isTimerRunning.value = true
        _timerSeconds.value = secondsRemaining()
        lastWarnedSecond = null
        startTimerJob()
    }

    fun skipTimer() {
        if (deadlineMillis == null && timerJob?.isActive != true) return
        timerJob?.cancel()
        timerJob = null
        clearCountdown()
        onTimerComplete()
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        deadlineMillis = null
        remainingWhenPausedMillis = 0L
        _isTimerRunning.value = false
        _isTimerPaused.value = false
        _timerType.value = CountdownType.NONE
        _timerTotalSeconds.value = 0
    }

    private fun millisRemaining(): Long {
        val deadline = deadlineMillis ?: return remainingWhenPausedMillis
        return (deadline - nowElapsedRealtime()).coerceAtLeast(0L)
    }

    /** Rounds up, so a countdown showing 1 still has a second left to run. */
    private fun secondsRemaining(): Int =
        ((millisRemaining() + 999L) / 1000L).toInt()

    private fun clearCountdown() {
        deadlineMillis = null
        remainingWhenPausedMillis = 0L
        lastWarnedSecond = null
        _timerSeconds.value = 0
        _timerTotalSeconds.value = 0
        _isTimerRunning.value = false
        _isTimerPaused.value = false
        _timerType.value = CountdownType.NONE
    }

    private fun startTimerJob() {
        // Always cancel first. resumeTimer() used to skip this, so a double
        // resume left two coroutines decrementing the same countdown and it ran
        // at double speed.
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = secondsRemaining()
                _timerSeconds.value = remaining
                if (remaining <= 0) break
                if (remaining <= WARNING_THRESHOLD_SECONDS && lastWarnedSecond != remaining) {
                    lastWarnedSecond = remaining
                    onCountdownWarning()
                }
                delay(TICK_MILLIS)
            }

            clearCountdown()
            onTimerComplete()
        }
    }

    private companion object {
        const val TICK_MILLIS = 1000L
        const val WARNING_THRESHOLD_SECONDS = 3
    }
}
