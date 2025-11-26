package com.example.workoutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession

@Database(
    entities = [Exercise::class, UserMetrics::class, WorkoutSession::class],
    version = 2,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
