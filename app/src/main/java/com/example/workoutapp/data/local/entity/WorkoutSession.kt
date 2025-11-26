package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val durationSeconds: Long,
    val totalWeightLifted: Float,
    val caloriesBurned: Float,
    val notes: String? = null,
    val isPaused: Boolean = false,
    val pausedAt: Long? = null,
    val timeOfDay: Int = 0, // Hour 0-23 when workout started
    val totalVolume: Float = 0f // sets × reps × weight
)
