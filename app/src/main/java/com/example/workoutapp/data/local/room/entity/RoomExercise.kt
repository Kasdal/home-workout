package com.example.workoutapp.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class RoomExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weight: Float,
    val reps: Int = 13,
    val sets: Int = 4,
    val exerciseType: String,
    val usesSensor: Boolean = true,
    val holdDurationSeconds: Int = 30,
    val isDeleted: Boolean = false,
    val photoUri: String? = null
)
