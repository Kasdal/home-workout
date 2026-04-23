package com.example.workoutapp.model

enum class ExerciseType {
    STANDARD,
    BODYWEIGHT,
    HOLD
}

data class Exercise(
    val id: Int = 0,
    val name: String,
    val weight: Float,
    val reps: Int = 13, // Customizable reps per set
    val sets: Int = 4,  // Customizable number of sets
    val exerciseType: String = ExerciseType.STANDARD.name,
    val usesSensor: Boolean = true,
    val holdDurationSeconds: Int = 30,
    val isDeleted: Boolean = false,
    val photoUri: String? = null // Exercise photo URI
)
