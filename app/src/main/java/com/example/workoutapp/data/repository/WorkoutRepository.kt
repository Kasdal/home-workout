package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getUserMetrics(): Flow<UserMetrics?>
    fun getAllUserMetrics(): Flow<List<UserMetrics>>
    suspend fun saveUserMetrics(metrics: UserMetrics)
    suspend fun addUserMetrics(metrics: UserMetrics)
    suspend fun updateUserMetrics(metrics: UserMetrics)
    suspend fun setActiveProfile(profileId: Int)
    suspend fun deleteUserMetrics(profileId: Int)


    fun getExercises(): Flow<List<Exercise>>
    suspend fun addExercise(exercise: Exercise)
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exerciseId: Int)

    fun getSessions(): Flow<List<WorkoutSession>>
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun deleteSession(sessionId: Int)
    
    fun getSettings(): Flow<Settings?>
    suspend fun saveSettings(settings: Settings)
    
    fun getRestDays(): Flow<List<RestDay>>
    suspend fun addRestDay(restDay: RestDay)
    suspend fun deleteRestDay(restDayId: Int)
    suspend fun getRestDayByDate(date: Long): RestDay?

    // Session Exercises
    suspend fun saveSessionExercises(exercises: List<com.example.workoutapp.data.local.entity.SessionExercise>)
    fun getSessionExercises(sessionId: Int): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>>
    fun getExerciseHistory(exerciseName: String): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>>
    fun getAllExerciseNames(): Flow<List<String>>
}
