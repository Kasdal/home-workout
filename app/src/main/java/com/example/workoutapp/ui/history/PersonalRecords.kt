package com.example.workoutapp.ui.history

data class PersonalRecords(
    val heaviestLiftByExercise: Map<String, Float>,
    val mostVolume: Float,
    val longestSession: Int,
    val currentStreak: Int,
    val totalWorkouts: Int,
    val totalVolume: Float = 0f,
    val totalDurationMin: Int = 0
)
