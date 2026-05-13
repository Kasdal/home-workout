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
    val totalVolume: Float = 0f, // sets × reps × weight
    val calorieFormulaVersion: Int = 1,
    val calorieEstimateMode: String = "STANDARD_MET",
    val calorieIntensity: String = "normal",
    val calorieUserWeightKg: Float = 70f,
    val calorieMetCorrectionFactor: Float = 1f,
    val calorieActiveSeconds: Float = 0f,
    val calorieRestSeconds: Int = 0
)
