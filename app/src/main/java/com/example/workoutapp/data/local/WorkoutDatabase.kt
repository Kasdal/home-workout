package com.example.workoutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession

@Database(
    entities = [Exercise::class, UserMetrics::class, WorkoutSession::class, Settings::class, RestDay::class],
    version = 4,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
