package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class WorkoutCountdownOrchestratorFactory @Inject constructor() {
    fun create(
        scope: CoroutineScope,
        onTimerSound: () -> Unit
    ): WorkoutCountdownOrchestrator {
        return WorkoutCountdownOrchestrator(
            scope = scope,
            onTimerSound = onTimerSound
        )
    }
}
