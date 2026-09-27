package com.example.workoutapp.domain.session

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * Supplies [WorkoutSessionClock] with a monotonic time source.
 *
 * The Android dependency lives here rather than in [WorkoutSessionClock] so the
 * clock itself stays free of android.* imports and remains testable on a plain
 * JVM with a fake time source.
 */
class WorkoutSessionClockFactory @Inject constructor() {
    fun create(scope: CoroutineScope): WorkoutSessionClock {
        return WorkoutSessionClock(scope) { SystemClock.elapsedRealtime() }
    }
}
