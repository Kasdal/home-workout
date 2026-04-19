package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutSessionClock(
    private val scope: CoroutineScope
) {

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun start() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0
        startTimerJob()
    }

    fun pause() {
        timerJob?.cancel()
    }

    fun resume() {
        if (timerJob?.isActive == true) return
        startTimerJob()
    }

    fun stop() {
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
