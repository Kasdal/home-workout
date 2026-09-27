package com.example.workoutapp.domain.session

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * Supplies [WorkoutCountdownOrchestrator] with a monotonic time source.
 *
 * The Android dependency lives here rather than in the orchestrator so the
 * orchestrator keeps no android.* imports and stays testable on a plain JVM.
 */
class WorkoutCountdownOrchestratorFactory @Inject constructor() {
    fun create(
        scope: CoroutineScope,
        onCountdownWarning: () -> Unit,
        onTimerComplete: () -> Unit
    ): WorkoutCountdownOrchestrator {
        return WorkoutCountdownOrchestrator(
            scope = scope,
            onCountdownWarning = onCountdownWarning,
            onTimerComplete = onTimerComplete,
            nowElapsedRealtime = { SystemClock.elapsedRealtime() }
        )
    }
}
