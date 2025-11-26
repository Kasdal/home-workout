package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weight: Float,
    val sets: Int = 4,
    val reps: Int = 13,
    val isDeleted: Boolean = false
)
