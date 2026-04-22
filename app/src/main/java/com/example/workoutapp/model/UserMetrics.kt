package com.example.workoutapp.model

data class UserMetrics(
    val id: Int = 0,
    val name: String? = null,
    val weightKg: Float,
    val heightCm: Float = 170f,
    val age: Int = 30,
    val gender: String = "Male", // "Male", "Female", "Other"
    val isActive: Boolean = false
)
