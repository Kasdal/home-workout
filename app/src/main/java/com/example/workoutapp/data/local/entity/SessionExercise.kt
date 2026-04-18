package com.example.workoutapp.data.local.entity

data class SessionExercise(
    val id: Int = 0,
    val sessionId: Int,
    val exerciseName: String,
    val weight: Float,
    val sets: Int,
    val reps: Int,
    val volume: Float, // sets × reps × weight
    val sortOrder: Int = 0
)
