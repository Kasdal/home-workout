package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_metrics")
data class UserMetrics(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String? = null,
    val weightKg: Float,
    val heightCm: Float = 170f,
    val age: Int = 30,
    val gender: String = "Male", // "Male", "Female", "Other"
    val isActive: Boolean = false
)

