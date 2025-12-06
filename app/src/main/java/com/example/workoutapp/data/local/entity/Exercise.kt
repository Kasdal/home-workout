package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weight: Float,
    val reps: Int = 13, // Customizable reps per set
    val sets: Int = 4,  // Customizable number of sets
    val isDeleted: Boolean = false,
    val photoUri: String? = null // Exercise photo URI
)
