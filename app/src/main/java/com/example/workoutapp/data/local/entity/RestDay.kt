package com.example.workoutapp.data.local.entity

data class RestDay(
    val id: Int = 0,
    val date: Long, // Timestamp for the day
    val note: String? = null
)
