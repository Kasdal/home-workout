package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class WorkoutCountdownOrchestratorFactory @Inject constructor() {
    fun create(
        scope: CoroutineScope,
        onCountdownWarning: () -> Unit,
        onTimerComplete: () -> Unit
    ): WorkoutCountdownOrchestrator {
        return WorkoutCountdownOrchestrator(
            scope = scope,
            onCountdownWarning = onCountdownWarning,
            onTimerComplete = onTimerComplete
        )
    }
}
