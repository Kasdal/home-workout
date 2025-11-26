package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.Exercise
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
    suspend fun saveSession(session: WorkoutSession)
}
