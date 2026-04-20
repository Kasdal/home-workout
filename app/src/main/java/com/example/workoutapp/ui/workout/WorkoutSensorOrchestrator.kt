package com.example.workoutapp.ui.workout

import com.example.workoutapp.data.remote.EspSensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WorkoutSensorSnapshot(
    val reps: Int = 0,
    val state: String = "REST",
    val distance: Int = 0,
    val connected: Boolean = false
)

data class SensorSetCompletionTarget(
    val exerciseId: Int,
    val targetReps: Int
)

class WorkoutSensorOrchestrator(
    private val scope: CoroutineScope,
    private val pollSensorStatus: (String, Long) -> Flow<EspSensorData?>,
    private val currentSetCompletionTarget: suspend () -> SensorSetCompletionTarget? = { null },
    private val onSetCompletionTriggered: suspend (Int) -> Boolean = { false },
    private val resetCounter: suspend (String) -> Boolean = { false },
    private val completionResetDelayMs: Long = 1000
) {

    private val _sensorSnapshot = MutableStateFlow(WorkoutSensorSnapshot())
    val sensorSnapshot: StateFlow<WorkoutSensorSnapshot> = _sensorSnapshot.asStateFlow()
    val isPolling: Boolean
        get() = sensorPollingJob?.isActive == true

    private var sensorPollingJob: Job? = null
    private var pendingResetJob: Job? = null
    private var lastSensorReps = 0
    private var currentIpAddress: String? = null

    fun start(ipAddress: String, intervalMs: Long = 200) {
        sensorPollingJob?.cancel()
        pendingResetJob?.cancel()
        currentIpAddress = ipAddress
        lastSensorReps = 0
        val pollingJob = scope.launch {
            pollSensorStatus(ipAddress, intervalMs)
                .catch { emitAll(flowOf(null)) }
                .collect { sensorData ->
                    val snapshot = if (sensorData != null) {
                        WorkoutSensorSnapshot(
                            reps = sensorData.reps,
                            state = sensorData.state,
                            distance = sensorData.dist,
                            connected = true
                        )
                    } else {
                        _sensorSnapshot.value.copy(connected = false)
                    }

                    _sensorSnapshot.value = snapshot
                    maybeTriggerSetCompletion(snapshot.reps)
                    lastSensorReps = snapshot.reps
                }
        }
        pollingJob.invokeOnCompletion {
            if (sensorPollingJob === pollingJob) {
                sensorPollingJob = null
            }
        }
        sensorPollingJob = pollingJob
    }

    fun stop() {
        sensorPollingJob?.cancel()
        sensorPollingJob = null
        pendingResetJob?.cancel()
        pendingResetJob = null
        currentIpAddress = null
        lastSensorReps = 0
        _sensorSnapshot.value = WorkoutSensorSnapshot()
    }

    fun resetRepTracking() {
        lastSensorReps = 0
    }

    private suspend fun maybeTriggerSetCompletion(currentReps: Int) {
        if (currentReps <= 0 || currentReps <= lastSensorReps) {
            return
        }

        val target = currentSetCompletionTarget() ?: return
        if (lastSensorReps < target.targetReps && currentReps >= target.targetReps) {
            if (onSetCompletionTriggered(target.exerciseId)) {
                scheduleCounterReset()
            }
        }
    }

    private fun scheduleCounterReset() {
        val ipAddress = currentIpAddress ?: return
        pendingResetJob?.cancel()
        pendingResetJob = scope.launch {
            kotlinx.coroutines.delay(completionResetDelayMs)
            if (!isActive) {
                return@launch
            }

            val didReset = resetCounter(ipAddress)
            if (!didReset || !isActive) {
                return@launch
            }

            resetRepTracking()
            _sensorSnapshot.value = _sensorSnapshot.value.copy(reps = 0)
        }
    }
}
