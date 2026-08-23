package com.example.workoutapp.model

data class WorkoutTemplate(
    val id: String = "",
    val name: String,
    val exerciseIds: List<Int> = emptyList(),
    val sortOrder: Int = 0
)
