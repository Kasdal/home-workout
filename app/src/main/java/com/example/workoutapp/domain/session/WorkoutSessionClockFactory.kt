package com.example.workoutapp.domain.session

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class WorkoutSessionClockFactory @Inject constructor() {
    fun create(scope: CoroutineScope): WorkoutSessionClock {
        return WorkoutSessionClock(scope)
    }
}
