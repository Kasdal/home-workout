package com.example.workoutapp.ui.workout

import com.example.workoutapp.data.repository.SensorRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class WorkoutSensorOrchestratorFactory @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    fun create(
        scope: CoroutineScope,
        currentSetCompletionTarget: suspend () -> SensorSetCompletionTarget?,
        onSetCompletionTriggered: suspend (Int) -> Boolean
    ): WorkoutSensorOrchestrator {
        return WorkoutSensorOrchestrator(
            scope = scope,
            pollSensorStatus = sensorRepository::pollSensorStatus,
            currentSetCompletionTarget = currentSetCompletionTarget,
            onSetCompletionTriggered = onSetCompletionTriggered,
            resetCounter = sensorRepository::resetCounter
        )
    }
}
