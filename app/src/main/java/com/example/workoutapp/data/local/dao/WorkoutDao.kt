package com.example.workoutapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.workoutapp.data.local.room.entity.RoomExercise
import com.example.workoutapp.data.local.room.entity.RoomRestDay
import com.example.workoutapp.data.local.room.entity.RoomSessionExercise
import com.example.workoutapp.data.local.room.entity.RoomSettings
import com.example.workoutapp.data.local.room.entity.RoomUserMetrics
import com.example.workoutapp.data.local.room.entity.RoomWorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- User Metrics ---
    @Query("SELECT * FROM user_metrics ORDER BY id DESC")
    fun getAllUserMetrics(): Flow<List<RoomUserMetrics>>


    // --- Exercises ---
    @Query("SELECT * FROM exercises WHERE isDeleted = 0")
    fun getAllExercises(): Flow<List<RoomExercise>>

    // --- Sessions ---
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<RoomWorkoutSession>>

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<RoomSettings?>

    // --- Rest Days ---
    @Query("SELECT * FROM rest_days ORDER BY date DESC")
    fun getAllRestDays(): Flow<List<RoomRestDay>>

    // --- Session Exercises ---
    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun getSessionExercises(sessionId: Int): Flow<List<RoomSessionExercise>>
}
