package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutCountdownOrchestrator(
    private val scope: CoroutineScope,
    private val onCountdownWarning: () -> Unit,
    private val onTimerComplete: () -> Unit
) {

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        _isTimerPaused.value = false
        startTimerJob()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isTimerPaused.value = true
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        if (_timerSeconds.value <= 0) return

        _isTimerPaused.value = false
        _isTimerRunning.value = true
        startTimerJob()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = false
    }

    private fun startTimerJob() {
        timerJob = scope.launch {
            while (_timerSeconds.value > 0) {
                val remaining = _timerSeconds.value
                if (remaining <= 3) {
                    onCountdownWarning()
                }
                delay(1000L)
                _timerSeconds.value--
            }

            onTimerComplete()
            _isTimerRunning.value = false
            _isTimerPaused.value = false
        }
    }
}
