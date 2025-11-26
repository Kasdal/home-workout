package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_metrics")
data class UserMetrics(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String? = null,
    val weightKg: Float,
    val heightCm: Float,
    val age: Int,
    val gender: String, // "Male", "Female", "Other"
    val isActive: Boolean = false
)

