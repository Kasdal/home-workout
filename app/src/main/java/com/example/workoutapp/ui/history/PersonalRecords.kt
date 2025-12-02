package com.example.workoutapp.ui.history

data class PersonalRecords(
    val heaviestLiftByExercise: Map<String, Float>, // Exercise name -> max weight
    val mostVolume: Float,
    val longestSession: Int, // minutes
    val currentStreak: Int, // days
    val totalWorkouts: Int
)
