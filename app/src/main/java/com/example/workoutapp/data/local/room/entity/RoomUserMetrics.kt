package com.example.workoutapp.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_metrics")
data class RoomUserMetrics(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String? = null,
    val weightKg: Float,
    val heightCm: Float = 170f,
    val age: Int = 30,
    val gender: String = "Male",
    val isActive: Boolean = false
)
