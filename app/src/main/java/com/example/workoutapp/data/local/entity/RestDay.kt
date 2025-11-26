package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rest_days")
data class RestDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // Timestamp for the day
    val note: String? = null
)
