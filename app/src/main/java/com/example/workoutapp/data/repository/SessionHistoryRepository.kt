package com.example.workoutapp.data.repository

import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    fun getSessions(): Flow<List<WorkoutSession>>
    suspend fun getSession(sessionId: Int): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun deleteSession(sessionId: Int)
    suspend fun saveSessionExercises(exercises: List<SessionExercise>)
    fun getSessionExercises(sessionId: Int): Flow<List<SessionExercise>>
    fun getExerciseHistory(exerciseName: String): Flow<List<SessionExercise>>
    fun getAllSessionExercises(): Flow<List<SessionExercise>>
}
