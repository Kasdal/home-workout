package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.UserMetrics
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getUserMetrics(): Flow<UserMetrics?>
    fun getAllUserMetrics(): Flow<List<UserMetrics>>
    suspend fun saveUserMetrics(metrics: UserMetrics)
    suspend fun addUserMetrics(metrics: UserMetrics)
    suspend fun updateUserMetrics(metrics: UserMetrics)
    suspend fun setActiveProfile(profileId: Int)
    suspend fun deleteUserMetrics(profileId: Int)
}
