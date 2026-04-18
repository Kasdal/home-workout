package com.example.workoutapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.room.entity.RoomExercise
import com.example.workoutapp.data.local.room.entity.RoomRestDay
import com.example.workoutapp.data.local.room.entity.RoomSessionExercise
import com.example.workoutapp.data.local.room.entity.RoomSettings
import com.example.workoutapp.data.local.room.entity.RoomUserMetrics
import com.example.workoutapp.data.local.room.entity.RoomWorkoutSession

@Database(
    entities = [RoomExercise::class, RoomUserMetrics::class, RoomWorkoutSession::class, RoomSettings::class, RoomRestDay::class, RoomSessionExercise::class],
    version = 9,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
