package com.example.workoutapp.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rest_days")
data class RoomRestDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val note: String? = null
)
