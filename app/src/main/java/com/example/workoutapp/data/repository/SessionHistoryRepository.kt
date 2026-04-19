package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.SessionExercise
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.local.entity.WorkoutStats
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    fun getSessions(): Flow<List<WorkoutSession>>
    suspend fun getSession(sessionId: Int): WorkoutSession?
    fun getWorkoutStats(): Flow<WorkoutStats?>
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun deleteSession(sessionId: Int)
    suspend fun saveSessionExercises(exercises: List<SessionExercise>)
    fun getSessionExercises(sessionId: Int): Flow<List<SessionExercise>>
    fun getExerciseHistory(exerciseName: String): Flow<List<SessionExercise>>
    fun getAllExerciseNames(): Flow<List<String>>
    fun getAllSessionExercises(): Flow<List<SessionExercise>>
}
