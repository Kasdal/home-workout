package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ticks [elapsedSeconds] once per second while running. Subclasses may override
 * the lifecycle methods to control ticking manually (used by unit tests).
 */
open class WorkoutSessionClock(
    private val scope: CoroutineScope
) {

    protected val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    open fun start() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0
        startTimerJob()
    }

    open fun pause() {
        timerJob?.cancel()
    }

    open fun resume() {
        if (timerJob?.isActive == true) return
        startTimerJob()
    }

    open fun stop() {
        timerJob?.cancel()
        timerJob = null
        _elapsedSeconds.value = 0
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                _elapsedSeconds.value++
            }
        }
    }
}
