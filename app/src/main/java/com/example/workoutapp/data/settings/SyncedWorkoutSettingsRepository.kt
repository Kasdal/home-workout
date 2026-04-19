package com.example.workoutapp.data.settings

import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class WorkoutSessionSettings(
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true
)

@Singleton
class SyncedWorkoutSettingsRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun observeSessionSettings(): Flow<WorkoutSessionSettings> {
        return settingsRepository.getSettings().map { settings ->
            WorkoutSessionSettings(
                restTimerDuration = settings?.restTimerDuration ?: 30,
                exerciseSwitchDuration = settings?.exerciseSwitchDuration ?: 90,
                undoLastSetEnabled = settings?.undoLastSetEnabled ?: true
            )
        }
    }

    suspend fun setRestTimerDuration(seconds: Int) {
        val current = settingsRepository.getSettings().first() ?: Settings()
        settingsRepository.saveSettings(current.copy(restTimerDuration = seconds))
    }

    suspend fun setExerciseSwitchDuration(seconds: Int) {
        val current = settingsRepository.getSettings().first() ?: Settings()
        settingsRepository.saveSettings(current.copy(exerciseSwitchDuration = seconds))
    }

    suspend fun setUndoLastSetEnabled(enabled: Boolean) {
        val current = settingsRepository.getSettings().first() ?: Settings()
        settingsRepository.saveSettings(current.copy(undoLastSetEnabled = enabled))
    }
}
