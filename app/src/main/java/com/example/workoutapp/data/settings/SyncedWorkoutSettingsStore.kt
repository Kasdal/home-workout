package com.example.workoutapp.data.settings

import kotlinx.coroutines.flow.Flow

interface SyncedWorkoutSettingsStore {
    fun observeSyncedWorkoutSettings(): Flow<WorkoutSessionSettings>
    suspend fun saveSyncedWorkoutSettings(settings: WorkoutSessionSettings)
}
