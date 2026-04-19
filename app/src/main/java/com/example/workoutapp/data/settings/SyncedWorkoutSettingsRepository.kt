package com.example.workoutapp.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class WorkoutSessionSettings(
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true
)

@Singleton
class SyncedWorkoutSettingsRepository @Inject constructor(
    private val syncedWorkoutSettingsStore: SyncedWorkoutSettingsStore
) {
    fun observeSessionSettings(): Flow<WorkoutSessionSettings> = syncedWorkoutSettingsStore.observeSyncedWorkoutSettings()

    suspend fun setRestTimerDuration(seconds: Int) {
        val current = syncedWorkoutSettingsStore.observeSyncedWorkoutSettings().first()
        syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(current.copy(restTimerDuration = seconds))
    }

    suspend fun setExerciseSwitchDuration(seconds: Int) {
        val current = syncedWorkoutSettingsStore.observeSyncedWorkoutSettings().first()
        syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(current.copy(exerciseSwitchDuration = seconds))
    }

    suspend fun setUndoLastSetEnabled(enabled: Boolean) {
        val current = syncedWorkoutSettingsStore.observeSyncedWorkoutSettings().first()
        syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(current.copy(undoLastSetEnabled = enabled))
    }
}
