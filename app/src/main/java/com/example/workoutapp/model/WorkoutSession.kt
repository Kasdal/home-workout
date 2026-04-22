package com.example.workoutapp.model

data class WorkoutSession(
    val id: Int = 0,
    val date: Long,
    val durationSeconds: Long,
    val totalWeightLifted: Float,
    val caloriesBurned: Float,
    val notes: String? = null,
    val isPaused: Boolean = false,
    val pausedAt: Long? = null,
    val timeOfDay: Int = 0, // Hour 0-23 when workout started
    val totalVolume: Float = 0f // sets × reps × weight
)
